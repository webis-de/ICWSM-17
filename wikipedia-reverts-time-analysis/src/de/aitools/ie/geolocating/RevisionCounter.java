package de.aitools.ie.geolocating;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.TreeMap;

import de.aitools.aq.geolocating.Geolocation;
import de.aitools.aq.geolocating.wikipedia.GeolocatedPageUnmarshaller;
import de.aitools.aq.geolocating.wikipedia.PageType;
import de.aitools.aq.geolocating.wikipedia.RevisionType;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class RevisionCounter {
  
  private final Bins bins;
  
  public RevisionCounter() {
    this.bins = new Bins();
  }
  
  public void add(final RevisionType revision) {
    final Geolocation geolocation = revision.getGeolocated();
    if (geolocation != null) {
      final String countryCode = geolocation.getCountryCode();
      
      final Instant instant = revision.getTimestamp();
      final ZoneId timeZone = ZoneId.of(geolocation.getTimeZone());   
      final ZonedDateTime time = ZonedDateTime.ofInstant(instant, timeZone);
      
      final boolean isReverted = revision.getReverted() != null;
      final boolean isVandalismCommentReverted =
          isReverted && revision.getReverted().getWithVandalismComment();
      
      this.bins.add(countryCode, time, isReverted, isVandalismCommentReverted);
    }
  }
  
  public void add(final PageType page) {
    for (final RevisionType revision : page.getRevisions()) {
      this.add(revision);
    }
  }
  
  public void add(final File inputFile) throws Exception {
    try (final GeolocatedPageUnmarshaller unmarshaller =
        new GeolocatedPageUnmarshaller(inputFile)) {
      unmarshaller.stream()
        .forEach(page -> this.add(page));
    }
  }
  
  public void write(final Writer writer) throws IOException {
    Bins.writeHead(writer);
    this.bins.write(new StringBuilder(), writer);
  }
  
  public static void main(final String[] args) throws Exception {
    if (args.length < 3) {
      System.err.println("Usage:");
      System.err.println("  <num-threads> <input-xml-file> [<input-xml-file> [...]] <output-file>");
      System.exit(1);
    }
    
    final int numThreads = Integer.parseInt(args[0]);
    final Queue<File> inputFiles = new ConcurrentLinkedQueue<File>();
    for (int a = 1; a < args.length - 1; ++a) {
      inputFiles.add(new File(args[a]));
    }
    final File outputFile = new File(args[args.length - 1]);
    final RevisionCounter counter = new RevisionCounter();

    System.out.println(new Date() + "  START");
    final Thread[] threads = new Thread[numThreads];
    for (int t = 0; t < numThreads; ++t) {
      threads[t] = new Thread() {
        @Override
        public void run() {
          for (File inputFile = inputFiles.poll();
              inputFile != null;
              inputFile = inputFiles.poll()) {
            try {
              System.out.println(new Date() + "  START  " + inputFile);
              counter.add(inputFile);
              System.out.println(new Date() + "  DONE   " + inputFile);
            } catch (final Exception e) {
              throw new RuntimeException(e);
            }
          }
        }
      };
      threads[t].start();
    }
    
    for (final Thread thread : threads) {
      thread.join();
    }
    System.out.println(new Date() + "  DONE");

    System.out.println(new Date() + "  START  WRITING COUNTS");
    try (final BufferedWriter writer =
        new BufferedWriter(new FileWriter(outputFile))) {
      counter.write(writer);
    }
    System.out.println(new Date() + "  DONE");
    
  }
  
  protected static class Bins {
    
    private Map<String, LocationBins> bins;
    
    public Bins() {
      this.bins = new TreeMap<>();
    }

    public void add(final String countryCode, final ZonedDateTime time,
        final boolean isReverted, final boolean isVandalismCommentReverted) {
      final String timeZone = time.getZone().getId();
      LocationBins bins = null;
      synchronized (this) {
        bins = this.bins.get(timeZone);
        if (bins == null) {
          bins = new LocationBins(countryCode);
          this.bins.put(timeZone, bins);
        }
      }
      bins.add(time, isReverted, isVandalismCommentReverted);
    }
    
    public static void writeHead(final Writer writer)
    throws IOException {
      writer.write("#time-zone\t");
      LocationBins.writeHead(writer);
    }
    
    public void write(final StringBuilder buffer, final Writer writer)
    throws IOException {
      int bufferStartSize = buffer.length();
      for (final Entry<String, LocationBins> entry : this.bins.entrySet()) {
        buffer.append(entry.getKey());
        buffer.append('\t');
        entry.getValue().write(buffer, writer);
        buffer.delete(bufferStartSize, buffer.length());
      }
    }
    
  } 
  
  protected static class LocationBins {
    
    private String countryCode;
    
    private TIntObjectMap<TimeBins> timeBins;
    
    public LocationBins(final String countryCode) {
      if (countryCode == null) { throw new NullPointerException(); }
      this.countryCode = countryCode;
      this.timeBins = new TIntObjectHashMap<>();
    }

    public void add(final ZonedDateTime time,
        final boolean isReverted, final boolean isVandalismCommentReverted) {
      final int daySinceEpoch = TimeBins.getDaySinceEpoch(time);
      TimeBins bins = null;
      synchronized (this) {
        bins = this.timeBins.get(daySinceEpoch);
        if (bins == null) {
          bins = new TimeBins(time);
          this.timeBins.put(daySinceEpoch, bins);
        }
      }
      bins.add(time, isReverted, isVandalismCommentReverted);
    }
    
    public static void writeHead(final Writer writer)
    throws IOException {
      writer.write("#country\t#day-since-epoch\t");
      TimeBins.writeHead(writer);
    }
    
    public void write(final StringBuilder buffer, final Writer writer)
    throws IOException {
      int bufferStartSize = buffer.length();
      buffer.append(this.countryCode).append('\t');
      int bufferSizeAfterFixed = buffer.length();
      
      final TIntObjectIterator<TimeBins> iterator = this.timeBins.iterator();
      while (iterator.hasNext()) {
        iterator.advance();
        buffer.append(iterator.key());
        buffer.append('\t');
        iterator.value().write(buffer, writer);
        buffer.delete(bufferSizeAfterFixed, buffer.length());
      }
      buffer.delete(bufferStartSize, buffer.length());
    }
    
  }

  protected static class TimeBins {
    
    private Season season;
    
    private DayOfWeek weekday;
    
    private Bin[] hourBins;
    
    public TimeBins(final ZonedDateTime time) {
      this.season = Season.get(time);
      this.weekday = time.getDayOfWeek();
      this.hourBins = new Bin[24];
      for (int h = 0; h < 24; ++h) {
        this.hourBins[h] = new Bin();
      }
    }
  
    public void add(final ZonedDateTime time,
        final boolean isReverted, final boolean isVandalismCommentReverted) {
      this.hourBins[time.getHour()].add(isReverted, isVandalismCommentReverted);
    }
    
    public static void writeHead(final Writer writer)
    throws IOException {
      writer.write("#season\t#day-of-week\t#hour\t");
      Bin.writeHead(writer);
    }
    
    public void write(final StringBuilder buffer, final Writer writer)
    throws IOException {
      int bufferStartSize = buffer.length();
      buffer.append(this.season).append('\t');
      buffer.append(this.weekday).append('\t');
      int bufferSizeAfterFixed = buffer.length();
      for (int h = 0; h < 24; ++h) {
        buffer.append(h);
        buffer.append('\t');
        this.hourBins[h].write(buffer, writer);
        buffer.delete(bufferSizeAfterFixed, buffer.length());
      }
      buffer.delete(bufferStartSize, buffer.length());
    }
    
    protected static int getDaySinceEpoch(final ZonedDateTime time) {
      final ZonedDateTime epoch =
          LocalDate.ofEpochDay(0).atStartOfDay(time.getZone());
      return (int) ChronoUnit.DAYS.between(epoch, time);
    }
    
  }
  
  protected static class Bin {
    
    private int total;
    
    private int reverted;
    
    private int vandalismCommentReverted;
    
    public Bin() {
      this.total = 0;
      this.reverted = 0;
      this.vandalismCommentReverted = 0;
    }
    
    public void add(
        final boolean isReverted, final boolean isVandalismCommentReverted) {
      synchronized (this) {
        ++this.total;
        if (isReverted) {
          ++this.reverted;
          if (isVandalismCommentReverted) {
            ++this.vandalismCommentReverted;
          }
        }
      }
    }

    public static void writeHead(final Writer writer)
    throws IOException {
      writer.write("#num-revisions\t#num-reverted-revisions\t#num-vandalism-comment-reverted-revisions\n");
    }
    
    public void write(final StringBuilder buffer, final Writer writer)
    throws IOException {
      writer.append(buffer.toString());
      writer.append(String.valueOf(this.total)).append('\t');
      writer.append(String.valueOf(this.reverted)).append('\t');
      writer.append(String.valueOf(this.vandalismCommentReverted)).append('\n');
    }
    
  }

}
