package com.adpetions.optimus.templating.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TemplateConfig {
    enum ConfigOptions {
        SUPPRESS_WHITESPACE,
        NO_SUPPRESS_WHITESPACE,
        TRACK_ATTRIBUTES,
        NO_TRACK_ATTRIBUTES,
        COALESCING,
        NO_COALESCING,
        FORCE_NON_SELF_CLOSING,
        NO_FORCE_NON_SELF_CLOSING,
        OMIT_XML_DECLARATION,
        NO_OMIT_XML_DECLARATION,
        PATH_MAP_CACHING_ON,
        PATH_MAP_CACHING_OFF
    }

    ConfigOptions[] options() default {};
    String[] allowSelfClosing() default {};
}
