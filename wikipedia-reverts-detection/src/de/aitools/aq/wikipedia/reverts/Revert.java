package de.aitools.aq.wikipedia.reverts;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import de.aitools.aq.wikipedia.WikipediaUtil;
import de.aitools.aq.wikipedia.xml.CommentType;
import de.aitools.aq.wikipedia.xml.PageType;
import de.aitools.aq.wikipedia.xml.RevisionType;
import de.aitools.aq.wikipedia.xml.TextType;

/**
 * A structure that corresponds to one Wikipedia full page revert.
 * 
 * Objects of this class are directly created from a Wikipedia {@link PageType}
 * object using {@link #createAll(PageType)}. They use pointers to the
 * {@link RevisionType}s of the page. These objects are made for filtering
 * the reverts. For serialization, create {@link RevertType}s from them using
 * {@link #toRevertType()} or {@link #toRevertsType(Iterable)}.
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class Revert implements Comparable<Revert> {
  
  private static final String REVERT_COMPARISON_STRING_EMPTY = "";
  
  public final int pageId;

  public final int startIndex;
  
  public final int endIndex;

  public final RevisionType start;
  
  public final RevisionType end;
  
  public final List<RevisionType> revertedRevisions;
  
  public final Set<Revert> interleavesWith;
  
  public final Set<Revert> encloses;
  
  public final Set<Revert> enclosedBy;

  private Revert(
      final int pageId,
      final int startIndex, final RevisionType start,
      final int endIndex, final RevisionType end,
      final List<RevisionType> revertedRevisions) {
    this.pageId = pageId;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.start = start;
    this.end = end;
    this.revertedRevisions = revertedRevisions;
    
    this.interleavesWith = new HashSet<>();
    this.encloses = new HashSet<>();
    this.enclosedBy = new HashSet<>();
  }

  private Revert(
      final int pageId,
      final int startIndex, final int endIndex,
      final List<RevisionType> revisions) {
    this.pageId = pageId;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.start = revisions.get(startIndex);
    this.end = revisions.get(endIndex);

    this.revertedRevisions = new ArrayList<>();
    for (int r = startIndex + 1; r < endIndex; ++r) {
      this.revertedRevisions.add(revisions.get(r));
    }
    
    this.interleavesWith = new HashSet<>();
    this.encloses = new HashSet<>();
    this.enclosedBy = new HashSet<>();
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("<revert");
    builder.append(" start=\"").append(this.startIndex).append("\"");
    builder.append(" end=\"").append(this.endIndex).append("\"");
    builder.append(" length=\"").append(this.getLength()).append("\"");
    builder.append(" revertToEmpty=\"").append(this.isRevertToEmpty()).append("\"");
    builder.append(" interleaved=\"").append(this.isInterleaved()).append("\"");
    builder.append(" enclosed=\"").append(this.isEnclosed()).append("\"");
    builder.append(" enclosing=\"").append(this.isEnclosing()).append("\"");
    builder.append("/>");
    return builder.toString();
  }

  @Override
  public int compareTo(final Revert other) {
    return this.startIndex - other.startIndex;
  }
  
  public int getLength() {
    return this.revertedRevisions.size();
  }
  
  public boolean isInterleaved() {
    return !this.interleavesWith.isEmpty();
  }
  
  public boolean isEnclosed() {
    return !this.enclosedBy.isEmpty();
  }
  
  public boolean isEnclosing() {
    return !this.encloses.isEmpty();
  }
  
  public boolean isRevertToEmpty() {
    return WikipediaUtil.isBlank(this.end) || WikipediaUtil.isDeleted(this.end);
  }
  
  public int getNumRevertedUsers() {
    final Set<String> users = new HashSet<>();

    for (final RevisionType revision : this.revertedRevisions) {
      users.add(WikipediaUtil.getContributor(revision));
    }
    
    return users.size();
  }
  
  public boolean isRevertingOnlyOneUser() {
    if (this.revertedRevisions.size() <= 1) {
      return true;
    }

    final String firstUser =
        WikipediaUtil.getContributor(this.revertedRevisions.get(0));

    final Iterator<RevisionType> otherRevisions =
        this.revertedRevisions.listIterator(1);
    while (otherRevisions.hasNext()) {
      final String user = WikipediaUtil.getContributor(otherRevisions.next());
      if (!firstUser.equals(user)) {
        return false;
      }
    }
    
    return true;
  }
  
  public boolean isRevertedByNextUser() {
    for (final Revert interleavedRevert : this.interleavesWith) {
      if (interleavedRevert.startIndex == this.endIndex - 1
          && interleavedRevert.isRevertingOnlyOneUser()) {
        return true;
      }
    }
    return false;
  }
  
  public boolean isRenamingRevert() {
    if (this.endIndex - this.startIndex == 1) {
      final TextType startText = this.start.getText();
      final TextType endText = this.end.getText();
      if (startText != null && endText != null) {
        final String startTextId = startText.getId();
        final String endTextId = endText.getId();
        if (startTextId != null && endTextId != null) {
          if (startTextId.equals(endTextId)) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  public long getTimeSpanInSeconds() {
    final long timeStart = WikipediaUtil.getTimeInMillis(this.start);
    final long timeEnd = WikipediaUtil.getTimeInMillis(this.end);
    return (timeEnd - timeStart) / 1000; // Dump contains only seconds
  }
  
  public boolean isDoubleSubmission() {
    if (this.endIndex - this.startIndex != 1) {
      return false;
    }
    
    // same author
    final String startContributor = WikipediaUtil.getContributor(this.start);
    final String endContributor = WikipediaUtil.getContributor(this.end);
    if (!startContributor.equals(endContributor)) {
      return false;
    }
    
    // same comment
    final CommentType startComment = this.start.getComment();
    final CommentType endComment = this.end.getComment();
    if (startComment != null) {
      if (endComment == null) {
        return false;
      }
      
      // both != null
      final String startCommentValue = this.start.getComment().getValue();
      final String endCommentValue = this.end.getComment().getValue();
      if (startCommentValue != null) {
        if (endCommentValue == null) {
          return false;
        }

        // both != null
        if (!startCommentValue.equals(endCommentValue)) {
          return false;
        }
      } else {
        if (endCommentValue != null) {
          return false;
        }
      }
    } else {
      if (endComment != null) {
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * Checks if the revert reverts the revisions of several (&gt; 1) users and is
   * itself reverted to the previous revision by the next user.
   * @return If so
   */
  public boolean isUnacceptedRevert() {
    return this.isRevertedByNextUser() && !this.isRevertingOnlyOneUser();
  }

  /**
   * Checks if the comment of the last revision implies that it reverts previous
   * revisions. See {@link WikipediaUtil#hasRevertComment(RevisionType)} for more
   * information on the check.
   * @return If the comment of the last revision implies a revert
   */
  public boolean isCommentRevert() {
    return WikipediaUtil.hasRevertComment(this.end);
  }

  /**
   * Checks if the comment of the last revision implies that it reverts previous
   * vandalism. See {@link WikipediaUtil#hasVandalismComment(RevisionType)}
   * for more information on the check.
   * @return If the comment of the last revision implies a vandalism revert
   */
  public boolean isVandalismCommentRevert() {
    return WikipediaUtil.hasVandalismComment(this.end);
  }

  /**
   * Checks if all reverted revisions are made by the user that reverted.
   * @return True if so
   */
  public boolean isSelfRevert()
  throws NullPointerException {
    final String endContributor = WikipediaUtil.getContributor(this.end);
    for (final RevisionType revision : this.revertedRevisions) {
      if (!endContributor.equals(WikipediaUtil.getContributor(revision))) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Creates a {@link RevertType} for this revert.
   * 
   * The RevertType can be serialized using JAXB.
   * @return The RevertType
   */
  public RevertType toRevertType() {
    final RevertType data = new RevertType();
    data.setPageId(BigInteger.valueOf(this.pageId));
    data.setHasRevertComment(this.isCommentRevert());
    data.setHasVandalismComment(this.isVandalismCommentRevert());
    
    int index = this.startIndex;
    data.setStart(
        new de.aitools.aq.wikipedia.reverts.RevisionType(index, this.start));
    final List<de.aitools.aq.wikipedia.reverts.RevisionType> reverted =
        data.getReverted();
    ++index;
    for (final RevisionType revision : this.revertedRevisions) {
      final de.aitools.aq.wikipedia.reverts.RevisionType simpleRevision =
          new de.aitools.aq.wikipedia.reverts.RevisionType(index, revision);
      ++index;
      reverted.add(simpleRevision);
    }
    
    final de.aitools.aq.wikipedia.reverts.RevisionType revertRevision =
        new de.aitools.aq.wikipedia.reverts.RevisionType(index, this.end);
    revertRevision.setRevertsToIndex(this.startIndex);
    data.setEnd(revertRevision);
    return data;
  }
  
  /**
   * Keeps all those reverts for which the predicate is <tt>true</tt>.
   * 
   * Updates the enclosed/enclosing and interleaved list accordingly.
   * @param reverts The reverts to filter
   * @param predicate Predicate which reverts to keep
   * @return The removed reverts
   * @throws NullPointerException If the reverts or the predicate are
   * <tt>null</tt>
   */
  public static Set<Revert> filter(
      final List<Revert> reverts, final Predicate<Revert> predicate)
  throws NullPointerException {
    if (reverts == null) { throw new NullPointerException(); }
    if (predicate == null) { throw new NullPointerException(); }
    
    final Set<Revert> keep = new HashSet<>(); 
    final Set<Revert> filter = new HashSet<>();
    Revert.filter(reverts, predicate, keep, filter, true);
    
    return filter;
  }
  
  private static void filter(
      final Iterable<Revert> reverts, final Predicate<Revert> predicate,
      final Set<Revert> keep, final Set<Revert> filter, final boolean recursive)
  throws NullPointerException {
    final Iterator<Revert> revertsIterator = reverts.iterator();
    while (revertsIterator.hasNext()) {
      final Revert revert = revertsIterator.next();
      if (Revert.filterOut(revert, predicate, keep, filter)) {
        revertsIterator.remove();
      } else if (recursive) {
        Revert.filter(revert.enclosedBy, predicate, keep, filter, false);
        Revert.filter(revert.encloses, predicate, keep, filter, false);
        Revert.filter(revert.interleavesWith, predicate, keep, filter, false);
      }
    }
  }
  
  private static boolean filterOut(
      final Revert revert, final Predicate<Revert> predicate,
      final Set<Revert> keep, final Set<Revert> filter) {
    if (keep.contains(revert)) {
      return false;
    } else if (filter.contains(revert)) {
      return true;
    } else if (predicate.test(revert)) {
      keep.add(revert);
      return false;
    } else {
      filter.add(revert);
      return true;
    }
  }

  /**
   * Extracts all full page reverts from a given page.
   * 
   * This function matches revisions solely by their hash sum and performs no
   * further cleaning.
   * @param page The page from which to get the reverts
   * @return All found reverts
   * @throws NullPointerException if the page is <tt>null</tt>
   */
  public static List<Revert> createAll(final PageType page)
  throws NullPointerException {
    final int pageId = page.getId().intValueExact();
    final List<Revert> reverts = new ArrayList<>();
    final List<RevisionType> revisions =
        WikipediaUtil.getRevisions(page);
    final int numRevisions = revisions.size();
    @SuppressWarnings("unchecked")
    final List<Revert>[] endsAt = new List[numRevisions];
    for (int r = 0; r < numRevisions; ++r) {
      endsAt[r] = new ArrayList<>();
    }

    for (int startIndex = 0; startIndex < numRevisions; ++startIndex) {
      final RevisionType start = revisions.get(startIndex);
      for (int endIndex = startIndex + 1; endIndex < numRevisions; ++endIndex) {
        final RevisionType end = revisions.get(endIndex);
        if (Revert.isRevertedTo(start, end)) {
          final Revert revert =
              new Revert(pageId, startIndex, endIndex, revisions);

          // interleave
          for (int r = startIndex + 1; r < endIndex; ++r) {
            for (final Revert intersected : endsAt[r]) {
              revert.interleavesWith.add(intersected);
              intersected.interleavesWith.add(revert);
            }
          }

          // encloses/enclosedBy
          for (int r = endIndex + 1; r < numRevisions; ++r) {
            for (final Revert enclosed : endsAt[r]) {
              revert.enclosedBy.add(enclosed);
              enclosed.encloses.add(revert);
            }
          }

          endsAt[endIndex].add(revert);
          reverts.add(revert);
          
          break;
        }
      }
    }

    return reverts;
  }
  
  private static boolean isRevertedTo(
      final RevisionType origin, final RevisionType target)
  throws NullPointerException {
    final String stringOrigin = Revert.getRevisionComparisonString(origin);
    final String stringTarget = Revert.getRevisionComparisonString(target);
    return stringOrigin.equals(stringTarget);
  }
  
  private static String getRevisionComparisonString(final RevisionType revision)
  throws NullPointerException {
    if (WikipediaUtil.isBlank(revision) || WikipediaUtil.isDeleted(revision)) {
      return Revert.REVERT_COMPARISON_STRING_EMPTY;
    } else {
      return revision.getSha1();
    }
  }
  
  public static void removeFromLinked(final Revert revert) {
    for (final Revert other : revert.enclosedBy) {
      other.encloses.remove(revert);
    }
    for (final Revert other : revert.encloses) {
      other.enclosedBy.remove(revert);
    }
    for (final Revert other : revert.interleavesWith) {
      other.interleavesWith.remove(revert);
    }
  }
  
  public static Revert merge(final Revert takeStart, final Revert takeEnd) {
    if (takeStart.pageId != takeEnd.pageId) { throw new IllegalArgumentException(); }
    if (takeStart.startIndex >= takeEnd.endIndex) { throw new IllegalArgumentException(); }
    if (takeStart.endIndex <= takeEnd.startIndex) { throw new IllegalArgumentException(); }
    
    final List<RevisionType> revertedRevisions = new ArrayList<>();
    int take = takeEnd.endIndex - takeStart.startIndex - 1;
    int lastIndex = takeStart.startIndex;
    for (int i = 0; take > 0 && i < takeStart.revertedRevisions.size(); ++i) {
      revertedRevisions.add(takeStart.revertedRevisions.get(i));
      ++lastIndex;
      
      take--;
    }
    for (int i = lastIndex - takeEnd.startIndex;
        take > 0 && i < takeEnd.revertedRevisions.size();
        ++i) {
      revertedRevisions.add(takeEnd.revertedRevisions.get(i));
      
      take--;
    }
    
    final Revert merged = new Revert(takeStart.pageId,
        takeStart.startIndex, takeStart.start, takeEnd.endIndex, takeEnd.end,
        revertedRevisions);

    for (final Revert other : takeStart.encloses) { Revert.update(merged, other); }
    for (final Revert other : takeStart.enclosedBy) { Revert.update(merged, other); }
    for (final Revert other : takeStart.interleavesWith) { Revert.update(merged, other); }
    for (final Revert other : takeEnd.encloses) { Revert.update(merged, other); }
    for (final Revert other : takeEnd.enclosedBy) { Revert.update(merged, other); }
    for (final Revert other : takeEnd.interleavesWith) { Revert.update(merged, other); }
      
    return merged;
  }
  
  private static void update(final Revert revert, final Revert other) {
    if (revert.startIndex < other.startIndex) {
      // <R <O
      if (other.startIndex < revert.endIndex) {
        // <R <O R>
        if (revert.endIndex < other.endIndex) {
          // <R <O R> O>
          revert.interleavesWith.add(other);
          other.interleavesWith.add(revert);
        } else {
          // <R <O O> R>
          revert.encloses.add(other);
          other.enclosedBy.add(revert);
        }
      } // else <R R> <O
    } else {
      // <O <R
      if (revert.startIndex < other.endIndex) {
        // <O <R O>
        if (other.endIndex < revert.endIndex) {
          // <O <R O> R>
          revert.interleavesWith.add(other);
          other.interleavesWith.add(revert);
        } else {
          // <O <R R> O>
          revert.enclosedBy.add(other);
          other.encloses.add(revert);
        }
      } // else <O O> <R
    }
  }

}
