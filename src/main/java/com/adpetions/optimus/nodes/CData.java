package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public class CData implements WriterNode {
    String cdata;

    public CData(String cdata) {
        this.cdata = cdata;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        writer.writeCData(cdata);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CDATA;
    }

    public String getCData() {
        return cdata;
    }

    public void setCData(String cdata) {
        this.cdata = cdata;
    }
}
