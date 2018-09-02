package com.adpetions.optimus.templating.annotations;

import com.adpetions.optimus.EventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventTemplate {
    String matchPath() default "*";
    EventType event() default EventType.START_ELEMENT;
    int priority() default 0;
}