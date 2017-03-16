package de.aitools.aq.geolocating.wikipedia;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.aitools.aq.geolocating.Geolocation;
import de.aitools.aq.wikipedia.WikipediaUtil;
import de.aitools.aq.xml.XmlInstantAdapter;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name="revision")
public class RevisionType {

  private BigInteger id;
  
  private Instant timestamp;
  
  private String contributor;
  
  private RevertedType reverted;
  
  private Geolocation geolocated;
  
  public RevisionType() {
    this.id = null;
    this.timestamp = null;
    this.contributor = null;
    this.reverted = null;
    this.geolocated = null;
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

  @XmlAttribute(required = true)
  public String getContributor() {
    return this.contributor;
  }

  @XmlElement(required = false)
  public RevertedType getReverted() {
    return this.reverted;
  }

  @XmlElement(required = false)
  public Geolocation getGeolocated() {
    return this.geolocated;
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
  
  public void setReverted(final RevertedType reverted) {
    this.reverted = reverted;
  }
  
  public void setGeolocated(final Geolocation geolocated) {
    this.geolocated = geolocated;
  }
  
  public boolean hasIp() {
    final String contributor = this.getContributor();
    if (contributor == null) {
      return false;
    }
    return contributor.startsWith(WikipediaUtil.CONTRIBUTOR_PREFIX_ANONYMOUS);
  }
  
  public boolean hasIpV4() {
    final String contributor = this.getContributor();
    if (contributor == null) {
      return false;
    }
    return (contributor.matches(
        WikipediaUtil.CONTRIBUTOR_PREFIX_ANONYMOUS + "(\\d{1,3}\\.){3}\\d{1,3}"));
  }
  
  public String getIp() throws IllegalStateException {
    if (!this.hasIp()) { throw new IllegalStateException(); }
    return this.getContributor().substring(
        WikipediaUtil.CONTRIBUTOR_PREFIX_ANONYMOUS.length());
  }
  
  public InetAddress getIpAddress()
  throws IllegalStateException, UnknownHostException {
    return InetAddress.getByName(this.getIp());
  }


}
