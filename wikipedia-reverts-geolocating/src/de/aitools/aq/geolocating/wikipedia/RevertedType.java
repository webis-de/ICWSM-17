package de.aitools.aq.geolocating.wikipedia;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name="reverted")
public class RevertedType {

  private BigInteger revertRevisionId;
  
  private boolean withRevertComment;
  
  private boolean withVandalismComment;
  
  
  public RevertedType() {
    this.revertRevisionId = null;
    this.withRevertComment = false;
    this.withVandalismComment = false;
  }

  @XmlAttribute(required = true)
  public BigInteger getRevertRevisionId() {
    return this.revertRevisionId;
  }

  @XmlAttribute(required = true)
  public boolean getWithRevertComment() {
    return this.withRevertComment;
  }

  @XmlAttribute(required = true)
  public boolean getWithVandalismComment() {
    return this.withVandalismComment;
  }
  
  public void setRevertRevisionId(final BigInteger revertRevisionId) {
    this.revertRevisionId = revertRevisionId;
  }
  
  public void setWithRevertComment(final boolean withRevertComment) {
    this.withRevertComment = withRevertComment;
  }
  
  public void setWithVandalismComment(final boolean withVandalismComment) {
    this.withVandalismComment = withVandalismComment;
  }


}
