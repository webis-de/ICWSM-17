package de.aitools.aq.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class PartialMarshaller<T> implements AutoCloseable, Consumer<T> {
  
  private final OutputStream stream;
  
  private final XMLStreamWriter writer;
  
  private final Marshaller marshaller;
  
  private final QName qname;
  
  private final Class<T> classType;
  
  public PartialMarshaller(
      final File outputFile,
      final String typeLocalName, final Class<T> classType)
  throws NullPointerException, XMLStreamException, JAXBException, IOException {
    this(PartialMarshaller.openStream(outputFile), typeLocalName, classType);
  }
  
  public PartialMarshaller(
      final OutputStream outputStream,
      final String typeLocalName, final Class<T> classType)
  throws NullPointerException, XMLStreamException, JAXBException {
    if (typeLocalName == null) { throw new NullPointerException(); }
    this.stream = outputStream;
    this.writer =
        XMLOutputFactory.newFactory().createXMLStreamWriter(this.stream);
    
    final JAXBContext context = JAXBContext.newInstance(classType);
    this.marshaller = context.createMarshaller();
    this.marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
    
    this.writer.writeStartDocument();
    this.writer.writeCharacters("\n");
    
    this.qname = new QName(typeLocalName);
    this.classType = classType;
  }
  
  public XMLStreamWriter getWriter() {
    return this.writer;
  }
  
  protected Marshaller getMarshaller() {
    return this.marshaller;
  }
  
  protected Class<T> getClassType() {
    return this.classType;
  }
  
  public void startElement(final String localName) throws XMLStreamException {
    this.writer.writeStartElement(localName);
  }
  
  public void endElement() throws XMLStreamException {
    this.writer.writeEndElement();
  }

  @Override
  public void accept(final T element)
  throws IllegalStateException {
    try {
      final JAXBElement<T> jaxbElement = 
          new JAXBElement<T>(this.qname, this.classType, null, element);
      
      this.writer.writeCharacters("\n  ");
      this.marshaller.marshal(jaxbElement, this.writer);
    } catch (final JAXBException | XMLStreamException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() throws Exception {
    this.writer.writeCharacters("\n");
    this.writer.writeEndDocument();
    this.writer.close();
    this.stream.close();
  }

  private static OutputStream openStream(final File outputFile)
  throws IOException {
    if (outputFile.getName().endsWith(".gz")) {
      return new GZIPOutputStream(new FileOutputStream(outputFile));
    } else {
      return new FileOutputStream(outputFile);
    }
  }

}
