package de.aitools.aq.wikipedia.reverts;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import de.aitools.aq.xml.PartialMarshaller;

public class RevertMarshaller extends PartialMarshaller<RevertType> {
  
  private static final String REVERTS_TYPE_LOCAL_NAME = "reverts";
  
  private static final String REVERT_TYPE_LOCAL_NAME = "revert";

  public RevertMarshaller(final File outputFile)
  throws NullPointerException, XMLStreamException, JAXBException, IOException {
    super(outputFile, REVERT_TYPE_LOCAL_NAME, RevertType.class);
    this.init();
  }

  public RevertMarshaller(final OutputStream outputStream)
  throws NullPointerException, XMLStreamException, JAXBException {
    super(outputStream, REVERT_TYPE_LOCAL_NAME, RevertType.class);
    this.init();
  }
  
  protected void init() throws XMLStreamException {
    this.startElement(REVERTS_TYPE_LOCAL_NAME);
  }

}
