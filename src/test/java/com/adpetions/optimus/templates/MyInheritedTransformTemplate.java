package com.adpetions.optimus.templates;

import com.adpetions.optimus.ContinueState;
import com.adpetions.optimus.EventType;
import com.adpetions.optimus.templating.annotations.EventTemplate;
import com.adpetions.optimus.templating.annotations.TemplateConfig;
import com.adpetions.optimus.templating.annotations.TemplateNamespace;

import static com.adpetions.optimus.templating.annotations.TemplateConfig.ConfigOptions.*;

@TemplateConfig(options = {SUPPRESS_WHITESPACE}, allowSelfClosing = {"html:img", "html:br"})
@TemplateNamespace(prefix="html",uri="http://www.w3.org/1999/xhtml")
public class MyInheritedTransformTemplate extends MyTestTransformTemplate {

    @EventTemplate(event= EventType.START_ELEMENT, matchPath="html:*", priority = 10)
    public ContinueState handleHtmlElements() {
        return null;
    }
}
