package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

public interface WriterNode {
    enum NodeType {
        ELEMENT_START,
        ELEMENT_END,
        DOCUMENT_START,
        DOCUMENT_END,
        ATTRIBUTE,
        NAMESPACE,
        TEXT,
        CDATA,
        COMMENT,
        PROCESSING_INSTRUCTION,
        COLLECTION,
        ELEMENT,
        ATTRIBUTE_MAP,
        ENTITY_REF
    }

    /**
     * Writes the node to the specified writer
     * @param writer the XML stream write to be written to
     */
    void write(TransformXMLStreamWriter writer) throws XMLStreamException;

    NodeType getNodeType();
}