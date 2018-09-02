package com.adpetions.optimus.writers;

import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.namespaces.TransformNamespaceContext;
import com.adpetions.optimus.nodes.WriterNode;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.Reader;

/**
 * A wrapper interface around XMLStreamWriter
 * Where methods return self so that they can be chained!
 */
public interface TransformXMLStreamWriter {
    /**
     * Writes a start tag to the output.  All writeStartElement methods
     * open a new scope in the internal namespace context.  Writing the
     * corresponding EndElement causes the scope to be closed.
     *
     * @param localName local name of the tag, may not be null
     * @return this
     * @throws XMLStreamException if the writer cannot start a new element
     */
    TransformXMLStreamWriter writeStartElement(String localName) throws XMLStreamException;

    /**
     * Writes a start tag to the output
     *
     * @param namespaceURI the namespaceURI of the prefix to use, may not be null
     * @param localName    local name of the tag, may not be null
     * @return this
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix
     */
    TransformXMLStreamWriter writeStartElement(String namespaceURI, String localName) throws XMLStreamException;

    /**
     * Writes a start tag to the output
     *
     * @param prefix       the prefix of the tag, may not be null
     * @param localName    local name of the tag, may not be null
     * @param namespaceURI the uri to bind the prefix to, may not be null
     * @return this
     * @throws XMLStreamException if the writer cannot start a new element
     */
    TransformXMLStreamWriter writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException;

    /**
     * Writes a start tag to the output
     *
     * @param qname the qualified name (QName) of the tag
     * @return this
     * @throws XMLStreamException if the writer cannot start a new element
     */
    TransformXMLStreamWriter writeStartElement(QName qname) throws XMLStreamException;

    /**
     * Writes an empty element tag to the output
     *
     * @param namespaceURI the uri to bind the tag to, may not be null
     * @param localName    local name of the tag, may not be null
     * @return this
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix
     */
    TransformXMLStreamWriter writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException;

    /**
     * Writes an empty element tag to the output
     *
     * @param prefix       the prefix of the tag, may not be null
     * @param localName    local name of the tag, may not be null
     * @param namespaceURI the uri to bind the tag to, may not be null
     * @return this
     * @throws XMLStreamException if the writer cannot write an empty element
     */
    TransformXMLStreamWriter writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException;

    /**
     * Writes an empty element tag to the output
     *
     * @param qname the qualified name (QName) of the tag
     * @return this
     * @throws XMLStreamException if the writer cannot write an empty element
     */
    TransformXMLStreamWriter writeEmptyElement(QName qname) throws XMLStreamException;

    /**
     * Writes an empty element tag to the output
     *
     * @param localName local name of the tag, may not be null
     * @return this
     * @throws XMLStreamException if the writer cannot write an empty element
     */
    TransformXMLStreamWriter writeEmptyElement(String localName) throws XMLStreamException;

    /**
     * Writes an end tag to the output relying on the internal
     * state of the writer to determine the prefix and local name
     * of the event.
     *
     * @return this
     * @throws XMLStreamException if the writer cannot write an end element
     */
    TransformXMLStreamWriter writeEndElement() throws XMLStreamException;

    /**
     * Closes any start tags and writes corresponding end tags.
     *
     * @return this
     * @throws XMLStreamException if the writer is unable to end the document
     */
    TransformXMLStreamWriter writeEndDocument() throws XMLStreamException;

    /**
     * Close this writer and free any resources associated with the
     * writer.  This must not close the underlying output stream.
     *
     * @throws XMLStreamException if the writer encounters an error closing
     */
    void close() throws XMLStreamException;

    /**
     * Write any cached data to the underlying output mechanism.
     *
     * @throws XMLStreamException if the writer encounters an error flushing
     */
    void flush() throws XMLStreamException;

    /**
     * Writes an attribute to the output stream without
     * a prefix.
     *
     * @param localName the local name of the attribute
     * @param value     the value of the attribute
     * @return this
     * @throws XMLStreamException if the writer cannot write the attribute
     * @throws IllegalStateException if the current state does not allow Attribute writing
     */
    TransformXMLStreamWriter writeAttribute(String localName, String value) throws XMLStreamException;

    /**
     * Writes an attribute to the output stream
     *
     * @param prefix       the prefix for this attribute
     * @param namespaceURI the uri of the prefix for this attribute
     * @param localName    the local name of the attribute
     * @param value        the value of the attribute
     * @return this
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *                            javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    TransformXMLStreamWriter writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException;

    /**
     * Writes an attribute to the output stream
     *
     * @param namespaceURI the uri of the prefix for this attribute
     * @param localName    the local name of the attribute
     * @param value        the value of the attribute
     * @return this
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *                            javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    TransformXMLStreamWriter writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException;

    /**
     * Writes an attribute to the output stream
     *
     * @param qname the qualified name for this attribute
     * @param value        the value of the attribute
     * @return this
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *                            javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    TransformXMLStreamWriter writeAttribute(QName qname, String value) throws XMLStreamException;

    /**
     * Writes a namespace to the output stream
     * If the prefix argument to this method is the empty string,
     * "xmlns", or null this method will delegate to writeDefaultNamespace
     *
     * @param prefix       the prefix to bind this namespace to
     * @param namespaceURI the uri to bind the prefix to
     * @return this
     * @throws XMLStreamException if the writer cannot write the namespace
     * @throws IllegalStateException if the current state does not allow Namespace writing
     */
    TransformXMLStreamWriter writeNamespace(String prefix, String namespaceURI) throws XMLStreamException;

    /**
     * Writes the default namespace to the stream
     *
     * @param namespaceURI the uri to bind the default namespace to
     * @return this
     * @throws XMLStreamException if the writer cannot write the default namespace
     * @throws IllegalStateException if the current state does not allow Namespace writing
     */
    TransformXMLStreamWriter writeDefaultNamespace(String namespaceURI) throws XMLStreamException;

    /**
     * Writes an xml comment with the data enclosed
     *
     * @param data the data contained in the comment, may be null
     * @return this
     * @throws XMLStreamException if the writer cannot write the comment
     */
    TransformXMLStreamWriter writeComment(String data) throws XMLStreamException;

    /**
     * Writes a processing instruction
     *
     * @param target the target of the processing instruction, may not be null
     * @return this
     * @throws XMLStreamException if the writer cannot write the processing instruction
     */
    TransformXMLStreamWriter writeProcessingInstruction(String target) throws XMLStreamException;

    /**
     * Writes a processing instruction
     *
     * @param target the target of the processing instruction, may not be null
     * @param data   the data contained in the processing instruction, may not be null
     * @return this
     * @throws XMLStreamException if the writer cannot write the processing instruction
     */
    TransformXMLStreamWriter writeProcessingInstruction(String target, String data) throws XMLStreamException;

    /**
     * Writes a CData section
     *
     * @param data the data contained in the CData Section, may not be null
     * @return this
     * @throws XMLStreamException if the writer cannot write the CData section
     */
    TransformXMLStreamWriter writeCData(String data) throws XMLStreamException;

    /**
     * Write a DTD section.  This string represents the entire doctypedecl production
     * from the XML 1.0 specification.
     *
     * @param dtd the DTD to be written
     * @return this
     * @throws XMLStreamException if the writer cannot write the DTD
     */
    TransformXMLStreamWriter writeDTD(String dtd) throws XMLStreamException;

    /**
     * Writes an entity reference
     *
     * @param name the name of the entity
     * @return this
     * @throws XMLStreamException if the writer cannot write the entity
     */
    TransformXMLStreamWriter writeEntityRef(String name) throws XMLStreamException;

    /**
     * Write the XML Declaration. Defaults the XML version to 1.0, and the encoding to utf-8
     *
     * @return this
     * @throws XMLStreamException if the writer cannot start the document
     */
    TransformXMLStreamWriter writeStartDocument() throws XMLStreamException;

    /**
     * Write the XML Declaration. Defaults the XML version to 1.0
     *
     * @param version version of the xml document
     * @return this
     * @throws XMLStreamException if the writer cannot start the document
     */
    TransformXMLStreamWriter writeStartDocument(String version) throws XMLStreamException;

    /**
     * Write the XML Declaration.  Note that the encoding parameter does
     * not set the actual encoding of the underlying output.  That must
     * be set when the instance of the XMLStreamWriter is created using the
     * XMLOutputFactory
     *
     * @param encoding encoding of the xml declaration
     * @param version  version of the xml document
     * @return this
     * @throws XMLStreamException If given encoding does not match encoding
     *                            of the underlying stream
     */
    TransformXMLStreamWriter writeStartDocument(String encoding, String version) throws XMLStreamException;

    /**
     * Write text to the output
     *
     * @param text the value to write
     * @return this
     * @throws XMLStreamException if the writer cannot write the characters
     */
    TransformXMLStreamWriter writeCharacters(String text) throws XMLStreamException;

    /**
     * Write text to the output
     *
     * @param text  the value to write
     * @param start the starting position in the array
     * @param len   the number of characters to write
     * @return this
     * @throws XMLStreamException if the writer cannot write the characters
     */
    TransformXMLStreamWriter writeCharacters(char[] text, int start, int len) throws XMLStreamException;

    /**
     * Gets the prefix the uri is bound to
     *
     * @param uri the uri
     * @return the prefix or null
     * @throws XMLStreamException if the writer encounters an error resolving the prefix for the URI
     */
    String getPrefix(String uri) throws XMLStreamException;

    /**
     * Sets the prefix the uri is bound to.  This prefix is bound
     * in the scope of the current START_ELEMENT / END_ELEMENT pair.
     * If this method is called before a START_ELEMENT has been written
     * the prefix is bound in the root scope.
     *
     * @param prefix the prefix to bind to the uri, may not be null
     * @param uri    the uri to bind to the prefix, may be null
     * @return this
     * @throws XMLStreamException if the writer encounters an error setting the prefix
     */
    TransformXMLStreamWriter setPrefix(String prefix, String uri) throws XMLStreamException;

    /**
     * Binds a URI to the default namespace
     * This URI is bound
     * in the scope of the current START_ELEMENT / END_ELEMENT pair.
     * If this method is called before a START_ELEMENT has been written
     * the uri is bound in the root scope.
     *
     * @param uri the uri to bind to the default namespace, may be null
     * @return this
     * @throws XMLStreamException if the writer encounters an error setting the default namespace
     */
    TransformXMLStreamWriter setDefaultNamespace(String uri) throws XMLStreamException;

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
     * @return this
     * @throws XMLStreamException if the writer encounters an error setting the namespace context
     */
    TransformXMLStreamWriter setNamespaceContext(TransformNamespaceContext context) throws XMLStreamException;

    /**
     * Returns the current namespace context.
     *
     * @return the current NamespaceContext
     */
    TransformNamespaceContext getNamespaceContext();

    /**
     * Get the value of a feature/property from the underlying implementation
     *
     * @param name The name of the property, may not be null
     * @return The value of the property
     * @throws IllegalArgumentException if the property is not supported
     * @throws NullPointerException     if the name is null
     */
    Object getProperty(String name) throws IllegalArgumentException;

    /**
     * Write an xml fragment to the output
     *
     * @param xmlFragment the string xml fragment to be written
     * @return this
     * @throws XMLStreamException if the writer encounters an error writing the fragment
     * @throws TransformException if the writer encounters an error transforming the fragment
     */
    TransformXMLStreamWriter writeFragment(String xmlFragment) throws XMLStreamException, TransformException;

    /**
     * Write an xml fragment to the output
     *
     * @param reader the reader for the fragment to be written
     * @return this
     * @throws XMLStreamException if the writer encounters an error writing from the reader
     * @throws TransformException if the writer encounters an error transforming from the reader
     */
    TransformXMLStreamWriter writeFragment(Reader reader) throws XMLStreamException, TransformException;

    /**
     * Writes the nodes to the output
     *
     * @param nodes the nodes to be written
     * @return this
     * @throws XMLStreamException if the writer encounters an error writing the nodes
     */
    TransformXMLStreamWriter write(WriterNode... nodes) throws XMLStreamException;

}
