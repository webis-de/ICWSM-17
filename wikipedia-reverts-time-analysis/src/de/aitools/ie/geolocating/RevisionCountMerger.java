package de.aitools.ie.geolocating;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class RevisionCountMerger {
  
  private final List<Group> groups;
  
  private final int mergeField;
  
  private final int countersStartField;
  
  private final Map<String[], long[]> counters;
  
  public RevisionCountMerger(
      final int mergeField, final String[] groupDescriptions,
      final int countersStartField) {
    this.mergeField = mergeField;
    this.countersStartField = countersStartField;
    this.groups = new ArrayList<>();
    for (final String groupDescription : groupDescriptions) {
      this.groups.add(new Group(groupDescription));
    }
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
    writer.append(header);
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
        Arrays.copyOfRange(fields, 0, this.countersStartField);
    final long[] counts = new long[fields.length - specifiers.length];
    for (int c = specifiers.length; c < fields.length; ++c) {
      counts[c - specifiers.length] = Long.parseLong(fields[c]);
    }
    this.add(specifiers, counts);
  }
  
  public void add(final String[] specifiers, final long[] counts) {
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
    final String[] determiners = new String[specifiers.length];
    System.arraycopy(specifiers, 0, determiners, 0, specifiers.length);
    
    for (final Group group : this.groups) {
      if (group.pattern.matcher(specifiers[this.mergeField]).matches()) {
        if (group.name.isEmpty()) {
          return null;
        }
        determiners[this.mergeField] = group.name;
        break;
      }
    }
    return determiners;
  }
  
  public static void main(final String[] args) throws IOException {
    final String[] groupDescriptions = new String[args.length - 2];
    System.arraycopy(args, 1, groupDescriptions, 0, groupDescriptions.length);
    final RevisionCountMerger merger = new RevisionCountMerger(
        Integer.parseInt(args[0]),
        groupDescriptions,
        Integer.parseInt(args[args.length - 1]));
    try (final BufferedReader reader =
        new BufferedReader(new InputStreamReader(System.in))) {
      try (final BufferedWriter writer =
          new BufferedWriter(new OutputStreamWriter(System.out))) {
        merger.process(reader, writer);
      }
    }
  }
  
  protected static class Group {
    
    public final String name;
    
    public final Pattern pattern;
    
    public Group(final String description) {
      final String[] namePattern  = description.split(":", 2);
      this.name = namePattern[0];
      this.pattern = Pattern.compile(namePattern[1]);
    }
    
    public Group(final String name, final Pattern pattern) {
      this.name = name;
      this.pattern = pattern;
    }
    
  }

}
