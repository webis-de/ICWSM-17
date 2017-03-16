package de.aitools.ie.geolocating;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class RevisionCountMarginalizer {
  
  private final Pattern[] patterns;
  
  private final int numPatterns;
  
  private final Map<String[], long[]> counters;
  
  public RevisionCountMarginalizer(final String[] args) {
    int numPatterns = 0;
    this.patterns = new Pattern[args.length];
    for (int a = 0; a < args.length; ++a) {
      final String arg = args[a];
      if (args == null || arg.isEmpty()) {
        this.patterns[a] = null;
      } else {
        this.patterns[a] = Pattern.compile(arg);
        ++numPatterns;
      }
    }
    this.numPatterns = numPatterns;
    this.counters = new TreeMap<>(new Comparator<String[]>() {
      @Override
      public int compare(final String[] o1, final String[] o2) {
        for (int i = 0; i < o1.length; ++i) {
          final int comparison = o1[i].compareTo(o2[i]);
          if (comparison != 0) { return comparison; }
        }
        return 0;
      }
    });
  }
  
  public void process(final BufferedReader reader, final Writer writer)
  throws IOException {
    String line = reader.readLine();
    if (line != null && line.startsWith("#")) {
      this.convertHeader(line, writer);
      line = reader.readLine();
    }
    while (line != null) {
      this.add(line);
      line = reader.readLine();
    }
    this.write(writer);
  }
  
  public void convertHeader(final String header, final Writer writer)
  throws IOException {
    final String[] fields = header.split("\t");
    for (int p = 0; p < this.patterns.length; ++p) {
      final Pattern pattern = this.patterns[p];
      if (pattern != null) {
        writer.append(fields[p]);
        writer.append('{');
        writer.append(pattern.toString());
        writer.append('}');
        writer.append('\t');
      }
    }
    writer.append(String.join("\t", Arrays.copyOfRange(
        fields, this.patterns.length, fields.length)));
    writer.append('\n');
  }
  
  public void write(final Writer writer) throws IOException {
    for (final Entry<String[], long[]> counter : this.counters.entrySet()) {
      writer.append(String.join("\t", counter.getKey()));
      for (final long count : counter.getValue()) {
        writer.append('\t').append(String.valueOf(count));
      }
      writer.append('\n');
    }
  }
  
  public void add(final String line) {
    final String[] fields = line.split("\t");
    final String[] specifiers =
        Arrays.copyOfRange(fields, 0, this.patterns.length);
    final long[] counts = new long[fields.length - specifiers.length];
    for (int c = specifiers.length; c < fields.length; ++c) {
      counts[c - specifiers.length] = Long.parseLong(fields[c]);
    }
    this.add(specifiers, counts);
  }
  
  public void add(final String[] specifiers, final long[] counts) {
    if (specifiers.length != this.patterns.length) {
      throw new IllegalArgumentException(Arrays.toString(specifiers));
    }
    
    final String[] determiners = this.getDeterminers(specifiers);
    if (determiners != null) {
      final long[] counters = this.getCounters(determiners, counts.length);
      for (int c = 0; c < counts.length; ++c) {
        counters[c] += counts[c];
      }
    }
  }
  
  protected long[] getCounters(
      final String[] determiners, final int numCounters) {
    long[] counters = this.counters.get(determiners);
    if (counters == null) {
      synchronized (this.counters) {
        counters = this.counters.get(determiners);
        if (counters == null) {
          counters = new long[numCounters];
          this.counters.put(determiners, counters);
        }
      }
    }
    if (counters.length != numCounters) {
      throw new IllegalStateException("Mismatch in number of counters: "
          + counters.length + " != " + numCounters);
    }
    return counters;
  }
  
  protected String[] getDeterminers(final String[] specifiers) {
    final String[] determiners = new String[this.numPatterns];
    int d = 0;
    for (int s = 0; s < specifiers.length; ++s) {
      final Pattern pattern = this.patterns[s];
      if (pattern != null) {
        if (pattern.matcher(specifiers[s]).matches()) {
          determiners[d] = specifiers[s];
        } else {
          return null;
        }
        ++d;
      }
    }
    return determiners;
  }
  
  public static void main(final String[] args) throws IOException {
    final RevisionCountMarginalizer merger = new RevisionCountMarginalizer(args);
    try (final BufferedReader reader =
        new BufferedReader(new InputStreamReader(System.in))) {
      try (final BufferedWriter writer =
          new BufferedWriter(new OutputStreamWriter(System.out))) {
        merger.process(reader, writer);
      }
    }
  }

}
