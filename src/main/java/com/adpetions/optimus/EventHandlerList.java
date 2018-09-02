package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Stack;

/**
 * Provides a wrapper around a list of event handlers
 * Has built in capability to sort those handlers by priority and to convert
 * the list to a stack.
 * The EventHandlerList is also used by the EventHandlerPathMap when matching
 * elements against the current transformation path.
 */
class EventHandlerList extends ArrayList<EventHandlerHolder> {
    private static Comparator<EventHandlerHolder> eventHandlerHolderPriorityComparator = (holder1, holder2) -> Integer.compare(holder2.priority, holder1.priority);

    private boolean sorted = false;

    EventHandlerList() {
        super();
    }

    EventHandlerList(Collection<? extends EventHandlerHolder> c) {
        super(c);
    }

    EventHandlerList ensurePrioritySorted() {
        if (!sorted) {
            sort(eventHandlerHolderPriorityComparator);
            sorted = true;
        }
        return this;
    }

    Stack<EventHandlerHolder> toStack() {
        // get sorted by priority (in case not already sorted)...
        ensurePrioritySorted();
        // create a new stack (so that original list isn't lost!)...
        Stack<EventHandlerHolder> result = new Stack<>();
        // add items to stack in reverse order...
        for (int i = size() - 1; i >= 0; i--) {
            result.push(get(i));
        }
        return result;
    }

    ContinueState callAll(TransformContext context) throws TransformException, XMLStreamException {
        return context.callStack(this.toStack());
    }

    @Override
    public boolean add(EventHandlerHolder eventHandlerHolder) {
        sorted = false;
        return super.add(eventHandlerHolder);
    }

    @Override
    public void add(int index, EventHandlerHolder element) {
        sorted = false;
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends EventHandlerHolder> c) {
        sorted = false;
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends EventHandlerHolder> c) {
        sorted = false;
        return super.addAll(index, c);
    }

    @Override
    public boolean remove(Object o) {
        sorted = false;
        return super.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        sorted = false;
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        sorted = false;
        return super.retainAll(c);
    }

    @Override
    public void sort(Comparator<? super EventHandlerHolder> c) {
        sorted = false;
        super.sort(eventHandlerHolderPriorityComparator);
    }

    @Override
    public EventHandlerHolder set(int index, EventHandlerHolder element) {
        sorted = false;
        return super.set(index, element);
    }

}
