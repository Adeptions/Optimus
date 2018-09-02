package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public class ProcessingInstruction implements WriterNode {
    private String target;
    private String data;

    public ProcessingInstruction(String target) {
        this.target = target;
    }

    public ProcessingInstruction(String target, String data) {
        this.target = target;
        this.data = data;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        if (data != null) {
            writer.writeProcessingInstruction(target, data);
        } else {
            writer.writeProcessingInstruction(target);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PROCESSING_INSTRUCTION;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
