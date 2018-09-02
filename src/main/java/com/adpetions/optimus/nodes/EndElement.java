package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class EndElement extends AbstractQNamed implements WriterNode, QNamed {
    public EndElement() {
    }

    public EndElement(QName qname) {
        this.qname = qname;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        writer.writeEndElement();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ELEMENT_END;
    }
}
