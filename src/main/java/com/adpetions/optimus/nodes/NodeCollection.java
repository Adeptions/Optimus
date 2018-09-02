package com.adpetions.optimus.nodes;

import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class NodeCollection implements WriterNode, List<WriterNode> {
    private List<WriterNode> nodes = new ArrayList<>();

    public NodeCollection(WriterNode... nodes) {
        this.nodes.addAll(Arrays.asList(nodes));
    }

    @Override
    public void write(TransformXMLStreamWriter writer) throws XMLStreamException {
        for (WriterNode node: nodes) {
            writer.write(node);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.COLLECTION;
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return nodes.contains(o);
    }

    @Override
    public Iterator<WriterNode> iterator() {
        return nodes.iterator();
    }

    @Override
    public Object[] toArray() {
        return nodes.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return nodes.toArray(a);
    }

    @Override
    public boolean add(WriterNode writerNode) {
        return nodes.add(writerNode);
    }

    @Override
    public void add(int index, WriterNode element) {
        nodes.add(index, element);
    }

    @Override
    public boolean remove(Object o) {
        return nodes.remove(o);
    }

    @Override
    public WriterNode remove(int index) {
        return nodes.remove(index);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return nodes.containsAll(c);
    }

    public void addAll(WriterNode... nodes) {
        this.nodes.addAll(Arrays.asList(nodes));
    }

    @Override
    public boolean addAll(Collection<? extends WriterNode> c) {
        return nodes.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends WriterNode> c) {
        return nodes.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return nodes.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return nodes.retainAll(c);
    }

    @Override
    public void clear() {
        nodes.clear();
    }

    @Override
    public WriterNode get(int index) {
        return nodes.get(index);
    }

    @Override
    public WriterNode set(int index, WriterNode element) {
        return nodes.set(index, element);
    }

    @Override
    public int indexOf(Object o) {
        return nodes.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return nodes.lastIndexOf(o);
    }

    @Override
    public ListIterator<WriterNode> listIterator() {
        return nodes.listIterator();
    }

    @Override
    public ListIterator<WriterNode> listIterator(int index) {
        return nodes.listIterator(index);
    }

    @Override
    public List<WriterNode> subList(int fromIndex, int toIndex) {
        return nodes.subList(fromIndex, toIndex);
    }
}
