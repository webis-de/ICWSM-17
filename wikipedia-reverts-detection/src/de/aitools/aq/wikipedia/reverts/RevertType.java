package de.aitools.aq.wikipedia.reverts;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {
    "pageId",
    "hasRevertComment",
    "hasVandalismComment",
    "start",
    "reverted",
    "end"
})
public class RevertType {
  
  private BigInteger pageId;
  
  private boolean hasRevertComment;
  
  private boolean hasVandalismComment;

  private RevisionType start;
  
  private List<RevisionType> reverted;
  
  private RevisionType end;
  
  public RevertType() {
    this.start = null;
    this.end = null;
    this.reverted = new ArrayList<>();
  }
  
  @XmlAttribute(required = true)
  public BigInteger getPageId() {
    return pageId;
  }

  @XmlAttribute(required = true)
  public boolean getHasRevertComment() {
    return hasRevertComment;
  }

  @XmlAttribute(required = true)
  public boolean getHasVandalismComment() {
    return hasVandalismComment;
  }

  @XmlElement(required = true)
  public RevisionType getStart() {
    return this.start;
  }

  @XmlElement(required = true)
  public RevisionType getEnd() {
    return this.end;
  }

  @XmlElement(required = true)
  public List<RevisionType> getReverted() {
    return this.reverted;
  }
  
  public void setPageId(final BigInteger pageId) {
    this.pageId = pageId;
  }
  
  public void setHasRevertComment(final boolean hasRevertComment) {
    this.hasRevertComment = hasRevertComment;
  }
  
  public void setHasVandalismComment(final boolean hasVandalismComment) {
    this.hasVandalismComment = hasVandalismComment;
  }
  
  public void setStart(final RevisionType revision)
  throws NullPointerException {
    if (revision == null) { throw new NullPointerException(); }
    this.start = revision;
  }
  
  public void setEnd(final RevisionType revision)
  throws NullPointerException {
    if (revision == null) { throw new NullPointerException(); }
    this.end = revision;
  }

}
