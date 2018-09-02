package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class Element extends AbstractQNamed implements WriterNode,QNamed {
    private AttributeMap attributes = new AttributeMap();
    private NodeCollection namespaces = new NodeCollection();
    private NodeCollection childNodes = new NodeCollection();

    public Element(String localName) {
        qname = new QName(localName);
    }

    public Element(String namespaceURI, String localName) {
        qname = new QName(namespaceURI, localName);
    }

    public Element(String namespaceURI, String localName, String prefix) {
        qname = new QName(namespaceURI, localName, prefix);
    }

    public Element(QName qname) {
        this.qname = qname;
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(qname);
        attributes.write(writer);
        namespaces.write(writer);
        childNodes.write(writer);
        writer.writeEndElement();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ELEMENT;
    }

    public AttributeMap getAttributes() {
        return attributes;
    }

    public NodeCollection getNamespaces() {
        return namespaces;
    }

    public NodeCollection getChildNodes() {
        return childNodes;
    }

    public Attribute getAttribute(QName attributeName) {
        return attributes.get(attributeName);
    }

    public Attribute getAttribute(String localName) {
        return attributes.get(new QName(localName));
    }

    public Attribute getAttribute(String namespaceURI, String localName) {
        return attributes.get(new QName(namespaceURI, localName));
    }

    /**
     * Gets the text value of the element
     * Only immediate text nodes are included in the resultant text
     *
     * @return the text value of the element or null if the element has no text nodes
     */
    public String getText() {
        StringBuilder textBuilder = new StringBuilder();
        boolean textEncountered = false;
        for (WriterNode node: childNodes) {
            switch (node.getNodeType()) {
                case CDATA:
                    textEncountered = true;
                    textBuilder.append(((CData)node).cdata);
                    break;
                case TEXT:
                    textEncountered = true;
                    textBuilder.append(((Text)node).text);
                    break;
            }
        }
        return (textEncountered ? textBuilder.toString() : null);
    }

    /**
     * Gets the text value of the element including text nodes nested in child nodes
     *
     * @return the text value of the element or null if the element has no text nodes
     */
    public String getAllText() {
        StringBuilder textBuilder = new StringBuilder();
        boolean textEncountered = false;
        for (WriterNode node: childNodes) {
            switch (node.getNodeType()) {
                case CDATA:
                    textEncountered = true;
                    textBuilder.append(((CData)node).cdata);
                    break;
                case TEXT:
                    textEncountered = true;
                    textBuilder.append(((Text)node).text);
                    break;
                case ELEMENT:
                    String subText = ((Element)node).getAllText();
                    if (subText != null) {
                        textEncountered = true;
                        textBuilder.append(subText);
                    }
                    break;
            }
        }
        return (textEncountered ? textBuilder.toString() : null);
    }
}
