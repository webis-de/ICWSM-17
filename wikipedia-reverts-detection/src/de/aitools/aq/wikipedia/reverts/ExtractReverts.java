package de.aitools.aq.wikipedia.reverts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import de.aitools.aq.wikipedia.BotList;
import de.aitools.aq.wikipedia.PageUnmarshaller;
import de.aitools.aq.wikipedia.WikipediaUtil;

/**
 * Program to extract and filter reverts from a Wikipedia history stub dump.
 * 
 * See the project README.txt, EXTRACTING REVERTS. This is the program that will
 * be called by src-shell/extract-reverts.sh . It creates in the
 * <tt>*-statistics.txt</tt> files (using its private RevertCounter class), the
 * <tt>reverts.xml.gz</tt>, and the <tt>*-chains-*.txt</tt> files (using
 * {@link RevertChain}).
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class ExtractReverts
implements AutoCloseable, Function<de.aitools.aq.wikipedia.xml.PageType, PageType> {
  
  private static final String FILENAME_BASE_INTERLEAVED_CHAINS = "interleaved-chains";
  
  private static final String FILENAME_BASE_ENCLOSING_CHAINS = "enclosing-chains";
  
  private static final String FILENAME_FINAL_REVERTS = "reverts.xml.gz";
  
  private static final String FILENAME_REVISIONS_STATISTICS = "revision-statistics.txt";
  
  private static final String FILENAME_REVERT_STATISTICS = "revert-statistics.txt";
  
  private static final String FILENAME_REVERTED_STATISTICS = "reverted-revisions-statistics.txt";
  
  private static final String FILENAME_DOUBLE_SUBMISSION_TIMES = "double-submission-time-spans-in-seconds.txt";
  
  private int numRevisions;

  private int numBlankRevisions;

  private int numDeletedRevisions;
  
  private int numCommentedRevertRevisions;
  
  private int numVandalismCommentedRevertRevisions;
  
  private final RevertCounter numReverts;
  
  private final RevertFilter numRevertsAfterRemovingRevertsToEmpty;
  
  private final RevertFilter numRevertsAfterRemovingRenaming;
  
  private final RevertFilter numRevertsAfterRemovingDoubleSubmissions;
  
  private final RevertFilter numRevertsAfterRemovingWithoutReverted;
  
  private final RevertFilter numRevertsAfterRemovingSelfReverts;
  
  private final RevertSelfCorrectionUpdater numRevertsAfterSelfCorrections;
  
  private final RevertFilter numRevertsAfterRemovingUnacceptedReverts;
  
  private final RevertFilter numRevertsAfterRemovingInterleavedReverts;
  
  private final RevertFilter numRevertsAfterRemovingDifferentUserRevertedReverts;
  
  private final RevertFilter numRevertsAfterRemovingNonAnonymousReverted;
  
  private final RevertFilter numRevertsAfterRemovingIpv6;
  
  private final BufferedWriter doubleSubmissionsTimeSpanWriter;
  
  private final BufferedWriter interleavedChainsWriter;
  
  private final BufferedWriter enclosingChainsWriter;
  
  private final BufferedWriter interleavedChainsAfterRemovingUnacceptedWriter;
  
  private final BufferedWriter enclosingChainsAfterRemovingUnacceptedWriter;
  
  private final BufferedWriter enclosingChainsAfterRemovingInterleavedWriter;
  
  private final RevertMarshaller finalRevertsMarshaller;
  
  private final File outputDirectory;
  
  public ExtractReverts(final File outputDirectory)
  throws IOException, XMLStreamException, JAXBException {
    this.numRevisions = 0;
    this.numBlankRevisions = 0;
    this.numDeletedRevisions = 0;
    this.numCommentedRevertRevisions = 0;
    this.numVandalismCommentedRevertRevisions = 0;
    
    this.numReverts =
        new RevertCounter("All full page reverts");
    this.numRevertsAfterRemovingRevertsToEmpty =
        new RevertFilter("Without reverts to page blank or deleted",
            revert -> !revert.isRevertToEmpty(),
            outputDirectory);
    this.numRevertsAfterRemovingRenaming =
        new RevertFilter("And without renaming",
            revert -> !revert.isRenamingRevert(),
            outputDirectory);
    this.numRevertsAfterRemovingDoubleSubmissions =
        new RevertFilter("And without double submissions",
            revert -> !revert.isDoubleSubmission(),
            outputDirectory);
    this.numRevertsAfterRemovingWithoutReverted =
        new RevertFilter("And without reverts without reverted",
            revert -> !revert.revertedRevisions.isEmpty(),
            outputDirectory);
    this.numRevertsAfterRemovingSelfReverts =
        new RevertFilter("And without self reverts",
            revert -> !revert.isSelfRevert(),
            outputDirectory);
    this.numRevertsAfterSelfCorrections =
        new RevertSelfCorrectionUpdater(outputDirectory);
    this.numRevertsAfterRemovingUnacceptedReverts =
        new RevertFilter("And without unaccepted reverts",
            revert -> !revert.isUnacceptedRevert(),
            outputDirectory);   
    this.numRevertsAfterRemovingInterleavedReverts =
        new RevertFilter("And without interleaved reverts",
            revert -> !revert.isInterleaved(),
            outputDirectory);   
    this.numRevertsAfterRemovingDifferentUserRevertedReverts =
        new RevertFilter("And without reverts reverting different users",
            revert -> revert.isRevertingOnlyOneUser(),
            outputDirectory);
    this.numRevertsAfterRemovingNonAnonymousReverted =
        new RevertFilter("And without reverts reverting non-anonymous users",
            revert -> WikipediaUtil.isAnonymousRevision(revert.revertedRevisions.get(0)),
            outputDirectory);
    this.numRevertsAfterRemovingIpv6 =
        new RevertFilter("And without reverts reverting ipv6 users",
            revert -> !WikipediaUtil.isAnonymousIpv6Revision(revert.revertedRevisions.get(0)),
            outputDirectory);
    
    this.doubleSubmissionsTimeSpanWriter =
        new BufferedWriter(new FileWriter(new File(outputDirectory,
            FILENAME_DOUBLE_SUBMISSION_TIMES)));
    
    this.interleavedChainsWriter =
        new BufferedWriter(new FileWriter(new File(outputDirectory,
            FILENAME_BASE_INTERLEAVED_CHAINS + ".txt")));
    this.enclosingChainsWriter =
        new BufferedWriter(new FileWriter(new File(outputDirectory, 
            FILENAME_BASE_ENCLOSING_CHAINS + ".txt")));
    
    this.interleavedChainsAfterRemovingUnacceptedWriter =
        new BufferedWriter(new FileWriter(new File(outputDirectory,
            FILENAME_BASE_INTERLEAVED_CHAINS + "-after-removing-unaccepted.txt")));
    this.enclosingChainsAfterRemovingUnacceptedWriter =
        new BufferedWriter(new FileWriter(new File(outputDirectory,
            FILENAME_BASE_ENCLOSING_CHAINS + "-after-removing-unaccepted.txt")));
    
    this.enclosingChainsAfterRemovingInterleavedWriter =
        new BufferedWriter(new FileWriter(new File(outputDirectory,
            FILENAME_BASE_ENCLOSING_CHAINS + "-after-removing-interleaved.txt")));
    
    this.finalRevertsMarshaller = new RevertMarshaller(
        new File(outputDirectory, FILENAME_FINAL_REVERTS));
    
    this.outputDirectory = outputDirectory;
  }

  @Override
  public PageType apply(final de.aitools.aq.wikipedia.xml.PageType page) {
    final PageType revisions = PageType.fromPage(page);
    this.countRevisions(page);
    
    final List<Revert> reverts = Revert.createAll(page);
    for (final Revert revert : reverts) { this.numReverts.count(revert); }
    
    try {
      this.preprocessReverts(reverts);
      this.filterReverts(reverts);
      
      // Write reverts
      synchronized (this.finalRevertsMarshaller) {
        for (final Revert revert : reverts) {
          this.finalRevertsMarshaller.accept(revert.toRevertType());
        }
      }
      
      // Write revisions
      ExtractReverts.addRevertInformation(revisions, reverts);
      return revisions;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void extractPages(final File inputFile, final File outputFile)
  throws Exception {
    final PageNamespaceFilter pagePredicate = new PageNamespaceFilter();
    try (final PageUnmarshaller unmarshaller =
        new PageUnmarshaller(inputFile)) {
      try (final PageMarshaller marshaller = new PageMarshaller(outputFile)) {
        unmarshaller.stream()
          .filter(pagePredicate)
          .map(page -> this.apply(page))
          .forEach(marshaller);
      }
    }
  }
  
  private void countRevisions(final de.aitools.aq.wikipedia.xml.PageType page) {
    synchronized (this) {
      for (final de.aitools.aq.wikipedia.xml.RevisionType revision
          : WikipediaUtil.getRevisions(page)) {
        ++this.numRevisions;
        if (WikipediaUtil.hasRevertComment(revision)) {
          ++this.numCommentedRevertRevisions;
        }
        if (WikipediaUtil.hasVandalismComment(revision)) {
          ++this.numVandalismCommentedRevertRevisions;
        }
        if (WikipediaUtil.isBlank(revision)) {
          ++this.numBlankRevisions;
        }
        if (WikipediaUtil.isDeleted(revision)) {
          ++this.numDeletedRevisions;
        }
      }
    }
  }
  
  private void preprocessReverts(final List<Revert> reverts)
  throws NullPointerException, XMLStreamException, JAXBException, IOException {
    this.numRevertsAfterRemovingRevertsToEmpty.filter(reverts);
    
    this.numRevertsAfterRemovingRenaming.filter(reverts);
    
    final Set<Revert> doubleSubmissions =
        this.numRevertsAfterRemovingDoubleSubmissions.filter(reverts);
    for (final Revert revert : doubleSubmissions) {
      final long timeSpan = revert.getTimeSpanInSeconds();
      synchronized (this.doubleSubmissionsTimeSpanWriter) {
        this.doubleSubmissionsTimeSpanWriter.append(String.valueOf(timeSpan))
          .append('\n');
      }
    }
    
    this.numRevertsAfterRemovingWithoutReverted.filter(reverts);
  }
  
  private void filterReverts(final List<Revert> reverts)
  throws NullPointerException, XMLStreamException, JAXBException, IOException {
    this.numRevertsAfterRemovingSelfReverts.filter(reverts);
    
    this.numRevertsAfterSelfCorrections.update(reverts);

    synchronized (this.interleavedChainsWriter) {
      for (final RevertChain chain
          : RevertChain.createInterleavedRevertChains(reverts)) {
        this.interleavedChainsWriter.write(chain.toString());
        this.interleavedChainsWriter.newLine();
      }
    }
    synchronized (this.enclosingChainsWriter) {
      for (final RevertChain chain
          : RevertChain.createEnclosingRevertChains(reverts)) {
        this.enclosingChainsWriter.write(chain.toString());
        this.enclosingChainsWriter.newLine();
      }
    }

    this.numRevertsAfterRemovingUnacceptedReverts.filter(reverts);
    synchronized (this.interleavedChainsAfterRemovingUnacceptedWriter) {
      for (final RevertChain chain
          : RevertChain.createInterleavedRevertChains(reverts)) {
        this.interleavedChainsAfterRemovingUnacceptedWriter.write(chain.toString());
        this.interleavedChainsAfterRemovingUnacceptedWriter.newLine();
      }
    }
    synchronized (this.enclosingChainsAfterRemovingUnacceptedWriter) {
      for (final RevertChain chain
          : RevertChain.createEnclosingRevertChains(reverts)) {
        this.enclosingChainsAfterRemovingUnacceptedWriter.write(chain.toString());
        this.enclosingChainsAfterRemovingUnacceptedWriter.newLine();
      }
    }

    this.numRevertsAfterRemovingInterleavedReverts.filter(reverts);
    synchronized (this.enclosingChainsAfterRemovingInterleavedWriter) {
      for (final RevertChain chain
          : RevertChain.createEnclosingRevertChains(reverts)) {
        this.enclosingChainsAfterRemovingInterleavedWriter.write(chain.toString());
        this.enclosingChainsAfterRemovingInterleavedWriter.newLine();
      }
    }

    this.numRevertsAfterRemovingDifferentUserRevertedReverts.filter(reverts);

    this.numRevertsAfterRemovingNonAnonymousReverted.filter(reverts);
    
    this.numRevertsAfterRemovingIpv6.filter(reverts);
  }
  
  private static void addRevertInformation(
      final PageType page, final List<Revert> reverts) {
    final List<RevisionType> revisions = page.getRevisions();

    for (final Revert revert : reverts) {
      revisions.get(revert.endIndex).setRevertsToIndex(revert.startIndex);
    }
  }
  
  private void writeRevisionStatistics(final Writer writer) throws IOException {
    writer.append("All revisions\t").append(
        String.valueOf(this.numRevisions)).append('\n');
    writer.append("Revert commented revisions\t").append(
        String.valueOf(this.numCommentedRevertRevisions)).append('\n');
    writer.append("Vandalism commented revisions\t").append(
        String.valueOf(this.numVandalismCommentedRevertRevisions)).append('\n');
    writer.append("Blank revisions\t").append(
        String.valueOf(this.numBlankRevisions)).append('\n');
    writer.append("Deleted revisions\t").append(
        String.valueOf(this.numDeletedRevisions)).append('\n');
    
  }
  
  private void writeRevertsTable(final Writer writer) throws IOException {
    writer.write("#1#step-name\t#2#num-reverts\t#3#num-reverts-with-revert-comment\t#4#num-reverts-with-vandalism-comment\n");
    writer.write(
        this.numReverts.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingRevertsToEmpty.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingRenaming.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingDoubleSubmissions.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingWithoutReverted.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingSelfReverts.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterSelfCorrections.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingUnacceptedReverts.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingInterleavedReverts.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingDifferentUserRevertedReverts.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingNonAnonymousReverted.makeRevertsTableRow());
    writer.write(
        this.numRevertsAfterRemovingIpv6.makeRevertsTableRow());
  }
  
  private void writeRevertedTable(final Writer writer) throws IOException {
    writer.append("#1#step-name\t#2#num-reverted-revisions\t#3#num-reverted-revisions-by-anonymous-user\t#4#num-reverted-revisions-by-registered-and-not-deleted-users\t#5#num-reverted-revisions-by-bots\n");
    writer.append(
        this.numReverts.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingRevertsToEmpty.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingRenaming.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingDoubleSubmissions.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingWithoutReverted.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingSelfReverts.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterSelfCorrections.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingUnacceptedReverts.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingInterleavedReverts.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingDifferentUserRevertedReverts.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingNonAnonymousReverted.makeRevertedTableRow());
    writer.append(
        this.numRevertsAfterRemovingIpv6.makeRevertedTableRow());
  }
  
  @Override
  public void close() throws Exception {
    this.numRevertsAfterRemovingRevertsToEmpty.close();
    this.numRevertsAfterRemovingRenaming.close();
    this.numRevertsAfterRemovingDoubleSubmissions.close();
    this.numRevertsAfterRemovingWithoutReverted.close();
    this.numRevertsAfterRemovingSelfReverts.close();
    this.numRevertsAfterSelfCorrections.close();
    this.numRevertsAfterRemovingUnacceptedReverts.close();
    this.numRevertsAfterRemovingInterleavedReverts.close();
    this.numRevertsAfterRemovingDifferentUserRevertedReverts.close();
    this.numRevertsAfterRemovingNonAnonymousReverted.close();
    this.numRevertsAfterRemovingIpv6.close();

    this.doubleSubmissionsTimeSpanWriter.close();
    this.interleavedChainsWriter.close();
    this.enclosingChainsWriter.close();
    this.interleavedChainsAfterRemovingUnacceptedWriter.close();
    this.enclosingChainsAfterRemovingUnacceptedWriter.close();
    this.enclosingChainsAfterRemovingInterleavedWriter.close();

    this.finalRevertsMarshaller.close();

    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(
        new File(this.outputDirectory, FILENAME_REVISIONS_STATISTICS)))) {
      this.writeRevisionStatistics(writer);
    }
    
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(
        new File(this.outputDirectory, FILENAME_REVERT_STATISTICS)))) {
      this.writeRevertsTable(writer);
    }

    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(
        new File(this.outputDirectory, FILENAME_REVERTED_STATISTICS)))) {
      this.writeRevertedTable(writer);
    }
  }
  
  private static class RevertCounter {
    
    public final String description;
    
    public int reverts;
    
    public int revertsCommented;
    
    public int revertsVandalismCommented;
    
    public int revertedTotal;
    
    public int revertedAnonymousUser;
    
    public int revertedRegisteredUser;
    
    public int revertedBot;
    
    public RevertCounter(final String description) throws NullPointerException {
      if (description == null) { throw new NullPointerException(); }
      this.description = description;
      
      this.reverts = 0;
      this.revertedTotal = 0;
      this.revertedAnonymousUser = 0;
      this.revertedRegisteredUser = 0;
      this.revertedBot = 0;
    }

    public void count(final Revert revert) {
      synchronized (this) {
        ++this.reverts;
  
        if (revert.isVandalismCommentRevert()) {
          ++this.revertsVandalismCommented;
        }
        if (revert.isCommentRevert()) {
          ++this.revertsCommented;
        }
        
        for (final de.aitools.aq.wikipedia.xml.RevisionType reverted
            : revert.revertedRevisions) {
          ++this.revertedTotal;
          
          if (WikipediaUtil.isAnonymousRevision(reverted)) {
            ++this.revertedAnonymousUser;
          } else {
            if (WikipediaUtil.isDeleted(reverted)
                || !BotList.contains(reverted.getContributor().getUsername())) {
              ++this.revertedRegisteredUser;
            } else {
              ++this.revertedBot;
            }
          }
        }
      }
    }

    public String makeRevertsTableRow() {
      final StringBuilder builder = new StringBuilder();
      builder.append(this.description);
      builder.append('\t').append(this.reverts);
      builder.append('\t').append(this.revertsCommented);
      builder.append('\t').append(this.revertsVandalismCommented);
      builder.append('\n');
      
      return builder.toString();
    }

    public String makeRevertedTableRow() {
      final StringBuilder builder = new StringBuilder();
      builder.append(this.description);
      builder.append('\t').append(this.revertedTotal);
      builder.append('\t').append(this.revertedAnonymousUser);
      builder.append('\t').append(this.revertedRegisteredUser);
      builder.append('\t').append(this.revertedBot);
      builder.append('\n');
      
      return builder.toString();
    }
    
  }
  
  private static class RevertFilter extends RevertCounter {
    
    private RevertMarshaller marshaller;
    
    private Predicate<Revert> predicate;
    
    public RevertFilter(final String description,
        final Predicate<Revert> predicate, final File outputDirectory)
    throws NullPointerException, XMLStreamException, JAXBException, IOException {
      super(description);
      
      this.predicate = predicate;
      final File marshallerOutput = new File(outputDirectory, "reverts-" +
          description.replaceAll("((And w)|(W))ithout ", "").replace(' ', '-')
            + ".xml.gz");
      this.marshaller = new RevertMarshaller(marshallerOutput);
    }
    
    public Set<Revert> filter(final List<Revert> reverts)
    throws NullPointerException, XMLStreamException, JAXBException, IOException {
      Set<Revert> filtered = Collections.emptySet();
      if (this.predicate != null) {
        filtered = Revert.filter(reverts, this.predicate);
        synchronized (this.marshaller) {
          for (final Revert revert : filtered) {
            this.marshaller.accept(revert.toRevertType());
          }
        }
      }
      
      for (final Revert revert : reverts) {
        this.count(revert);
      }

      return filtered;
    }
    
    public void close() throws Exception {
      if (this.marshaller != null) {
        this.marshaller.close();
      }
    }
    
  }
  
  private static class RevertSelfCorrectionUpdater extends RevertCounter {
    
    private RevertMarshaller marshaller;

    public RevertSelfCorrectionUpdater(final File outputDirectory)
    throws NullPointerException, XMLStreamException, JAXBException, IOException {
      super("Changing immediate self reverted reverts");
      this.marshaller = new RevertMarshaller(new File(outputDirectory,
          "reverts-self-corrected-updates.xml.gz"));
    }
    
    public void update(final List<Revert> reverts) throws XMLStreamException {

      reverts.sort(new Comparator<Revert>() {
        @Override
        public int compare(final Revert o1, final Revert o2) {
          return o1.endIndex - o2.endIndex;
        }
      });
      
      for (int i = 0; i < reverts.size(); ++i) {
        final Revert revert = reverts.get(i);
        final String contributor = WikipediaUtil.getContributor(revert.end);
        
        int j = i + 1;
        while (j < reverts.size()) {
          final Revert other = reverts.get(j);
          if ((other.endIndex != (revert.endIndex + (j - i)))
              || !WikipediaUtil.getContributor(other.end).equals(contributor)) {
            break;
          }
          ++j;
        }
        --j;
        
        if (j > i) {
          final Revert merged = Revert.merge(reverts.get(j), revert);
          reverts.set(i, merged);
          synchronized (this.marshaller) {
            this.marshaller.accept(revert.toRevertType());
            Revert.removeFromLinked(revert);
            for (int delete = j - i; delete > 0; --delete) {
              final Revert deleted = reverts.remove(i + 1);
              this.marshaller.accept(deleted.toRevertType());
              Revert.removeFromLinked(deleted);
            }

            this.marshaller.accept(merged.toRevertType());
            this.marshaller.getWriter().writeCharacters("\n");
          }
        }
      }

      reverts.sort(new Comparator<Revert>() {
        @Override
        public int compare(final Revert o1, final Revert o2) {
          return o1.startIndex - o2.startIndex;
        }
      });

      for (final Revert revert : reverts) {
        this.count(revert);
      }
    }
    
    public void close() throws Exception {
      if (this.marshaller != null) {
        this.marshaller.close();
      }
    }
    
  }
  
  public static void main(final String[] args)
  throws Exception  {
    if (args.length < 3) {
      System.err.println("Usage:");
      System.err.println("  <num-threads> <wikipedia-xml-file> [<wikipedia-xml-file> [...]] <output-dir>");
      System.exit(1);
    }

    final int numThreads = Integer.parseInt(args[0]);
    final Queue<File> inputFiles = new ConcurrentLinkedQueue<File>();
    for (int a = 1; a < args.length - 1; ++a) {
      inputFiles.add(new File(args[a]));
    }
    final File outputDirectory = new File(args[args.length - 1]);
    outputDirectory.mkdirs();
    
    try (final ExtractReverts extractor = new ExtractReverts(outputDirectory)) {
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
                extractor.extractPages(inputFile, outputFile);
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
      System.out.println(new Date() + "  START  WRITING COUNTS");
    }
    System.out.println(new Date() + "  DONE");
  }

}
