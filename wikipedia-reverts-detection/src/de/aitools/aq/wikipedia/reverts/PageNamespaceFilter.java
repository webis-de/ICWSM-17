package de.aitools.aq.wikipedia.reverts;

import java.math.BigInteger;
import java.util.function.Predicate;

import de.aitools.aq.wikipedia.xml.PageType;

public class PageNamespaceFilter implements Predicate<PageType> {

  private static final BigInteger PAGE_NAMESPACE = BigInteger.ZERO; 

  @Override
  public boolean test(final PageType page) {
    return page.getNs().equals(PAGE_NAMESPACE);
  }

}
