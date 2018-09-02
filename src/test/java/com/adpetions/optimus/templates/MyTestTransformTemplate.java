package com.adpetions.optimus.templates;

import com.adpetions.optimus.ContinueState;
import com.adpetions.optimus.EventType;
import com.adpetions.optimus.Transformer;
import com.adpetions.optimus.TransformContext;
import com.adpetions.optimus.templating.AbstractTransformTemplate;
import com.adpetions.optimus.templating.OptimusTransformTemplate;
import com.adpetions.optimus.templating.annotations.*;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import static com.adpetions.optimus.templating.annotations.TemplateConfig.ConfigOptions.*;

@TemplateConfig(options = {NO_SUPPRESS_WHITESPACE,COALESCING,TRACK_ATTRIBUTES,FORCE_NON_SELF_CLOSING})
@TemplateDefaultNamespaceURI("urn:my-default-namespace")
@TemplateNamespaces({
        @TemplateNamespace(prefix="def",uri="urn:my-default-namespace"),
        @TemplateNamespace(prefix="foo",uri="http://www.foo.com"),
        @TemplateNamespace(prefix="bar",uri="http://www.bar.com"),
        @TemplateNamespace(prefix="html",uri="http://www.w3.org/1999/xhtml")})
public class MyTestTransformTemplate extends AbstractTransformTemplate implements OptimusTransformTemplate {
    private Object cargo;

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
    }

    @EventTemplate(event= EventType.START_ELEMENT, matchPath="*", priority = 15)
    public ContinueState handleAllElements() {
        return null;
    }

    @EventTemplate(event=EventType.START_ELEMENT, matchPath="def:*", priority = 10)
    public ContinueState handleAllDefaultNsElements() {
        return null;
    }

    @EventTemplate(event=EventType.START_ELEMENT, matchPath="foo:*", priority = 5)
    public ContinueState handleFooElements() {
        return null;
    }

    @EventTemplate(event=EventType.START_ELEMENT, matchPath="bar:*", priority = 1)
    public ContinueState handleBarElements() {
        return null;
    }

    @EventTemplate(event=EventType.ATTRIBUTE, matchPath="@*", priority = 10)
    public ContinueState handleAttributes() {
        return null;
    }

    @EventTemplate(event=EventType.ATTRIBUTE, matchPath="@foo:*", priority = 9)
    public ContinueState handleFooAttributes() {
        return null;
    }

    @EventTemplate(event=EventType.ATTRIBUTE, matchPath="/root/@*", priority = 8)
    public ContinueState handleRootAttributes() {
        return null;
    }

    @EventTemplate(event=EventType.ATTRIBUTE, matchPath="foo:*/@*", priority = 7)
    public ContinueState handleAttributesOnFooElements() {
        return null;
    }
}
