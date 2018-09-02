package com.adpetions.optimus.entities;

import com.adpetions.optimus.exceptions.TransformException;

/**
 * Interface for resolving entity references
 */
public interface EntityReferenceResolver {
    /**
     * Resolve a named entity to its string value
     *
     * @param entityReference the entity reference name
     * @return the string represented by the entity reference name
     */
    String resolveEntityReference(String entityReference) throws TransformException;
}