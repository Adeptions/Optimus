package com.adpetions.optimus.namespaces;

import javax.xml.namespace.NamespaceContext;
import java.util.Map;

public interface ExtendedNamespaceContext extends NamespaceContext {

    /**
     * Inititializes the internal namespaces
     */
    void initializeNamespaces();

    /**
     * Set the default namespace URI (the namespace URI to be used in parts of path expressions
     * that do not have a prefix)
     *
     * @param defaultNamespaceURI the default namespace URI to be set
     */
    void setDefaultNamespaceURI(String defaultNamespaceURI);

    /**
     * Get the default namespace URI (the namespace URI to be used in parts of path expressions
     * that do not have a prefix)
     *
     * @return the default namespace URI
     */
    String getDefaultNamespaceURI();

    /**
     * Get a map of the prefixes to URIs
     *
     * @return the map of prefixes to URIs
     */
    Map<String, String> getNamespacePrefixes();

    /**
     * Adds a new namespace prefix (with bound namespace URI)
     *
     * @param namespacePrefix <code>String</code> the prefix to be added
     * @param namespaceURI <code>String</code> the URI to bind the prefix to
     */
    void addNamespace(String namespacePrefix, String namespaceURI);
}