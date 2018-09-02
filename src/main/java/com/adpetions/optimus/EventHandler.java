package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * Interface to be used by all registered Transformer event handlers
 * @param <T> the type of the cargo object
 */
@FunctionalInterface
public interface EventHandler<T> {
    /**
     * Handle the event from Transformer
     * @param context the context of the transform
     * @param cargo the cargo (informational/state) object
     * @param writer the output writer
     * @return the ContinueState
     */
    ContinueState handle(TransformContext context, T cargo, TransformXMLStreamWriter writer) throws XMLStreamException,TransformException;

}