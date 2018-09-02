package com.adpetions.optimus;

/**
 * Enum for the event types fired by Transformer
 */
public enum EventType {
    START_ELEMENT,
    END_ELEMENT,
    PROCESSING_INSTRUCTION,
    CHARACTERS,
    CDATA,
    ENTITY_REFERENCE,
    COMMENT,
    WHITE_SPACE,
    START_DOCUMENT,
    END_DOCUMENT,
    ATTRIBUTE,
    NAMESPACE,
    BEFORE_ATTRIBUTES, // called at START_ELEMENT - after start tag begins but before attributes
    AFTER_ATTRIBUTES, // called at START_ELEMENT - after start tag begins and after attributes
    BEFORE_NAMESPACES, // called at START_ELEMENT - after start tag and attributes but before namespaces
    AFTER_NAMESPACES, // called at START_ELEMENT - after start tag, attributes and namespaces
    AFTER_START_ELEMENT // called at START_ELEMENT - at very end of start tag (i.e. after AFTER_ATTRIBUTES & AFTER_NAMESPACES)
}
