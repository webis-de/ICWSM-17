package de.aitools.aq.wikipedia.reverts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.aitools.aq.wikipedia.WikipediaUtil;
import de.aitools.aq.wikipedia.xml.RevisionType;

public class RevertChain {

  private static final String REVISION_SEPARATOR = " ";

  private static final String REVISION_FIELDS_SEPARATOR = ":";
  
  private static final String REVISION_FIELD_BLANK = "_";
  
  private static final String REVISION_TEXT_EMPTY = "X";
  
  private static final String REVISION_USER_ANONYMOUS = "A";
  
  private static final String REVISION_USER_REGISTERED = "R";
  
  private static final String REVISION_USER_DELETED = "D";
  
  public final String chain;
  
  private RevertChain(final String chain) {
    if (chain == null) { throw new NullPointerException(); }
    this.chain = chain;
  }
  
  @Override
  public String toString() { 
    return this.chain;
  }

  public static List<RevertChain> createInterleavedRevertChains(
      final List<Revert> reverts)
  throws NullPointerException {
    final List<RevertChain> chains = new ArrayList<>();
    
    final Set<Revert> handled = new HashSet<>();
    for (final Revert revert : reverts) {
      if (revert.isInterleaved() && !handled.contains(revert)) {
        final TreeSet<Revert> chainReverts = new TreeSet<>();
        RevertChain.addInterleavedRevertsRecursive(revert, chainReverts, handled);
        final String chain = String.join(
            REVISION_SEPARATOR, RevertChain.compileChain(chainReverts));
        chains.add(new RevertChain(chain));
      }
    }
    
    return chains;
  }

  public static List<RevertChain> createEnclosingRevertChains(
      final List<Revert> reverts)
  throws NullPointerException {
    final List<RevertChain> chains = new ArrayList<>();
    
    final Set<Revert> handled = new HashSet<>();
    for (final Revert revert : reverts) {
      if ((revert.isEnclosed() || revert.isEnclosing())
          && !handled.contains(revert)) {
        final TreeSet<Revert> chainReverts = new TreeSet<>();
        RevertChain.addEnclosedRevertsRecursive(revert, chainReverts, handled);
        final String chain = String.join(
            REVISION_SEPARATOR, RevertChain.compileChain(chainReverts));
        chains.add(new RevertChain(chain));
      }
    }
    
    return chains;
  }
  
  private static void addInterleavedRevertsRecursive(
      final Revert revert, final Set<Revert> chainReverts,
      final Set<Revert> handled) {
    handled.add(revert);
    chainReverts.add(revert);
    for (final Revert interleaved : revert.interleavesWith) {
      if (!handled.contains(interleaved)) {
        RevertChain.addInterleavedRevertsRecursive(
            interleaved, chainReverts, handled);
      }
    }
  }
  
  private static void addEnclosedRevertsRecursive(
      final Revert revert, final Set<Revert> chainReverts,
      final Set<Revert> handled) {
    handled.add(revert);
    chainReverts.add(revert);
    for (final Revert enclosed : revert.enclosedBy) {
      if (!handled.contains(enclosed)) {
        RevertChain.addEnclosedRevertsRecursive(
            enclosed, chainReverts, handled);
      }
    }
    for (final Revert enclosing : revert.encloses) {
      if (!handled.contains(enclosing)) {
        RevertChain.addEnclosedRevertsRecursive(
            enclosing, chainReverts, handled);
      }
    }
  }
  
  private static String[] compileChain(final Iterable<Revert> reverts) {
    final Map<String, String> userMap = new HashMap<>();
    final Map<String, String> contentMap = new HashMap<>();
    
    final int start = reverts.iterator().next().startIndex;
    int end = 0;
    for (final Revert revert : reverts) {
      if (revert.endIndex > end) {
        end = revert.endIndex;
      }
    }
    final int length = end - start + 1;

    final String[] revisions = new String[length];
    for (final Revert revert : reverts) {
      final int startIndex = revert.startIndex - start;
      final int endIndex = revert.endIndex - start;

      if (revisions[startIndex] == null
          || revisions[startIndex].matches(".*:_:_")) {
        revisions[startIndex] = RevertChain.compileRevertRevision(
            revert.start, userMap, contentMap);
      }
      int index = startIndex + 1;
      for (final RevisionType revision : revert.revertedRevisions) {
        if (revisions[index] == null) {
          revisions[index] =
              RevertChain.compileNonRevertRevision(revision, userMap);
        }
        ++index;
      }
      revisions[endIndex] = RevertChain.compileRevertEndRevision(
          revert.end, startIndex, userMap, contentMap);
    }
    
    return revisions;
  }
  
  private static String compileNonRevertRevision(
      final RevisionType revision,
      final Map<String, String> userMap) {
    return String.join(REVISION_FIELDS_SEPARATOR,
        RevertChain.getUser(revision, userMap),
        REVISION_FIELD_BLANK,
        REVISION_FIELD_BLANK);
  }
  
  private static String compileRevertRevision(
      final RevisionType revision,
      final Map<String, String> userMap, final Map<String, String> contentMap) {
    return String.join(REVISION_FIELDS_SEPARATOR,
        RevertChain.getUser(revision, userMap),
        RevertChain.getText(revision, contentMap),
        REVISION_FIELD_BLANK);
  }
  
  private static String compileRevertEndRevision(
      final RevisionType revision, final int revertStartIndex,
      final Map<String, String> userMap, final Map<String, String> contentMap) {
    return String.join(REVISION_FIELDS_SEPARATOR,
        RevertChain.getUser(revision, userMap),
        RevertChain.getText(revision, contentMap),
        String.valueOf(revertStartIndex));
  }
  
  private static String getText(
      final RevisionType revision,
      final Map<String, String> contentMap) {
    if (WikipediaUtil.isBlank(revision) || WikipediaUtil.isDeleted(revision)) {
      return REVISION_TEXT_EMPTY;
    } else {
      final String sha1 = revision.getSha1();
      String text = contentMap.get(sha1);
      if (text == null) {
        text = String.valueOf(contentMap.size());
        contentMap.put(sha1, text);
      }
      return text;
    }
  }
  
  private static String getUser(
      final RevisionType revision,
      final Map<String, String> userMap) {
    final String contributor = WikipediaUtil.getContributor(revision);
    String contributorShort = userMap.get(contributor);
    if (contributorShort == null) {
      final int idShort = userMap.size();  
      if (contributor.startsWith(
          WikipediaUtil.CONTRIBUTOR_PREFIX_ANONYMOUS)) {
        contributorShort = REVISION_USER_ANONYMOUS + idShort;
      } else if (contributor.startsWith(
          WikipediaUtil.CONTRIBUTOR_PREFIX_REGISTERED)) {
        contributorShort = REVISION_USER_REGISTERED + idShort;
      } else if (contributor.equals(
          WikipediaUtil.CONTRIBUTOR_DELETED)) {
        contributorShort = REVISION_USER_DELETED;
      } else {
        throw new IllegalStateException(
            "Unknown contributor prefix: " + contributor);
      }
      userMap.put(contributor, contributorShort);
    }
    return contributorShort;
  }

}
