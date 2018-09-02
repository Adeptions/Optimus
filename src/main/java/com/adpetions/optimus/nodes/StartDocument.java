package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public class StartDocument implements WriterNode {
    private String version;
    private String encoding;

    public StartDocument() {

    }

    public StartDocument(String version) {
        this.version = version;
    }

    public StartDocument(String encoding, String version) {
        this.encoding = encoding;
        this.version = version;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        if (version != null && encoding != null) {
            writer.writeStartDocument(encoding, version);
        } else if (version != null) {
            writer.writeStartDocument(version);
        } else {
            writer.writeStartDocument();
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.DOCUMENT_START;
    }
}
