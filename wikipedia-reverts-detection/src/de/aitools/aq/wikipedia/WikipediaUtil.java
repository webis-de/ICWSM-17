package de.aitools.aq.wikipedia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import de.aitools.aq.wikipedia.xml.CommentType;
import de.aitools.aq.wikipedia.xml.ContributorType;
import de.aitools.aq.wikipedia.xml.PageType;
import de.aitools.aq.wikipedia.xml.RevisionType;

/**
 * Utility class with static methods for the JAXB-generated Wikipedia classes. 
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class WikipediaUtil {
  
  /**
   * Prefix added by {@link #getContributor(RevisionType)} for revisions by
   * anonymous users.
   */
  public static final String CONTRIBUTOR_PREFIX_ANONYMOUS = "ip:";

  /**
   * Prefix added by {@link #getContributor(RevisionType)} for revisions by
   * registered users.
   */
  public static final String CONTRIBUTOR_PREFIX_REGISTERED = "id:";

  /**
   * Prefix added by {@link #getContributor(RevisionType)} for revisions by
   * deleted users.
   */
  public static final String CONTRIBUTOR_DELETED = "deleted";
  
  private WikipediaUtil() { }
  
  /**
   * Extracts the list of revisions from a Wikipedia page.
   * @param page The page
   * @return All revisions in the order that they appear for the page 
   * @throws NullPointerException if the page is <tt>null</tt>
   */
  public static List<RevisionType> getRevisions(final PageType page)
  throws NullPointerException {
    final List<Object> elements = page.getRevisionOrUpload();
    final List<RevisionType> revisions = new ArrayList<>(elements.size());
    for (final Object element : elements) {
      if (element instanceof RevisionType) {
        final RevisionType revision = (RevisionType) element;
        revisions.add(revision);
      }
    }
    return revisions;
  }

  /**
   * Checks if the revision has an empty text.
   * @param revision The revision
   * @return True if so
   * @throws NullPointerException if the revision is <tt>null</tt>
   */
  public static boolean isBlank(final RevisionType revision)
  throws NullPointerException {
    final BigInteger bytes = revision.getText().getBytes();
    return bytes != null && bytes.intValueExact() == 0;
  }

  /**
   * Checks if the revision has been deleted.
   * @param revision The revision
   * @return True if so
   * @throws NullPointerException if the revision is <tt>null</tt>
   */
  public static boolean isDeleted(final RevisionType revision)
  throws NullPointerException {
    return revision.getText().getDeleted() != null;
  }

  /**
   * Checks if the revision was made by an anonymous user.
   * @param revision The revision
   * @return True if so
   * @throws NullPointerException if the revision is <tt>null</tt>
   */
  public static boolean isAnonymousRevision(final RevisionType revision)
  throws NullPointerException {
    return revision.getContributor().getIp() != null;
  }

  /**
   * Checks if the revision was made by an anonymous user with an IPv6 address.
   * @param revision The revision
   * @return True if so
   * @throws NullPointerException if the revision is <tt>null</tt>
   */
  public static boolean isAnonymousIpv6Revision(final RevisionType revision)
  throws NullPointerException {
    final boolean anonymous = WikipediaUtil.isAnonymousRevision(revision);
    if (!anonymous) {
      return false;
    }
    
    final String ip = revision.getContributor().getIp();
    return ip.contains(":");
  }

  /**
   * Gets the time of the revision in milliseconds since the epoch.
   * <p>
   * Note that Wikipedia stores only the server time.
   * </p>
   * @param revision The revision
   * @return The time
   * @throws NullPointerException if the revision is <tt>null</tt>
   */
  public static long getTimeInMillis(final RevisionType revision)
  throws NullPointerException {
    return revision.getTimestamp().toGregorianCalendar().getTimeInMillis();
  }

  /**
   * Gets the contributor of the revision.
   * <p>
   * Adds the prefix {@link #CONTRIBUTOR_PREFIX_ANONYMOUS} or
   * {@link #CONTRIBUTOR_PREFIX_REGISTERED} to distinguish anonymous and
   * registered users. For registered users, it returns the ID.
   * </p>
   * @param revision The revision
   * @return A unique string for the contributor
   * @throws NullPointerException if the revision is <tt>null</tt>
   */
  public static String getContributor(final RevisionType revision)
  throws NullPointerException {
    final ContributorType contributor = revision.getContributor();
    if (WikipediaUtil.isAnonymousRevision(revision)) {
      return CONTRIBUTOR_PREFIX_ANONYMOUS + contributor.getIp();
    } else if (contributor.getId() != null) {
      return CONTRIBUTOR_PREFIX_REGISTERED + contributor.getId();
    } else if (contributor.getDeleted() != null) {
      return CONTRIBUTOR_DELETED;
    } else {
      throw new IllegalStateException(
          "Detected contributor that is neither anonymous, nor registered, "
          + "nor deleted.");
    }
  }

  /**
   * Checks if the comment of given revision implies that it reverts previous
   * revisions.
   * <p>
   * Checks for the Strings "revert" and "rv" like in the paper
   * kittur07-conflict-and-coordination-in-wikipedia.
   * </p>
   * @param revision The revision to check
   * @return If the comment implies that the revision reverts
   * @throws NullPointerException If revision is <tt>null</tt>
   */
  public static boolean hasRevertComment(final RevisionType revision)
  throws NullPointerException {
    final CommentType comment = revision.getComment();
    if (comment == null) {
      return false;
    }

    final String commentLowerCase = comment.getValue().toLowerCase();
    
    return commentLowerCase.contains("revert")
        || commentLowerCase.contains("rv");
  }

  /**
   * Checks if the comment of given revision implies that it reverts previous
   * vandalism.
   * <p>
   * Checks for the Strings "vandal" and "rvv" like in the paper
   * kittur07-conflict-and-coordination-in-wikipedia.
   * </p>
   * @param revision The revision to check
   * @return If the comment implies that the revision reverts vandalism
   * @throws NullPointerException If revision is <tt>null</tt>
   */
  public static boolean hasVandalismComment(final RevisionType revision)
  throws NullPointerException {
    final CommentType comment = revision.getComment();
    if (comment == null) {
      return false;
    }

    final String commentLowerCase = comment.getValue().toLowerCase();
    
    return commentLowerCase.contains("vandal")
        || commentLowerCase.contains("rvv");
  }

}
