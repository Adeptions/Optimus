package com.adpetions.optimus.templating;

import com.adpetions.optimus.Transformer;
import com.adpetions.optimus.TransformContext;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import java.util.List;

/**
 * A basic abstract transform template
 * Extend this class to save having to implement setTransformContext() and setWriter() methods on each and every
 * implementation of OptimusTransformTemplate
 */
public abstract class AbstractTransformTemplate implements OptimusTransformTemplate {
    protected TransformContext context;
    protected TransformXMLStreamWriter writer;

    @Override
    public void initialize(Transformer transformer, TransformContext context, TransformXMLStreamWriter writer) throws Exception {
        this.context = context;
        this.writer = writer;
    }

    @Override
    public List<OptimusTransformTemplate> getImports() throws Exception {
        return null;
    }
}
