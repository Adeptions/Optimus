package com.adpetions.optimus.templates;

import com.adpetions.optimus.ContinueState;
import com.adpetions.optimus.EventType;
import com.adpetions.optimus.templating.AbstractTransformTemplate;
import com.adpetions.optimus.templating.OptimusTransformTemplate;
import com.adpetions.optimus.templating.annotations.EventTemplate;

public class ImportedTemplate extends AbstractTransformTemplate implements OptimusTransformTemplate {
    private StringBuilder allTextBuilder;
    public ImportedTemplate() {
        allTextBuilder = new StringBuilder();
    }

    @EventTemplate(event= EventType.CHARACTERS)
    public ContinueState handleText() throws Exception {
        allTextBuilder.append(context.getText());
        return null;
    }

}
