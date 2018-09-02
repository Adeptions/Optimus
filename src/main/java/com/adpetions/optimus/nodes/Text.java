package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public class Text implements WriterNode {
    String text;

    public Text(String text) {
        this.text = text;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        if (text == null) {
            writer.writeCharacters("");
        } else {
            writer.writeCharacters(text);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.TEXT;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
