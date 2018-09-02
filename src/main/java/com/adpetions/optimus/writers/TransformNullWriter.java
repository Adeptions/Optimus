package com.adpetions.optimus.writers;

import com.adpetions.optimus.namespaces.TransformNamespaceContext;
import com.adpetions.optimus.nodes.WriterNode;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.Reader;

public class TransformNullWriter implements TransformXMLStreamWriter {
    private TransformNamespaceContext namespaceContext;

    // <editor-fold desc="Constructors">
    /**
     * Construct a new instance of {@code TransformNullWriter}
     *
     */
    public TransformNullWriter() {
        this.namespaceContext = new TransformNamespaceContext();
    }

    /**
     * Construct a new instance of {@code TransformNullWriter} with an initial namespace context
     *
     * @param namespaceContext the namespace context to use
     */
    public TransformNullWriter(TransformNamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    // </editor-fold>

    @Override
    public TransformXMLStreamWriter writeStartElement(String localName) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeStartElement(QName qname) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeEmptyElement(String localName) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeEmptyElement(QName qname) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeEndElement() throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeEndDocument() throws XMLStreamException {
        return this;
    }

    @Override
    public void close() throws XMLStreamException {

    }

    @Override
    public void flush() throws XMLStreamException {

    }

    @Override
    public TransformXMLStreamWriter writeAttribute(String localName, String value) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeAttribute(QName qname, String value) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeComment(String data) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeProcessingInstruction(String target) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeProcessingInstruction(String target, String data) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeCData(String data) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeDTD(String dtd) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeEntityRef(String name) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeStartDocument() throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeStartDocument(String version) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeStartDocument(String encoding, String version) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeCharacters(String text) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        return this;
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return null;
    }

    @Override
    public TransformXMLStreamWriter setPrefix(String prefix, String uri) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter setDefaultNamespace(String uri) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter setNamespaceContext(TransformNamespaceContext context) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformNamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return null;
    }

    @Override
    public TransformXMLStreamWriter writeFragment(String xmlFragment) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter writeFragment(Reader reader) throws XMLStreamException {
        return this;
    }

    @Override
    public TransformXMLStreamWriter write(WriterNode... nodes) throws XMLStreamException {
        return this;
    }

}
