package com.adpetions.optimus.writers;

import com.adpetions.optimus.Transformer;
import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.namespaces.TransformNamespaceContext;
import com.adpetions.optimus.nodes.Attribute;
import com.adpetions.optimus.nodes.CData;
import com.adpetions.optimus.nodes.Comment;
import com.adpetions.optimus.nodes.EndElement;
import com.adpetions.optimus.nodes.Namespace;
import com.adpetions.optimus.nodes.NodeCollection;
import com.adpetions.optimus.nodes.ProcessingInstruction;
import com.adpetions.optimus.nodes.StartElement;
import com.adpetions.optimus.nodes.Text;
import com.adpetions.optimus.nodes.WriterNode;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple XML writer - where methods return self so that chaining is possible
 *
 * <p>Special features:-
 *  1. on close (or end document) automatically writes end elements for elements started.
 *  2. attribute buffering - so that the same attribute can be written and re-written without
 *     the resulting xml being malformed.  The last attribute write wins - allows attributes to
 *     be easily overridden by successive callers (event handlers)</p>
 */
public class TransformSimpleWriter implements TransformXMLStreamWriter {
    private Writer writer;
    private boolean stringWriting;
    private XMLStreamWriter xmlWriter;
    private int openXmlElements;
    private TransformNamespaceContext namespaceContext;
    private Map<QName,String> bufferedAttributes = new LinkedHashMap<>();

    // <editor-fold desc="Constructors">
    /**
     * Construct a new instance of {@code TransformSimpleWriter}
     * To be used to write to a string - see getXmlString()
     */
    public TransformSimpleWriter() throws TransformException, XMLStreamException {
        this.namespaceContext = new TransformNamespaceContext();
        this.writer = new StringWriter();
        stringWriting = true;
        openXmlElements = 0;
        createXmlWriter();
    }

    /**
     * Construct a new instance of {@code TransformSimpleWriter} with an initial namespace context
     * To be used to write to a string - see getXmlString()
     * @param namespaceContext the namespace context to use
     */
    public TransformSimpleWriter(TransformNamespaceContext namespaceContext) throws TransformException, XMLStreamException {
        this.namespaceContext = namespaceContext;
        this.writer = new StringWriter();
        stringWriting = true;
        openXmlElements = 0;
        createXmlWriter();
    }

    /**
     * Construct a new instance of {@code TransformSimpleWriter} with a specified writer
     * @param writer the underlying writer to be used
     */
    public TransformSimpleWriter(Writer writer) throws TransformException, XMLStreamException {
        this.namespaceContext = new TransformNamespaceContext();
        this.writer = writer;
        stringWriting = false;
        openXmlElements = 0;
        createXmlWriter();
    }

    /**
     * Construct a new instance of {@code TransformSimpleWriter} with a specified writer and an initial namespace context
     * @param writer the underlying writer to be used
     * @param namespaceContext the namespace context to use
     */
    public TransformSimpleWriter(Writer writer, TransformNamespaceContext namespaceContext) throws TransformException, XMLStreamException {
        this.namespaceContext = namespaceContext;
        this.writer = writer;
        stringWriting = false;
        openXmlElements = 0;
        createXmlWriter();
    }

    /**
     * Construct a new instance of {@code TransformSimpleWriter} with a specified output stream
     * @param outputStream the underlying output stream
     */
    public TransformSimpleWriter(OutputStream outputStream) throws TransformException, XMLStreamException {
        this.namespaceContext = new TransformNamespaceContext();
        stringWriting = false;
        openXmlElements = 0;
        createXmlWriter(outputStream);
    }

    /**
     * Construct a new instance of {@code TransformSimpleWriter} with a specified output stream and
     * an initial namespace context
     * @param outputStream the underlying output stream
     * @param namespaceContext the namespace context to use
     */
    public TransformSimpleWriter(OutputStream outputStream, TransformNamespaceContext namespaceContext) throws TransformException, XMLStreamException {
        this.namespaceContext = namespaceContext;
        stringWriting = false;
        openXmlElements = 0;
        createXmlWriter(outputStream);
    }
    // </editor-fold>

    // <editor-fold desc="Private utility methods">
    private void createXmlWriter() throws TransformException, XMLStreamException {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        xmlWriter = outputFactory.createXMLStreamWriter(this.writer);
    }

    private void createXmlWriter(OutputStream outputStream) throws TransformException, XMLStreamException {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        xmlWriter = outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
    }

    private void writeRootNamespaces() throws XMLStreamException {
        if (openXmlElements == 0) {
            String defaultNsUri = namespaceContext.getDefaultNamespaceURI();
            if (defaultNsUri != null) {
                xmlWriter.writeDefaultNamespace(defaultNsUri);
            }
            Map<String,String> namespaces = namespaceContext.getNamespacePrefixes();
            for (Map.Entry<String,String> nsEntry: namespaces.entrySet()) {
                String nsUri = nsEntry.getValue();
                if (!nsUri.equals(XMLConstants.XML_NS_URI) && !nsUri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                    xmlWriter.writeNamespace(nsEntry.getKey(), nsUri);
                }
            }
        }
    }

    private void addBufferedAttribute(String localName, String value) throws XMLStreamException {
        addBufferedAttribute(new QName(localName), value);
    }

    private void addBufferedAttribute(String prefix, String namespaceURI, String localName, String value) {
        addBufferedAttribute(new QName(namespaceURI, localName, prefix), value);
    }

    private void addBufferedAttribute(String namespaceURI, String localName, String value) {
        addBufferedAttribute(new QName(namespaceURI, localName), value);
    }

    private void addBufferedAttribute(QName qname, String value) {
        bufferedAttributes.put(qname, value);
    }

    private void writeBufferedAttributes() throws XMLStreamException {
        for (Map.Entry<QName,String> entry: bufferedAttributes.entrySet()) {
            QName name = entry.getKey();
            xmlWriter.writeAttribute(name.getPrefix(), name.getNamespaceURI(), name.getLocalPart(), entry.getValue());
        }
        bufferedAttributes.clear();
    }
    // </editor-fold>

    // <editor-fold desc="Public utility methods">

    /**
     * Gets the final XML string for the writer
     * (Only use if the writer is writing to a string - obviously!)
     *
     * @return the written xml string
     */
    public String getXmlString() throws XMLStreamException {
        if (!stringWriting) {
            throw new XMLStreamException("Writer is not writing to string");
        }
        for (int i = 0; i < openXmlElements; i++) {
            xmlWriter.writeEndElement();
        }
        openXmlElements = 0;
        xmlWriter.flush();
        xmlWriter.close();
        return writer.toString();
    }
    // </editor-fold>

    // <editor-fold desc="TransformXMLStreamWriter implementation methods">
    /**
     * Writes a start tag to the output.  All writeStartElement methods
     * open a new scope in the internal namespace context.  Writing the
     * corresponding EndElement causes the scope to be closed.
     *
     * @param localName local name of the tag, may not be null
     */
    @Override
    public TransformXMLStreamWriter writeStartElement(String localName) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeStartElement(localName);
        writeRootNamespaces();
        openXmlElements++;
        return this;
    }

    /**
     * Writes a start tag to the output
     *
     * @param namespaceURI the namespaceURI of the prefix to use, may not be null
     * @param localName    local name of the tag, may not be null
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *                            javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    @Override
    public TransformXMLStreamWriter writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeStartElement(namespaceURI, localName);
        writeRootNamespaces();
        openXmlElements++;
        return this;
    }

    /**
     * Writes a start tag to the output
     *
     * @param prefix       the prefix of the tag, may not be null
     * @param localName    local name of the tag, may not be null
     * @param namespaceURI the uri to bind the prefix to, may not be null
     */
    @Override
    public TransformXMLStreamWriter writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeStartElement(prefix, localName, namespaceURI);
        writeRootNamespaces();
        openXmlElements++;
        return this;
    }

    /**
     * Writes a start tag to the output
     *
     * @param qname the qualified name (QName) of the tag
     */
    @Override
    public TransformXMLStreamWriter writeStartElement(QName qname) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeStartElement(qname.getPrefix(), qname.getLocalPart(), qname.getNamespaceURI());
        writeRootNamespaces();
        openXmlElements++;
        return this;
    }

    /**
     * Writes an empty element tag to the output
     *
     * @param namespaceURI the uri to bind the tag to, may not be null
     * @param localName    local name of the tag, may not be null
     * @throws javax.xml.stream.XMLStreamException if the namespace URI has not been bound to a prefix and
     *                                             javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    @Override
    public TransformXMLStreamWriter writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeEmptyElement(namespaceURI, localName);
        return this;
    }

    /**
     * Writes an empty element tag to the output
     *
     * @param prefix       the prefix of the tag, may not be null
     * @param localName    local name of the tag, may not be null
     * @param namespaceURI the uri to bind the tag to, may not be null
     */
    @Override
    public TransformXMLStreamWriter writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeEmptyElement(prefix, localName, namespaceURI);
        return this;
    }

    /**
     * Writes an empty element tag to the output
     *
     * @param localName local name of the tag, may not be null
     */
    @Override
    public TransformXMLStreamWriter writeEmptyElement(String localName) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeEmptyElement(localName);
        return this;
    }

    /**
     * Writes an empty element tag to the output
     *
     * @param qname the qualified name (QName) of the tag
     */
    @Override
    public TransformXMLStreamWriter writeEmptyElement(QName qname) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeEmptyElement(qname.getPrefix(), qname.getLocalPart(), qname.getNamespaceURI());
        return this;
    }

    /**
     * Writes an end tag to the output relying on the internal
     * state of the writer to determine the prefix and local name
     * of the event.
     */
    @Override
    public TransformXMLStreamWriter writeEndElement() throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeEndElement();
        openXmlElements--;
        return this;
    }

    /**
     * Closes any start tags and writes corresponding end tags.
     */
    @Override
    public TransformXMLStreamWriter writeEndDocument() throws XMLStreamException {
        writeBufferedAttributes();
        for (int i = 0; i < openXmlElements; i++) {
            xmlWriter.writeEndElement();
        }
        xmlWriter.writeEndDocument();
        return this;
    }

    /**
     * Close this writer and free any resources associated with the
     * writer.  This must not close the underlying output stream.
     */
    @Override
    public void close() throws XMLStreamException {
        writeBufferedAttributes();
        for (int i = 0; i < openXmlElements; i++) {
            xmlWriter.writeEndElement();
        }
        openXmlElements = 0;
        xmlWriter.flush();
        xmlWriter.close();
    }

    /**
     * Write any cached data to the underlying output mechanism.
     */
    @Override
    public void flush() throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.flush();
    }

    /**
     * Writes an attribute to the output stream without
     * a prefix.
     *
     * @param localName the local name of the attribute
     * @param value     the value of the attribute
     * @throws IllegalStateException               if the current state does not allow Attribute writing
     */
    @Override
    public TransformXMLStreamWriter writeAttribute(String localName, String value) throws XMLStreamException {
        addBufferedAttribute(localName, value);
        //xmlWriter.writeAttribute(localName, value);
        return this;
    }

    /**
     * Writes an attribute to the output stream
     *
     * @param prefix       the prefix for this attribute
     * @param namespaceURI the uri of the prefix for this attribute
     * @param localName    the local name of the attribute
     * @param value        the value of the attribute
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *                            javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    @Override
    public TransformXMLStreamWriter writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        addBufferedAttribute(prefix, namespaceURI, localName, value);
        //xmlWriter.writeAttribute(prefix, namespaceURI, localName, value);
        return this;
    }

    /**
     * Writes an attribute to the output stream
     *
     * @param namespaceURI the uri of the prefix for this attribute
     * @param localName    the local name of the attribute
     * @param value        the value of the attribute
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *                            javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    @Override
    public TransformXMLStreamWriter writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        addBufferedAttribute(namespaceURI, localName, value);
        //xmlWriter.writeAttribute(namespaceURI, localName, value);
        return this;
    }

    /**
     * Writes an attribute to the output stream
     *
     * @param qname the qualified name for this attribute
     * @param value        the value of the attribute
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *                            javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    @Override
    public TransformXMLStreamWriter writeAttribute(QName qname, String value) throws XMLStreamException {
        addBufferedAttribute(qname, value);
        //xmlWriter.writeAttribute(qName.getPrefix(), qName.getNamespaceURI(), qName.getLocalPart(), value);
        return this;
    }

    /**
     * Writes a namespace to the output stream
     * If the prefix argument to this method is the empty string,
     * "xmlns", or null this method will delegate to writeDefaultNamespace
     *
     * @param prefix       the prefix to bind this namespace to
     * @param namespaceURI the uri to bind the prefix to
     * @throws IllegalStateException if the current state does not allow Namespace writing
     */
    @Override
    public TransformXMLStreamWriter writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        xmlWriter.writeNamespace(prefix, namespaceURI);
        return this;
    }

    /**
     * Writes the default namespace to the stream
     *
     * @param namespaceURI the uri to bind the default namespace to
     * @throws IllegalStateException               if the current state does not allow Namespace writing
     */
    @Override
    public TransformXMLStreamWriter writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        xmlWriter.writeDefaultNamespace(namespaceURI);
        return this;
    }

    /**
     * Writes an xml comment with the data enclosed
     *
     * @param data the data contained in the comment, may be null
     */
    @Override
    public TransformXMLStreamWriter writeComment(String data) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeComment(data);
        return this;
    }

    /**
     * Writes a processing instruction
     *
     * @param target the target of the processing instruction, may not be null
     */
    @Override
    public TransformXMLStreamWriter writeProcessingInstruction(String target) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeProcessingInstruction(target);
        return this;
    }

    /**
     * Writes a processing instruction
     *
     * @param target the target of the processing instruction, may not be null
     * @param data   the data contained in the processing instruction, may not be null
     */
    @Override
    public TransformXMLStreamWriter writeProcessingInstruction(String target, String data) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeProcessingInstruction(target, data);
        return this;
    }

    /**
     * Writes a CData section
     *
     * @param data the data contained in the CData Section, may not be null
     */
    @Override
    public TransformXMLStreamWriter writeCData(String data) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeCData(data);
        return this;
    }

    /**
     * Write a DTD section.  This string represents the entire doctypedecl production
     * from the XML 1.0 specification.
     *
     * @param dtd the DTD to be written
     */
    @Override
    public TransformXMLStreamWriter writeDTD(String dtd) throws XMLStreamException {
        xmlWriter.writeDTD(dtd);
        return this;
    }

    /**
     * Writes an entity reference
     *
     * @param name the name of the entity
     */
    @Override
    public TransformXMLStreamWriter writeEntityRef(String name) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeEntityRef(name);
        return this;
    }

    /**
     * Write the XML Declaration. Defaults the XML version to 1.0, and the encoding to utf-8
     */
    @Override
    public TransformXMLStreamWriter writeStartDocument() throws XMLStreamException {
        xmlWriter.writeStartDocument();
        return this;
    }

    /**
     * Write the XML Declaration. Defaults the XML version to 1.0
     *
     * @param version version of the xml document
     */
    @Override
    public TransformXMLStreamWriter writeStartDocument(String version) throws XMLStreamException {
        xmlWriter.writeStartDocument(version);
        return this;
    }

    /**
     * Write the XML Declaration.  Note that the encoding parameter does
     * not set the actual encoding of the underlying output.  That must
     * be set when the instance of the XMLStreamWriter is created using the
     * XMLOutputFactory
     *
     * @param encoding encoding of the xml declaration
     * @param version  version of the xml document
     * @throws XMLStreamException If given encoding does not match encoding
     *                            of the underlying stream
     */
    @Override
    public TransformXMLStreamWriter writeStartDocument(String encoding, String version) throws XMLStreamException {
        xmlWriter.writeStartDocument(encoding, version);
        return this;
    }

    /**
     * Write text to the output
     *
     * @param text the value to write
     */
    @Override
    public TransformXMLStreamWriter writeCharacters(String text) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeCharacters(text);
        return this;
    }

    /**
     * Write text to the output
     *
     * @param text  the value to write
     * @param start the starting position in the array
     * @param len   the number of characters to write
     */
    @Override
    public TransformXMLStreamWriter writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        writeBufferedAttributes();
        xmlWriter.writeCharacters(text, start, len);
        return this;
    }

    /**
     * Gets the prefix the uri is bound to
     *
     * @param uri the uri
     * @return the prefix or null
     */
    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return xmlWriter.getPrefix(uri);
    }

    /**
     * Sets the prefix the uri is bound to.  This prefix is bound
     * in the scope of the current START_ELEMENT / END_ELEMENT pair.
     * If this method is called before a START_ELEMENT has been written
     * the prefix is bound in the root scope.
     *
     * @param prefix the prefix to bind to the uri, may not be null
     * @param uri    the uri to bind to the prefix, may be null
     */
    @Override
    public TransformXMLStreamWriter setPrefix(String prefix, String uri) throws XMLStreamException {
        xmlWriter.setPrefix(prefix, uri);
        return this;
    }

    /**
     * Binds a URI to the default namespace
     * This URI is bound
     * in the scope of the current START_ELEMENT / END_ELEMENT pair.
     * If this method is called before a START_ELEMENT has been written
     * the uri is bound in the root scope.
     *
     * @param uri the uri to bind to the default namespace, may be null
     */
    @Override
    public TransformXMLStreamWriter setDefaultNamespace(String uri) throws XMLStreamException {
        xmlWriter.setDefaultNamespace(uri);
        return this;
    }

    /**
     * Sets the current namespace context for prefix and uri bindings.
     * This context becomes the root namespace context for writing and
     * will replace the current root namespace context.  Subsequent calls
     * to setPrefix and setDefaultNamespace will bind namespaces using
     * the context passed to the method as the root context for resolving
     * namespaces.  This method may only be called once at the start of
     * the document.  It does not cause the namespaces to be declared.
     * If a namespace URI to prefix mapping is found in the namespace
     * context it is treated as declared and the prefix may be used
     * by the StreamWriter.
     *
     * @param context the namespace context to use for this writer, may not be null
     */
    @Override
    public TransformXMLStreamWriter setNamespaceContext(TransformNamespaceContext context) throws XMLStreamException {
        xmlWriter.setNamespaceContext(context);
        return this;
    }

    /**
     * Returns the current namespace context.
     *
     * @return the current NamespaceContext
     */
    @Override
    public TransformNamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    /**
     * Get the value of a feature/property from the underlying implementation
     *
     * @param name The name of the property, may not be null
     * @return The value of the property
     * @throws IllegalArgumentException if the property is not supported
     * @throws NullPointerException     if the name is null
     */
    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return xmlWriter.getProperty(name);
    }

    /**
     * Write an xml fragment to the output
     *
     * @param xmlFragment the string xml fragment to be written
     */
    @Override
    public TransformXMLStreamWriter writeFragment(String xmlFragment) throws XMLStreamException, TransformException {
        writeBufferedAttributes();
        Transformer transformer = new Transformer(xmlFragment);
        transformer.transform(this);
        return this;
    }

    /**
     * Write an xml fragment to the output
     *
     * @param reader the reader for the fragment to be written
     */
    @Override
    public TransformXMLStreamWriter writeFragment(Reader reader) throws XMLStreamException, TransformException {
        writeBufferedAttributes();
        Transformer transformer = new Transformer(reader);
        transformer.transform(this);
        return this;
    }

    /**
     * Writes the nodes to the output
     *
     * @param nodes the nodes to be written
     */
    @Override
    public TransformXMLStreamWriter write(WriterNode... nodes) throws XMLStreamException {
        for (WriterNode node: nodes) {
            if (node != null) {
                node.write(this);
            }
        }
        return this;
    }

    // </editor-fold>

    public static StartElement start(String localName) {
        return new StartElement(localName);
    }

    public static StartElement start(String namespaceURI, String localName) {
        return new StartElement(namespaceURI, localName);
    }

    public static StartElement start(String namespaceURI, String localName, String prefix) {
        return new StartElement(namespaceURI, localName, prefix);
    }

    public static StartElement start(QName qname) {
        return new StartElement(qname);
    }


    public static Attribute att(String localName, String value) {
        return new Attribute(localName, value);
    }

    public static Attribute att(String namespaceURI, String localName, String prefix, String value) {
        return new Attribute(namespaceURI, localName, prefix, value);
    }

    public static Attribute att(String namespaceURI, String localName, String value) {
        return new Attribute(namespaceURI, localName, value);
    }

    public static Attribute att(QName qname, String value) {
        return new Attribute(qname, value);
    }

    public static Namespace ns(String namespaceURI) {
        return new Namespace(namespaceURI);
    }

    public static Namespace ns(String prefix, String namespaceURI) {
        return new Namespace(prefix, namespaceURI);
    }

    public static EndElement end() {
        return new EndElement();
    }

    public static Text text(String text) {
        return new Text(text);
    }

    public static CData cdata(String cdata) {
        return new CData(cdata);
    }

    public static Comment comment(String data) {
        return new Comment(data);
    }

    public static ProcessingInstruction pi(String target) {
        return new ProcessingInstruction(target);
    }

    public static ProcessingInstruction pi(String target, String data) {
        return new ProcessingInstruction(target, data);
    }

    public static NodeCollection nodes() {
        return new NodeCollection();
    }

    public static NodeCollection nodes(WriterNode... nodes) {
        return new NodeCollection(nodes);
    }
}
