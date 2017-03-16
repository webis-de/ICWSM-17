package de.aitools.aq.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Implements an {@link Iterator} over certain elements in an XML stream.
 * <p>
 * Based on <a href="http://stackoverflow.com/a/9260039">http://stackoverflow.com/a/9260039</a>.
 * </p>
 * <p>
 * You can use this iterator also as a {@link #stream()} or within a for-each
 * statement. However, each entry is returned exactly once. The iterator can not
 * be reset.
 * </p>
 *
 * @author johannes.kiesel@uni.weimar.de
 *
 */
public class PartialUnmarshaller<T>
implements Iterator<T>, Iterable<T>, AutoCloseable {
  
  private final XMLStreamReader reader;
  private final Unmarshaller unmarshaller;
  private final String typeLocalName;
  private final Class<T> classType;

  /**
   * Creates a new PartialUnmarshaller for given input file.
   * <p>
   * If the filename ends in <tt>.gz</tt>, data will be GZIP-uncompressed
   * in-memory.
   * </p><p>
   * This class can only be used if the local name (XML tag) of the elements
   * that should be unmarshalled is unique for these elements or only occurs
   * again within these elements. This is because the unmarshalling starts when
   * the corresponding start tag is encountered.
   * </p>
   * @param inputFile The XML instance document
   * @param typeLocalName XML local name of the elements to be unmarshalled
   * @param classType Target class for unmarshalling that corresponds to the
   * elements with the given local name
   * @throws XMLStreamException When an error occurs on parsing to the first
   * target element
   * @throws JAXBException When the JAXB context or the internal Unmarshaller
   * could not be created for given class type
   * @throws IOException When the input file could not be opened for reading
   */
  public PartialUnmarshaller(
      final File inputFile,
      final String typeLocalName, final Class<T> classType)
  throws XMLStreamException, JAXBException, IOException {
    this(PartialUnmarshaller.openStream(inputFile), typeLocalName, classType);
  }


  /**
   * Creates a new PartialUnmarshaller for given input stream.
   * <p>
   * This class can only be used if the local name (XML tag) of the elements
   * that should be unmarshalled is unique for these elements or only occurs
   * again within these elements. This is because the unmarshalling starts when
   * the corresponding start tag is encountered.
   * </p>
   * @param inputStream The XML instance document
   * @param typeLocalName XML local name of the elements to be unmarshalled
   * @param classType Target class for unmarshalling that corresponds to the
   * elements with the given local name
   * @throws XMLStreamException When an error occurs on parsing to the first
   * target element
   * @throws JAXBException When the JAXB context or the internal Unmarshaller
   * could not be created for given class type
   */
  public PartialUnmarshaller(
      final InputStream inputStream,
      final String typeLocalName, final Class<T> classType)
  throws XMLStreamException, JAXBException {
    if (typeLocalName == null) { throw new NullPointerException(); }
    if (classType == null) { throw new NullPointerException(); }

    this.unmarshaller =
    		JAXBContext.newInstance(classType).createUnmarshaller();
    this.reader =
    		XMLInputFactory.newInstance().createXMLStreamReader(inputStream);

    this.typeLocalName = typeLocalName;
    this.classType = classType;

    this.skipToNext();
  }

  @Override
  public T next() {
    if (!this.hasNext()) { throw new NoSuchElementException(); }
    try {
      final T value =
          this.unmarshaller.unmarshal(this.reader, this.classType).getValue();
      this.skipToNext();
      return value;
    } catch (final XMLStreamException | JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean hasNext() {
    try {
      return this.reader.hasNext();
    } catch (final XMLStreamException e) {
      throw new IllegalStateException(e);
    }
  }
  
  /**
   * Returns itself.
   * <p>
   * This method is used so that the PartialUnmarshaller can be used in a
   * for-each statement.
   * </p>
   * @return Itself
   */
  @Override
  public Iterator<T> iterator() {
    return this;
  }

  @Override
  public void close() throws XMLStreamException {
    this.reader.close();
  }

  /**
   * Returns a {@link Stream} view of this PartialUnmarshaller.
   * <p>
   * The stream takes the elements from the PartialUnmarshaller, so an element
   * taken from a stream or the PartialUnmarshaller will no longer be available
   * from any stream created using this method or {@link #next()}.
   * </p><p>
   * Closing the stream will also close the PartialUnmarshaller.
   * </p>
   * @return The stream view.
   */
  public Stream<T> stream() {
    final int characteristics = Spliterator.ORDERED
        | Spliterator.NONNULL
        | Spliterator.IMMUTABLE;
    final Spliterator<T> spliterator =
        Spliterators.spliteratorUnknownSize(this, characteristics);
    
    final boolean parallel = false;
    final Stream<T> stream = StreamSupport.stream(spliterator, parallel);
    return stream.onClose(new Runnable() {
      @Override
      public void run() {
        try {
          PartialUnmarshaller.this.close();
        } catch (final XMLStreamException e) {
          throw new IllegalStateException(e);
        }
      }
    });
  }
  
  private void skipToNext() throws XMLStreamException {
    while (this.reader.hasNext()
        && (this.reader.getEventType() != XMLStreamConstants.START_ELEMENT
            || !this.reader.getLocalName().equals(this.typeLocalName))) {
      this.reader.next();
    }
  }

  private static InputStream openStream(final File inputFile)
  throws IOException {
    if (inputFile.getName().endsWith(".gz")) {
      return new GZIPInputStream(new FileInputStream(inputFile));
    } else {
      return new FileInputStream(inputFile);
    }
  }

}
