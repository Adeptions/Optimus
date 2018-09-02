package com.adpetions.optimus.namespaces;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TransformNamespaceContext implements ExtendedNamespaceContext {
    /** Lookup of prefixes to URIs */
    protected final Map<String, String> namespacePrefixes = new HashMap<>();
    /** Lookup of URIs to prefixes */
    protected final Map<String, List<String>> namespaceURIs = new HashMap<>();
    /** Default namespace URI */
    protected String defaultNamespaceURI;

    // <editor-fold desc="Constructors">
    /**
     * Construct instance of TransformNamespaceContext
     */
    public TransformNamespaceContext() {
        initializeNamespaces();
    }

    /**
     * Construct instance of TransformNamespaceContext with a single namespace prefix and URI
     *
     * @param namespacePrefix <code>String</code> the namespace prefix
     * @param namespaceURI <code>String</code> the namespace URI
     */
    public TransformNamespaceContext(String namespacePrefix, String namespaceURI) {
        initializeNamespaces();
        addNamespace(namespacePrefix, namespaceURI);
    }

    /**
     * Construct instance of TransformNamespaceContext with a default namespace URI
     *
     * @param defaultNamespaceURI <code>String</code> the namespace prefix
     */
    public TransformNamespaceContext(String defaultNamespaceURI) {
        initializeNamespaces();
        this.defaultNamespaceURI = defaultNamespaceURI;
    }

    /**
     * Construct instance of TransformNamespaceContext with a list (map) of prefixes and namespace URIs
     *
     * @param namespaces <code>Map</code> the list of namespace prefixes and URIs
     */
    public TransformNamespaceContext(Map<String, String> namespaces) {
        initializeNamespaces();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            addNamespace(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Construct instance of TransformNamespaceContext with a default namespace URI and
     * a list (map) of prefixes and namespace URIs
     *
     * @param defaultNamespaceURI the default namespace URI
     * @param namespaces a map of namespaces (prefix and namespace URI)
     */
    public TransformNamespaceContext(String defaultNamespaceURI, Map<String, String> namespaces) {
        initializeNamespaces();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            addNamespace(entry.getKey(), entry.getValue());
        }
    }

    public TransformNamespaceContext(TransformNamespaceContext inherit) {
        this.defaultNamespaceURI = inherit.defaultNamespaceURI;
        for (Map.Entry<String,String> entry: inherit.namespacePrefixes.entrySet()) {
            addNamespace(entry.getKey(), entry.getValue());
        }
    }
    // </editor-fold>

    // <editor-fold desc="ExtendedNamespaceContext implementation methods">
    /* (non-Javadoc)
     * @see com.marrow.optimus.namespaces.ExtendedNamespaceContext#initializeNamespaces()
     */
    @Override
    public void initializeNamespaces() {
        addNamespace(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        addNamespace(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
    }

    /* (non-Javadoc)
     * @see com.marrow.optimus.namespaces.ExtendedNamespaceContext#setDefaultNamespaceURI(java.lang.String)
     */
    @Override
    public void setDefaultNamespaceURI(String defaultNamespaceURI) {
        this.defaultNamespaceURI = defaultNamespaceURI;
    }

    /* (non-Javadoc)
     * @see com.marrow.optimus.namespaces.ExtendedNamespaceContext#getDefaultNamespaceURI()
     */
    @Override
    public String getDefaultNamespaceURI() {
        return defaultNamespaceURI;
    }

    /* (non-Javadoc)
     * @see com.marrow.optimus.namespaces.ExtendedNamespaceContext#getNamespacePrefixes()
     */
    @Override
    public Map<String, String> getNamespacePrefixes() {
        return new HashMap<>(namespacePrefixes);
    }

    /* (non-Javadoc)
     * @see com.marrow.optimus.namespaces.ExtendedNamespaceContext#addNamespace(java.lang.String, java.lang.String)
     */
    @Override
    public void addNamespace(String namespacePrefix, String namespaceURI) {
        namespacePrefixes.put(namespacePrefix, namespaceURI);
        List<String> prefixesForUri;
        if (namespaceURIs.containsKey(namespaceURI)) {
            prefixesForUri = namespaceURIs.get(namespaceURI);
        } else {
            prefixesForUri = new ArrayList<>();
            namespaceURIs.put(namespaceURI, prefixesForUri);
        }
        prefixesForUri.add(namespacePrefix);
    }
    // </editor-fold>

    // <editor-fold desc="NamespaceContext implementation methods">
    /* (non-Javadoc)
     * @see javax.xml.namespace.NamespaceContext#getNamespaceURI(java.lang.String)
     */
    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        String result = "";
        if (namespacePrefixes.containsKey(prefix)) {
            result = namespacePrefixes.get(prefix);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see javax.xml.namespace.NamespaceContext#getPrefix(java.lang.String)
     */
    @Override
    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException();
        }
        String result = null;
        if (namespaceURIs.containsKey(namespaceURI)) {
            result = namespaceURIs.get(namespaceURI).get(0);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see javax.xml.namespace.NamespaceContext#getPrefixes(java.lang.String)
     */
    @Override
    public Iterator getPrefixes(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException();
        }
        Iterator<String> result = null;
        if (namespaceURIs.containsKey(namespaceURI)) {
            result = namespaceURIs.get(namespaceURI).iterator();
        }
        return result;
    }
    // </editor-fold>
}
