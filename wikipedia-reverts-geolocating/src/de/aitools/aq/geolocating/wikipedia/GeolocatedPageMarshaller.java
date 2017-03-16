package de.aitools.aq.geolocating.wikipedia;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import de.aitools.aq.xml.PartialMarshaller;

public class GeolocatedPageMarshaller
extends PartialMarshaller<PageType> {
  
  private static final String PAGES_TYPE_LOCAL_NAME = "pages";
  
  private static final String PAGE_TYPE_LOCAL_NAME = "page";

  public GeolocatedPageMarshaller(final File outputFile)
  throws NullPointerException, XMLStreamException, JAXBException, IOException {
    super(outputFile, PAGE_TYPE_LOCAL_NAME, PageType.class);
    this.init();
  }

  public GeolocatedPageMarshaller(final OutputStream outputStream)
  throws NullPointerException, XMLStreamException, JAXBException {
    super(outputStream, PAGE_TYPE_LOCAL_NAME, PageType.class);
    this.init();
  }
  
  protected void init() throws XMLStreamException {
    this.startElement(PAGES_TYPE_LOCAL_NAME);
  }

}
