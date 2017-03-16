package de.aitools.aq.geolocating.wikipedia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class PageType {
  
  private BigInteger id;
  
  private List<RevisionType> revisions;
  
  public PageType() {
    this.id = null;
    this.revisions = new ArrayList<>();
  }
  
  public PageType(final de.aitools.aq.wikipedia.reverts.PageType oldPage) {
    this();
    
    this.id = oldPage.getPageId();
    for (final de.aitools.aq.wikipedia.reverts.RevisionType oldRevision
        : oldPage.getRevisions()) {
      if (oldRevision.getIndex().intValueExact() != this.revisions.size()) {
        throw new IllegalStateException(
            oldRevision.getIndex() + "!=" + this.revisions.size());
      }
      
      final RevisionType revision = new RevisionType();
      revision.setId(oldRevision.getId());
      revision.setTimestamp(oldRevision.getTimestamp());
      revision.setContributor(oldRevision.getContributor());

      final BigInteger revertsToIndex = oldRevision.getRevertsToIndex();
      if (revertsToIndex != null) {
        final RevertedType revertedType = new RevertedType();
        revertedType.setRevertRevisionId(
            oldRevision.getId());
        revertedType.setWithRevertComment(
            oldRevision.getHasRevertComment());
        revertedType.setWithVandalismComment(
            oldRevision.getHasVandalismComment());
        
        final Iterator<RevisionType> reverted =
            this.revisions.listIterator(revertsToIndex.intValueExact());
        reverted.next(); // The target is not reverted
        while (reverted.hasNext()) {
          reverted.next().setReverted(revertedType);
        }
      }
      
      this.revisions.add(revision);
    }
  }
  
  @XmlAttribute(required = true)
  public BigInteger getId() {
    return id;
  }

  @XmlElement(required = true, name = "revision")
  public List<RevisionType> getRevisions() {
    return this.revisions;
  }
  
  public void setId(final BigInteger id) {
    this.id = id;
  }

}
