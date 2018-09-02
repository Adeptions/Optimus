package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public class Namespace implements WriterNode {
    private String namespaceURI;
    private String prefix;

    public Namespace(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    public Namespace(String prefix, String namespaceURI) {
        this.prefix = prefix;
        this.namespaceURI = namespaceURI;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        if (prefix != null) {
            writer.writeNamespace(prefix, namespaceURI);
        } else {
            writer.writeDefaultNamespace(namespaceURI);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.NAMESPACE;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
