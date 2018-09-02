package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class Attribute extends AbstractQNamed implements WriterNode,QNamed {
    private String value;

    public Attribute(String localName, String value) {
        qname = new QName(localName);
        this.value = value;
    }

    public Attribute(String namespaceURI, String localName, String prefix, String value) {
        qname = new QName(namespaceURI, localName, prefix);
        this.value = value;
    }

    public Attribute(String namespaceURI, String localName, String value) {
        qname = new QName(namespaceURI, localName);
        this.value = value;
    }

    public Attribute(QName qname, String value) {
        this.qname = qname;
        this.value = value;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttribute(qname, value);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ATTRIBUTE;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
