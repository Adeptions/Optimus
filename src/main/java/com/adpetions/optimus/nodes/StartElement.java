package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class StartElement extends AbstractQNamed implements WriterNode,QNamed {
    public StartElement(String localName) {
        qname = new QName(localName);
    }

    public StartElement(String namespaceURI, String localName) {
        qname = new QName(namespaceURI, localName);
    }

    public StartElement(String namespaceURI, String localName, String prefix) {
        qname = new QName(namespaceURI, localName, prefix);
    }

    public StartElement(QName qname) {
        this.qname = qname;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(qname);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ELEMENT_START;
    }
}
