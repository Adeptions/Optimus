package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public class EndDocument implements WriterNode {
    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        writer.writeEndDocument();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.DOCUMENT_END;
    }
}
