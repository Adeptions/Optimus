package com.adpetions.optimus.templating.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TemplateNamespace {
    String prefix();
    String uri();
}