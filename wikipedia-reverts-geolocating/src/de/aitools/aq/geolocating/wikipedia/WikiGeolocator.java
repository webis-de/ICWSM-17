package de.aitools.aq.geolocating.wikipedia;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.aitools.aq.decision.tree.CountingDecisionInternalNode;
import de.aitools.aq.decision.tree.CountingDecisionLeafNode;
import de.aitools.aq.decision.tree.CountingDecisionNodeFactory;
import de.aitools.aq.decision.tree.DecisionBranch;
import de.aitools.aq.decision.tree.DecisionLeafNode;
import de.aitools.aq.decision.tree.DecisionNode;
import de.aitools.aq.geolocating.Geolocation;
import de.aitools.aq.geolocating.Geolocator;
import de.aitools.aq.geolocating.collector.GeolocationCollector;
import de.aitools.aq.geolocating.collector.IpBlock;
import de.aitools.aq.wikipedia.reverts.PageUnmarshaller;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

public class WikiGeolocator {
  
  private GeolocationCollector<WikiGeolocations> collector;
  
  private DecisionNode<WikiGeolocations, Boolean> decisions;
  
  private WikiCounter finalCount;
  
  private WikiCounter rejectedCount;

  public WikiGeolocator(final GeolocationCollector<WikiGeolocations> collector)
  throws NullPointerException, IOException {
    if (collector == null) { throw new NullPointerException(); }
    this.collector = collector;
    this.decisions =
        Geolocator.createDefaultDecisionTree(new WikiDecisionNodeFactory());
    this.finalCount = new WikiCounter();
    this.rejectedCount = new WikiCounter();
  }
  
  public PageType geolocate(
      final de.aitools.aq.wikipedia.reverts.PageType page) {
    final PageType geolocatedPage = new PageType(page);
    for (final RevisionType revision : geolocatedPage.getRevisions()) {
      this.geolocate(revision);
    }
    return geolocatedPage;
  }
  
  private void geolocate(final RevisionType revision) {
    if (revision.hasIpV4()) {
      try {
        final InetAddress address = revision.getIpAddress();
        
        WikiGeolocations geolocations = this.collector.collect(
            address, revision.getTimestamp());
        geolocations.setIsReverted(revision.getReverted() != null);
        
        final DecisionLeafNode<? extends WikiGeolocations, ? extends Boolean>
            decision = this.decisions.decide(geolocations);
        if (decision.getValue()) {
          revision.setGeolocated(new Geolocation(geolocations));
          this.finalCount.count(geolocations);
        } else {
          this.rejectedCount.count(geolocations);
        }
      } catch (final UnknownHostException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }
  
  public void geolocatePages(final File inputFile, final File outputFile)
  throws Exception {
    try (final PageUnmarshaller unmarshaller =
        new PageUnmarshaller(inputFile)) {
      try (final GeolocatedPageMarshaller marshaller =
          new GeolocatedPageMarshaller(outputFile)) {
        
        unmarshaller.stream()
          .map(page -> this.geolocate(page))
          .forEach(marshaller);
      }
    }
  }
  
  public void writeCounts(final File directory)
  throws IOException {
    try (final Writer writer = new FileWriter(new File(directory,
        "decision-tree-counts.txt"))) {
      writer.append(this.decisions.toString());
      writer.append('\n');
      writer.append("Final\t").append(this.finalCount.toString()).append('\n');
      writer.append("Rejected\t").append(this.rejectedCount.toString()).append('\n');
    }
  }

  public static void main(final String[] args) throws Exception {
    if (args.length < 5) {
      System.err.println("Usage:");
      System.err.println("  <iplocation-dir> <rir-dir> <num-threads> <input-xml-file> [<input-xml-file> [...]] <output-dir>");
      System.exit(1);
    }
    
    final File iplocationsDirectory = new File(args[0]);
    final File rirDirectory = new File(args[1]);
    final int numThreads = Integer.parseInt(args[2]);
    final Queue<File> inputFiles = new ConcurrentLinkedQueue<File>();
    for (int a = 3; a < args.length - 1; ++a) {
      inputFiles.add(new File(args[a]));
    }
    final File outputDirectory = new File(args[args.length - 1]);
    
    System.out.println(new Date() + "  LOADING");
    final WikiGeolocator geolocator =
        new WikiGeolocator(new GeolocationCollector<>(
            iplocationsDirectory, rirDirectory, () -> new WikiGeolocations()));

    System.out.println(new Date() + "  START");
    final Thread[] threads = new Thread[numThreads];
    for (int t = 0; t < numThreads; ++t) {
      threads[t] = new Thread() {
        @Override
        public void run() {
          for (File inputFile = inputFiles.poll();
              inputFile != null;
              inputFile = inputFiles.poll()) {
            final File outputFile =
                new File(outputDirectory, inputFile.getName());
            try {
              System.out.println(new Date() + "    START  " + inputFile);
              geolocator.geolocatePages(inputFile, outputFile);
              System.out.println(new Date() + "    DONE   " + inputFile);
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
    geolocator.writeCounts(outputDirectory);
    System.out.println(new Date() + "  DONE");
  }
  
  private static class WikiDecisionLeafNode
  extends CountingDecisionLeafNode<WikiGeolocations, Boolean> {
    
    private final WikiCounter wikiCounter;

    public WikiDecisionLeafNode(final Boolean value) {
      super(value);
      this.wikiCounter = new WikiCounter();
    }
    
    @Override
    protected void count(final WikiGeolocations geolocations) {
      super.count(geolocations);
      this.wikiCounter.count(geolocations);
    }
    
    @Override
    protected void countsToString(final StringBuilder output) {
      this.wikiCounter.countsToString(output);
    }
    
  }
  
  private static class WikiDecisionInternalNode
  extends CountingDecisionInternalNode<WikiGeolocations, Boolean> {
    
    private final WikiCounter wikiCounter;

    public WikiDecisionInternalNode(
        final DecisionBranch<WikiGeolocations, Boolean> branching) {
      super(branching);
      this.wikiCounter = new WikiCounter();
    }
    
    @Override
    protected void count(final WikiGeolocations geolocations) {
      super.count(geolocations);
      this.wikiCounter.count(geolocations);
    }
    
    @Override
    protected void countsToString(final StringBuilder output) {
      this.wikiCounter.countsToString(output);
    }
    
  }
  
  private static class WikiCounter {
    
    private long count;
    
    private long countReverted;
    
    private final TLongSet uniqueIps;
    
    private final TLongSet uniqueIpsReverts;
    
    private final TIntLongMap numIplocations;
    
    private final TIntLongMap numIplocationsReverted;
    
    public WikiCounter() {
      this.count = 0;
      this.countReverted = 0;
      this.uniqueIps = new TLongHashSet();
      this.uniqueIpsReverts = new TLongHashSet();
      this.numIplocations = new TIntLongHashMap();
      this.numIplocationsReverted = new TIntLongHashMap();
    }

    public void count(final WikiGeolocations geolocations) {
      final long ip = IpBlock.ipToLong(geolocations.getIp());
      final int numIplocations =
          geolocations.getIplocationGeolocations().size();
      synchronized (this) {
        ++this.count;
        this.uniqueIps.add(ip);
        this.numIplocations.adjustOrPutValue(numIplocations, 1L, 1L);
        if (geolocations.getIsReverted()) {
          ++this.countReverted;
          this.uniqueIpsReverts.add(ip);
          this.numIplocationsReverted.adjustOrPutValue(numIplocations, 1L, 1L);
        }
      }
    }
    
    @Override
    public String toString() {
      return this.countsToString(new StringBuilder()).toString();
    }
    
    public StringBuilder countsToString(final StringBuilder output) {
      output.append(this.count);
      output.append('\t').append(this.countReverted);
      output.append('\t').append(this.uniqueIps.size());
      output.append('\t').append(this.uniqueIpsReverts.size());
      output.append('\t');
      WikiCounter.appendLengthCounts(output, this.numIplocations);
      output.append('\t');
      WikiCounter.appendLengthCounts(output, this.numIplocationsReverted);
      return output;
    }
    
    private static void appendLengthCounts(
        final StringBuilder output, final TIntLongMap counts) {
      final int[] lengths = counts.keys();
      Arrays.sort(lengths);
      
      boolean isFirst = true;
      for (final int length : lengths) {
        final long count = counts.get(length);
        if (isFirst) {
          isFirst = false;
        } else {
          output.append(',');
        }
        output.append(length).append(':').append(count);
      }
    }
    
  }
  
  private static class WikiDecisionNodeFactory
  extends CountingDecisionNodeFactory<WikiGeolocations, Boolean> {
    
    @Override
    public DecisionNode<WikiGeolocations, Boolean> internal(
        final DecisionBranch<WikiGeolocations, Boolean> branching)
    throws NullPointerException {
      return new WikiDecisionInternalNode(branching);
    }
    
    @Override
    public DecisionNode<WikiGeolocations, Boolean> leaf(final Boolean value)
    throws NullPointerException {
      return new WikiDecisionLeafNode(value);
    }
    
  }

}
