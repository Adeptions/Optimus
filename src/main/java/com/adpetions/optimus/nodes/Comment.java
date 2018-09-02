package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public class Comment implements WriterNode {
    private String data;

    public Comment(String data) {
        this.data = data;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        writer.writeComment(data);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.COMMENT;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
