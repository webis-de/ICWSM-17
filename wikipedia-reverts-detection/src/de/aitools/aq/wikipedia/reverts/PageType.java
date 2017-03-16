package de.aitools.aq.wikipedia.reverts;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import de.aitools.aq.wikipedia.WikipediaUtil;

public class PageType {
  
  private BigInteger pageId;
  
  private List<RevisionType> revisions;
  
  public PageType() {
    this.pageId = null;
    this.revisions = new ArrayList<>();
  }
  
  @XmlAttribute(required = true)
  public BigInteger getPageId() {
    return pageId;
  }

  @XmlElement(required = true, name = "revision")
  public List<RevisionType> getRevisions() {
    return this.revisions;
  }
  
  public void setPageId(final BigInteger pageId) {
    this.pageId = pageId;
  }
  
  public static PageType fromPage(
      final de.aitools.aq.wikipedia.xml.PageType page) {
    final PageType data = new PageType();
    data.setPageId(page.getId());
    
    final List<RevisionType> revisions = data.getRevisions();
    int index = 0;
    for (final de.aitools.aq.wikipedia.xml.RevisionType revision
        : WikipediaUtil.getRevisions(page)) {
      revisions.add(new RevisionType(index, revision));
      ++index;
    }
    
    return data;
  }
  
  

}
