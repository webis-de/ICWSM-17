package de.aitools.aq.wikipedia.reverts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import de.aitools.aq.xml.PartialUnmarshaller;

/**
 * @author johannes.kiesel@uni.weimar.de
 */
public class RevertUnmarshaller extends PartialUnmarshaller<RevertType> {
  
  private static final String REVERT_TYPE_LOCAL_NAME = "revert";

  /**
   * Creates a new RevertUnmarshaller for given input file.
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
  public RevertUnmarshaller(final File inputFile)
  throws XMLStreamException, JAXBException, IOException {
    super(inputFile, REVERT_TYPE_LOCAL_NAME, RevertType.class);
  }

  /**
   * Creates a new RevertUnmarshaller for given input stream.
   * @param inputStream The XML instance document
   * @throws XMLStreamException When an error occurs on parsing to the first
   * target element
   * @throws JAXBException When the JAXB context or the internal Unmarshaller
   * could not be created
   */
  public RevertUnmarshaller(final InputStream inputStream)
  throws XMLStreamException, JAXBException {
    super(inputStream, REVERT_TYPE_LOCAL_NAME, RevertType.class);
  }
  
  public static void main(String[] args) throws XMLStreamException, JAXBException, IOException {
    final RevertUnmarshaller u = new RevertUnmarshaller(new File(args[0]));
    for (final RevertType revert : u) {
      System.out.println(revert);
    }
    u.close();
  }

}
