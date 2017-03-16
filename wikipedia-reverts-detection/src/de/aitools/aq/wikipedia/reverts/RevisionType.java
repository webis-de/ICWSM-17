package de.aitools.aq.wikipedia.reverts;

import java.math.BigInteger;
import java.time.Instant;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.aitools.aq.wikipedia.WikipediaUtil;
import de.aitools.aq.xml.XmlInstantAdapter;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name="revision")
public class RevisionType {
  
  private BigInteger index;

  private BigInteger id;
  
  private Instant timestamp;
  
  private String contributor;
  
  private BigInteger revertsToIndex;
  
  private boolean hasRevertComment;
  
  private boolean hasVandalismComment;
  
  public RevisionType() {
    this.index = null;
    this.id = null;
    this.timestamp = null;
    this.contributor = null;
    this.revertsToIndex = null;
    this.hasRevertComment = false;
    this.hasVandalismComment = false;
  }
  
  public RevisionType(final RevisionType revision) {
    this.index = revision.index;
    this.id = revision.id;
    this.timestamp = revision.timestamp;
    this.contributor = revision.contributor;
    this.revertsToIndex = revision.revertsToIndex;
    this.hasRevertComment = revision.hasRevertComment;
    this.hasVandalismComment = revision.hasVandalismComment;
  }
  
  public RevisionType(final int index,
      final de.aitools.aq.wikipedia.xml.RevisionType revision) {
    this.index = new BigInteger(String.valueOf(index));
    this.id = revision.getId();
    this.timestamp = XmlInstantAdapter.calendarToInstant(
        revision.getTimestamp());
    this.contributor = WikipediaUtil.getContributor(revision);
    this.revertsToIndex = null;
    this.hasRevertComment = WikipediaUtil.hasRevertComment(revision);
    this.hasVandalismComment = WikipediaUtil.hasVandalismComment(revision);
  }
  
  @XmlAttribute(required = true)
  public BigInteger getIndex() {
    return this.index;
  }
  
  @XmlAttribute(required = true)
  public BigInteger getId() {
    return this.id;
  }

  @XmlAttribute(required = true)
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  public Instant getTimestamp() {
    return this.timestamp;
  }
  
  public long getTimeInMillisSinceEpoch() {
    return this.getTimestamp().toEpochMilli();
  }

  @XmlAttribute(required = true)
  public String getContributor() {
    return this.contributor;
  }
  
  @XmlAttribute(required = false)
  public BigInteger getRevertsToIndex() {
    return this.revertsToIndex;
  }
  
  @XmlAttribute(required = false)
  public boolean getHasRevertComment() {
    return this.hasRevertComment;
  }
  
  @XmlAttribute(required = false)
  public boolean getHasVandalismComment() {
    return this.hasVandalismComment;
  }
  
  public void setIndex(final BigInteger index) {
    this.index = index;
  }
  
  public void setId(final BigInteger id) {
    this.id = id;
  }
  
  public void setTimestamp(final Instant timestamp) {
    this.timestamp = timestamp;
  }
  
  public void setContributor(final String contributor) {
    this.contributor = contributor;
  }
  
  public void setRevertsToIndex(final int revertsToIndex) {
    this.revertsToIndex = new BigInteger(String.valueOf(revertsToIndex));
  }
  
  public void setRevertsToIndex(final BigInteger revertsToIndex) {
    this.revertsToIndex = revertsToIndex;
  }
  
  public void setHasRevertComment(final boolean hasRevertComment) {
    this.hasRevertComment = hasRevertComment;
  }
  
  public void setHasVandalismComment(final boolean hasVandalismComment) {
    this.hasVandalismComment = hasVandalismComment;
  }
  
}
