package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AttributeMap implements WriterNode, Map<QName,Attribute> {
    private Map<QName,Attribute> attributes = new HashMap<>();

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        for (Attribute att: attributes.values()) {
            writer.write(att);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ATTRIBUTE_MAP;
    }

    @Override
    public int size() {
        return attributes.size();
    }

    @Override
    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return attributes.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return attributes.containsValue(value);
    }

    @Override
    public Attribute get(Object key) {
        return attributes.get(key);
    }

    @Override
    public Attribute put(QName key, Attribute value) {
        return attributes.put(key, value);
    }

    @Override
    public Attribute remove(Object key) {
        return attributes.remove(key);
    }

    @Override
    public void putAll(Map<? extends QName, ? extends Attribute> m) {
        attributes.putAll(m);
    }

    @Override
    public void clear() {
        attributes.clear();
    }

    @Override
    public Set<QName> keySet() {
        return attributes.keySet();
    }

    @Override
    public Collection<Attribute> values() {
        return attributes.values();
    }

    @Override
    public Set<Entry<QName, Attribute>> entrySet() {
        return attributes.entrySet();
    }
}
