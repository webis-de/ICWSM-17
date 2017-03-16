package de.aitools.aq.wikipedia;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import de.aitools.aq.wikipedia.xml.PageType;
import de.aitools.aq.xml.PartialUnmarshaller;

/**
 * Implements an Unmarshaller that reads {@link PageType} elements from a
 * Wikipedia dump
 *
 * @author johannes.kiesel@uni.weimar.de
 */
public class PageUnmarshaller extends PartialUnmarshaller<PageType> {
  
  private static final String PAGE_TYPE_LOCAL_NAME = "page";

  /**
   * Creates a new PageUnmarshaller for given input file.
   * <p>
   * If the filename ends in <tt>.gz</tt>, data will be GZIP-uncompressed
   * in-memory.
   * </p>
   * @param inputFile The XML instance document
   * @throws XMLStreamException When an error occurs on parsing to the first
   * target element
   * @throws JAXBException When the JAXB context or the internal Unmarshaller
   * could not be created
   * @throws IOException When the input file could not be opened for reading
   */
  public PageUnmarshaller(final File inputFile)
  throws XMLStreamException, JAXBException, IOException {
    super(inputFile, PAGE_TYPE_LOCAL_NAME, PageType.class);
  }

  /**
   * Creates a new PageUnmarshaller for given input stream.
   * @param inputStream The XML instance document
   * @throws XMLStreamException When an error occurs on parsing to the first
   * target element
   * @throws JAXBException When the JAXB context or the internal Unmarshaller
   * could not be created
   */
  public PageUnmarshaller(final InputStream inputStream)
  throws XMLStreamException, JAXBException {
    super(inputStream, PAGE_TYPE_LOCAL_NAME, PageType.class);
  }

}
