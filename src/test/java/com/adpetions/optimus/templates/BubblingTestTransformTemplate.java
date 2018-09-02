package com.adpetions.optimus.templates;

import com.adpetions.optimus.ContinueState;
import com.adpetions.optimus.EventType;
import com.adpetions.optimus.TransformContext;
import com.adpetions.optimus.Transformer;
import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.templating.AbstractTransformTemplate;
import com.adpetions.optimus.templating.OptimusTransformTemplate;
import com.adpetions.optimus.templating.annotations.EventTemplate;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class BubblingTestTransformTemplate extends AbstractTransformTemplate implements OptimusTransformTemplate {
    private StringBuilder cargo;
    private List<OptimusTransformTemplate> imports = new ArrayList<>();

    public BubblingTestTransformTemplate() {
        imports.add(new ImportedTemplate());
    }

    public String getResult() {
        return cargo.toString();
    }

    @Override
    public List<OptimusTransformTemplate> getImports() throws Exception {
        return imports;
        //return null;
    }

    /**
     * Initialize the template and any further initialization of Transformer
     *
     * @param transformer the Transformer transformer
     * @param context the transform context (store this to gain access to context during transform)
     * @param writer the output writer (store this to gain access to writer during transform)
     */
    @Override
    public void initialize(Transformer transformer, TransformContext context, TransformXMLStreamWriter writer) throws Exception {
        super.initialize(transformer, context, writer);
        // any further initialization of the template and/or the Transformer transformer
        cargo = new StringBuilder();
    }

    @EventTemplate(event= EventType.START_DOCUMENT, priority = 30)
    public ContinueState startDocumentHandler1() {
        cargo.append("[1]");
        return null;
    }

    @EventTemplate(event= EventType.START_DOCUMENT, priority = 20)
    public ContinueState startDocumentHandler2() {
        cargo.append("[2]");
        context.cancelNext();
        return null;
    }

    @EventTemplate(event= EventType.START_DOCUMENT, priority = 10)
    public ContinueState startDocumentHandler3() throws XMLStreamException, TransformException {
        cargo.append("[3]");
        context.callNext();
        return null;
    }
}
