package de.aitools.aq.geolocating.wikipedia;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import de.aitools.aq.xml.PartialUnmarshaller;

/**
 * @author johannes.kiesel@uni.weimar.de
 */
public class GeolocatedPageUnmarshaller
extends PartialUnmarshaller<PageType> {
  
  private static final String PAGE_TYPE_LOCAL_NAME = "page";

  /**
   * Creates a new GeolocatedPageUnmarshaller for given input file.
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
  public GeolocatedPageUnmarshaller(final File inputFile)
  throws XMLStreamException, JAXBException, IOException {
    super(inputFile, PAGE_TYPE_LOCAL_NAME, PageType.class);
  }

  /**
   * Creates a new GeolocatedPageUnmarshaller for given input stream.
   * @param inputStream The XML instance document
   * @throws XMLStreamException When an error occurs on parsing to the first
   * target element
   * @throws JAXBException When the JAXB context or the internal Unmarshaller
   * could not be created
   */
  public GeolocatedPageUnmarshaller(final InputStream inputStream)
  throws XMLStreamException, JAXBException {
    super(inputStream, PAGE_TYPE_LOCAL_NAME, PageType.class);
  }
  
  public static void main(String[] args) throws XMLStreamException, JAXBException, IOException {
    final GeolocatedPageUnmarshaller u = new GeolocatedPageUnmarshaller(new File(args[0]));
    for (final PageType page : u) {
      System.out.println(page);
    }
    u.close();
  }

}
