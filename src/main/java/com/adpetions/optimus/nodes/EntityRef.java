package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public class EntityRef implements WriterNode {
    private String name;

    public EntityRef(String name) {
        this.name = name;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        writer.writeEntityRef(name);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ENTITY_REF;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
