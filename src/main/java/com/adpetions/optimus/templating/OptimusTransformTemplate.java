package com.adpetions.optimus.templating;

import com.adpetions.optimus.TransformContext;
import com.adpetions.optimus.Transformer;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import java.util.List;

public interface OptimusTransformTemplate {
    /**
     * Initialize the template and any further initialization of Transformer
     * @param transformer the Transformer transformer
     * @param context the transform context (store this to gain access to context during transform)
     * @param writer the output writer (store this to gain access to writer during transform)
     */
    void initialize(Transformer transformer, TransformContext context, TransformXMLStreamWriter writer) throws Exception;

    /**
     * Get the list of imported templates (OptimusTransformTemplate) used by this transform template
     * @return the list of imported templates
     */
    List<OptimusTransformTemplate> getImports() throws Exception;

}