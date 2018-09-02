package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.nodes.Attribute;
import com.adpetions.optimus.nodes.AttributeMap;
import com.adpetions.optimus.nodes.CData;
import com.adpetions.optimus.nodes.Comment;
import com.adpetions.optimus.nodes.Element;
import com.adpetions.optimus.nodes.EntityRef;
import com.adpetions.optimus.nodes.Namespace;
import com.adpetions.optimus.nodes.NodeCollection;
import com.adpetions.optimus.nodes.ProcessingInstruction;
import com.adpetions.optimus.nodes.Text;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * The context that is provided to handlers during transformation
 * The context is used to provide access to current state and to the underlying XML reader
 * in order to find out information such as current node name etc.
 * Also provides methods, during transformation, to determine the current path; depth etc.
 */
public class TransformContext {
    Transformer transformer;

    // package level properties - so that Transformer can easily set/access these...
    EventType eventType;
    int index; // applicable only to attributes and namespaces
    Stack<QName> path;
    Stack<Map<QName,String>> pathAttributes;
    Stack<Boolean> wasSkippingStack;
    Stack<Boolean> skippedStack;
    Stack<String> defaultNamespaceStack;
    Stack<Map<QName,Integer>> indexPedicateStack;
    Boolean currentlySkipping;
    QName overrideName;
    String overrideAttributeValue;
    String overrideText;
    String overridePITarget;
    String overridePIData;
    String overrideNamespacePrefix;
    String overrideNamespaceURI;

    boolean cancelBubble = false;

    boolean trackPathAttributes;

    Stack<EventHandlerHolder> callStack;

    // flag to determine when readElement is being processed
    boolean elementHasBeenRead = false;

    // <editor-fold desc="Constructors">
    /**
     * Instantiates a TransformContext object
     *
     * @param transformer the owning Transformer transformer
     */
    TransformContext(Transformer transformer) {
        this.transformer = transformer;
        // initialize the path and add the root to it...
        path = new Stack<>();
        // initialize path attributes tracking...
        trackPathAttributes = transformer.trackAttributes;
        if (trackPathAttributes) {
            // initialize the path attributes...
            pathAttributes = new Stack<>();
            pathAttributes.push(new HashMap<>());
        }
        // initialize the skipping stacks...
        currentlySkipping = false;
        wasSkippingStack = new Stack<>();
        wasSkippingStack.push(false);
        skippedStack = new Stack<>();
        skippedStack.push(false);
        // initialize the default namespace tracking stack...
        defaultNamespaceStack = new Stack<>();
        defaultNamespaceStack.push("");
        // initialize the index predicate stack...
        indexPedicateStack = new Stack<>();
    }
    // </editor-fold>

    // <editor-fold desc="Initialize for event methods">
    void initializeForEventHandler(EventType eventType) throws TransformException, XMLStreamException {
        initializeForEventHandler(eventType, -1, false);
    }

    void initializeForEventHandler(EventType eventType, boolean nested) throws TransformException, XMLStreamException {
        initializeForEventHandler(eventType, -1, nested);
    }

    void initializeForEventHandler(EventType eventType, int index, boolean nested) throws TransformException, XMLStreamException {
        this.eventType = eventType;
        this.index = index;
        overrideName = null;
        overrideAttributeValue = null;
        overrideText = null;
        overridePITarget = null;
        overridePIData = null;
        overrideNamespacePrefix = null;
        overrideNamespaceURI = null;
        String prefix;
        // setup the override names, values etc. for this event type...
        switch (this.eventType) {
            case START_DOCUMENT:
                pushPathDocument();
                break;
            case START_ELEMENT:
                pushPathElement();
                prefix = transformer.xmlReader.getPrefix();
                if (prefix == null) {
                    overrideName = new QName(transformer.xmlReader.getNamespaceURI(), transformer.xmlReader.getLocalName());
                } else {
                    overrideName = new QName(transformer.xmlReader.getNamespaceURI(), transformer.xmlReader.getLocalName(), prefix);
                }
                break;
            case ATTRIBUTE:
                pushPathAttribute();
                prefix = transformer.xmlReader.getAttributePrefix(index);
                if (prefix == null) {
                    overrideName = new QName(transformer.xmlReader.getAttributeNamespace(index), transformer.xmlReader.getAttributeLocalName(index));
                } else {
                    overrideName = new QName(transformer.xmlReader.getAttributeNamespace(index), transformer.xmlReader.getAttributeLocalName(index), prefix);
                }
                overrideAttributeValue = transformer.xmlReader.getAttributeValue(index);
                break;
            case NAMESPACE:
                pushPathNamespace();
                overrideNamespacePrefix = transformer.xmlReader.getNamespacePrefix(index);
                overrideNamespaceURI = transformer.xmlReader.getNamespaceURI(index);
                break;
            case PROCESSING_INSTRUCTION:
                pushPathProcessingInstruction();
                overridePITarget = transformer.xmlReader.getPITarget();
                overridePIData = transformer.xmlReader.getPIData();
                break;
            case COMMENT:
                pushPathComment();
                overrideText = transformer.xmlReader.getText();
                break;
            case CHARACTERS:
            case WHITE_SPACE:
            case CDATA:
                pushPathText();
                overrideText = transformer.xmlReader.getText();
                break;
            case ENTITY_REFERENCE:
                pushPathText();
                if (transformer.entityReferenceResolver != null) {
                    overrideText = transformer.entityReferenceResolver.resolveEntityReference(transformer.xmlReader.getLocalName());
                } else {
                    overrideText = "&" + transformer.xmlReader.getLocalName() + ";";
                }
                break;
        }
    }
    // </editor-fold>

    // <editor-fold desc="Path push/pop methods">
    void pushPathDocument() {
        pushPath(new QName(null, "/"));
    }

    void pushPathElement() throws XMLStreamException {
        String prefix = transformer.xmlReader.getPrefix();
        QName elementQName;
        if (prefix != null) {
            elementQName = new QName(transformer.xmlReader.getNamespaceURI(), transformer.xmlReader.getLocalName(), prefix);
        } else {
            elementQName = new QName(transformer.xmlReader.getNamespaceURI(), transformer.xmlReader.getLocalName());
        }
        Map<QName,String> attMap = new HashMap<>();
        if (trackPathAttributes) {
            QName attName;
            for (int a = 0, amax = transformer.xmlReader.getAttributeCount(); a < amax; a++) {
                prefix = transformer.xmlReader.getAttributePrefix(a);
                if (prefix != null) {
                    attName = new QName(transformer.xmlReader.getAttributeNamespace(a),
                            transformer.xmlReader.getAttributeLocalName(a),
                            prefix);
                } else {
                    attName = new QName(transformer.xmlReader.getAttributeNamespace(a),
                            transformer.xmlReader.getAttributeLocalName(a));
                }
                attMap.put(attName, transformer.xmlReader.getAttributeValue(a));
            }
        }
        pushPath(elementQName, attMap);
        wasSkippingStack.push(currentlySkipping);
        skippedStack.push(false);
        // update default namespace...
        int namespacesCount = transformer.xmlReader.getNamespaceCount();
        String defaultNamespaceURI = null;
        String defaultPrefix;
        for (int i = 0; i < namespacesCount; i++) {
            defaultPrefix = transformer.xmlReader.getNamespacePrefix(i);
            if (defaultPrefix == null) {
                defaultNamespaceURI = transformer.xmlReader.getNamespaceURI(i);
                break;
            }
        }
        String currentDefaultNamespace = defaultNamespaceStack.peek();
        if (defaultNamespaceURI != null) {
            transformer.xmlWriter.setDefaultNamespace(defaultNamespaceURI);
            defaultNamespaceStack.push(defaultNamespaceURI);
        } else {
            defaultNamespaceStack.push(currentDefaultNamespace);
        }
    }

    void pushPathAttribute() {
        String prefix = transformer.xmlReader.getAttributePrefix(index);
        QName attributeQname;
        if (prefix != null) {
            attributeQname = new QName(transformer.xmlReader.getAttributeNamespace(index), "@" + transformer.xmlReader.getAttributeLocalName(index), prefix);
        } else {
            attributeQname = new QName(transformer.xmlReader.getAttributeNamespace(index), "@" + transformer.xmlReader.getAttributeLocalName(index));
        }
        pushPath(attributeQname);
    }

    void pushPathNamespace() {
        String prefix = transformer.xmlReader.getNamespacePrefix(index);
        QName nsQname;
        if (prefix != null) {
            nsQname = new QName(XMLConstants.XML_NS_URI, "@" + prefix, prefix);
        } else {
            nsQname = new QName(XMLConstants.XML_NS_URI, "@");
        }
        pushPath(nsQname);
    }

    void pushPathText() {
        pushPath(new QName("#text()"));
    }

    void pushPathComment() {
        pushPath(new QName("!comment()"));
    }

    void pushPathProcessingInstruction() {
        pushPath(new QName("?" + transformer.xmlReader.getPITarget()));
    }

    private void pushPath(QName qname) {
        pushPath(qname, new HashMap<>());
    }

    private void pushPath(QName qname, Map<QName, String> attributes) {
        path.push(qname);
        if (indexPedicateStack.size() > 0) {
            Map<QName,Integer> indexer = indexPedicateStack.peek();
            Integer index = indexer.get(qname);
            if (index != null) {
                indexer.put(qname, index + 1);
            } else {
                indexer.put(qname, 1);
            }
        }
        indexPedicateStack.push(new HashMap<>());
        if (trackPathAttributes) {
            pathAttributes.push(attributes);
        }
    }

    void popPathDocument(boolean nested) {
        if (!nested) {
            popPath();
        }
    }

    void popPathElement() throws XMLStreamException {
        popPath();
        currentlySkipping = wasSkippingStack.pop();
        skippedStack.pop();
        String currentDefaultNamespace = defaultNamespaceStack.pop();
        transformer.xmlWriter.setDefaultNamespace(currentDefaultNamespace);
    }

    void popPath() {
        path.pop();
        indexPedicateStack.pop();
        if (trackPathAttributes) {
            pathAttributes.pop();
        }
    }
    // </editor-fold>

    // <editor-fold desc="Package skipping methods">
    void skipThisElement() {
        skippedStack.pop();
        skippedStack.push(true);
    }

    Boolean isSkippingThisElement() {
        return skippedStack.peek();
    }
    // </editor-fold>

    // <editor-fold desc="Private call chain methods">
    ContinueState callStack(Stack<EventHandlerHolder> stack) throws XMLStreamException, TransformException {
        ContinueState result = ContinueState.CONTINUE;
        callStack = stack;
        cancelBubble = false;
        while (!cancelBubble  && !transformer.quit && !stack.empty()) {
            EventHandlerHolder nextHolder = callStack.pop();
            ContinueState newContinueState = nextHolder.call(this);
            result = (newContinueState == null || newContinueState == ContinueState.CONTINUE ? result : newContinueState);
            transformer.quit = transformer.quit || result == ContinueState.QUIT;
        }
        return result;
    }
    // </editor-fold>

    // <editor-fold desc="Public call chain methods">
    public void cancelNext() {
        cancelBubble = true;
    }

    public ContinueState callNext() throws XMLStreamException, TransformException {
        ContinueState result = callStack(callStack);
        cancelBubble = true;
        return result;
    }
    // </editor-fold>

    // <editor-fold desc="Public state accessor methods">
    /**
     * Determines whether the context is currently skipping (either by skipping an element and an element and descendants)
     * @return whether the context is currently skipping
     */
    public Boolean isSkipping() {
        return currentlySkipping;
    }

    /**
     * Determines whether the transform is currently applying nodes (having called the apply() method)
     * NB. Useful for taking alternative actions in event handler where the same handler might be fired when originally
     *     reading the xml (using readElement) and when re-processing that read xml using apply() method.
     * @return whether the transform is currently applying
     */
    public boolean isApplying() {
        return transformer.applyingLevel > 0;
    }

    /**
     * Get the current path depth
     * All nodes count as a depth (including attributes and namespaces!)
     * The document root is also counted as a depth - so the root element depth will always be 2!
     * @return the current path depth
     */
    public int getPathDepth() {
        return this.path.size() - 1;
    }

    /**
     * The current path - as a list of QNames
     * @return the current path
     */
    public List<QName> getPath() {
        return this.path;
    }

    /**
     * Returns a QName for the current START_ELEMENT, END_ELEMENT or ATTRIBUTE event
     * @return the QName for the current START_ELEMENT, END_ELEMENT or ATTRIBUTE event
     * @throws IllegalStateException if this is not a START_ELEMENT or END_ELEMENT
     */
    public QName getName() {
        if (eventType == EventType.ATTRIBUTE) {
            return transformer.xmlReader.getAttributeName(index);
        }
        return transformer.xmlReader.getName();
    }

    /**
     * Returns the local name of the current event.
     * For START_ELEMENT or END_ELEMENT returns the local name of the current element.
     * For ENTITY_REFERENCE it returns entity name.
     * For ATTRIBUTE it returns the local name of the current attribute
     * The current event must be START_ELEMENT or END_ELEMENT, ATTRIBUTE or ENTITY_REFERENCE
     * @return the localName
     * @throws IllegalStateException if this not a START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE
     */
    public String getLocalName() {
        if (eventType == EventType.ATTRIBUTE) {
            return transformer.xmlReader.getAttributeLocalName(index);
        }
        return transformer.xmlReader.getLocalName();
    }

    /**
     * Gets the 'index' of the current node
     * The 'index' is the count of same nodes in the same parent - same nodes being those with the same
     * name and namespace (for elements, attributes etc.) or type (for text and comment nodes)
     * @return the index of the current node
     */
    public int getIndex() {
        return getAncestorIndex(0);
    }

    /**
     * Gets the 'index' of the given ancestor node within its parent
     * The 'index' is the count of same nodes in the same parent - same nodes being those with the same
     * name and namespace (for elements, attributes etc.) or type (for text and comment nodes)
     * @param ancestorLevel the level of the ancestor (0 is the current item, 1 is the parent,
     *                      2 is the grandparent etc.
     * @return the index of the node within its parent
     */
    public int getAncestorIndex(int ancestorLevel) {
        int depthLevel = this.path.size() - 1 - ancestorLevel;
        QName lookupName = path.get(depthLevel);
        Map<QName,Integer> parentIndexer = indexPedicateStack.get(depthLevel - 1);
        return parentIndexer.get(lookupName);
    }

    /**
     * Determines whether the current event has a name (is a START_ELEMENT, END_ELEMENT or ATTRIBUTE)
     *
     * @return whether the current event has a name
     */
    public boolean hasName() {
        return (eventType == EventType.ATTRIBUTE) || transformer.xmlReader.hasName();
    }

    /**
     * If the current event is a START_ELEMENT or END_ELEMENT  this method
     * returns the URI of the prefix or the default namespace.
     * If the current event is an ATTRIBUTE this method returns
     * the URI of the current attribute
     * If the current event is a NAMESPACE this method returns
     * the URI of the current namespace
     * Returns null if the event does not have a prefix.
     * @return the URI bound to this elements prefix, the default namespace, or null
     */
    public String getNamespaceURI() {
        switch (eventType) {
            case ATTRIBUTE:
                return transformer.xmlReader.getAttributeNamespace(index);
            case NAMESPACE:
                return transformer.xmlReader.getNamespaceURI(index);
            default:
                return transformer.xmlReader.getNamespaceURI();
        }
    }

    /**
     * Return the uri for the given prefix.
     * The uri returned depends on the current state of the processor.
     *
     * <p><strong>NOTE:</strong>The 'xml' prefix is bound as defined in
     * <a href="http://www.w3.org/TR/REC-xml-names/#ns-using">Namespaces in XML</a>
     * specification to "http://www.w3.org/XML/1998/namespace".
     *
     * <p><strong>NOTE:</strong> The 'xmlns' prefix must be resolved to following namespace
     * <a href="http://www.w3.org/2000/xmlns/">http://www.w3.org/2000/xmlns/</a>
     * @param prefix The prefix to lookup, may not be null
     * @return the uri bound to the given prefix or null if it is not bound
     * @throws IllegalArgumentException if the prefix is null
     */
    public String getNamespaceURI(String prefix) {
        return transformer.xmlReader.getNamespaceURI(prefix);
    }

    /**
     * Returns the uri for the namespace declared at the
     * index.
     *
     * @param index the position of the namespace declaration
     * @return returns the namespace uri
     * @throws IllegalStateException if this is not a START_ELEMENT, END_ELEMENT or NAMESPACE
     */
    public String getNamespaceURI(int index) {
        return transformer.xmlReader.getNamespaceURI(index);
    }

    /**
     * Returns the prefix of the current event or null if the event does not have a prefix
     * @return the prefix or null
     */
    public String getPrefix() {
        switch (eventType) {
            case ATTRIBUTE:
                return transformer.xmlReader.getAttributeName(index).getPrefix();
            case NAMESPACE:
                return transformer.xmlReader.getNamespacePrefix(index);
            default:
                return transformer.xmlReader.getPrefix();
        }
    }

    /**
     * Get the name of an ancestor node
     *
     * @param ancestorLevel the level of the ancestor (0 is the current item, 1 is the parent,
     *                      2 is the grandparent etc.
     * @return the QName of the ancestor at the specified level
     */
    public QName getAncestorName(int ancestorLevel) {
        int depthLevel = this.path.size() - 1 - ancestorLevel;
        return path.get(depthLevel);
    }

    /**
     * Determines if the current path contains a specific ancestor
     *
     * @param ancestorName the name of the ancestor to check
     * @return whether the ancestor name is in the ancestry
     */
    public boolean hasAncestor(QName ancestorName) {
        boolean result = false;
        for (QName ancestor: this.path) {
            if (ancestor.equals(ancestorName)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Returns true if the cursor points to a start tag (otherwise false)
     * @return true if the cursor points to a start tag, false otherwise
     */
    public boolean isStartElement() {
        return transformer.xmlReader.isStartElement();
    }

    /**
     * Returns true if the cursor points to an end tag (otherwise false)
     * @return true if the cursor points to an end tag, false otherwise
     */
    public boolean isEndElement() {
        return transformer.xmlReader.isEndElement();
    }

    /**
     * Returns true if the cursor points to a character data event
     * @return true if the cursor points to character data, false otherwise
     */
    public boolean isCharacters() {
        return transformer.xmlReader.isCharacters();
    }

    /**
     * Returns true if the cursor points to a character data event
     * that consists of all whitespace
     * @return true if the cursor points to all whitespace, false otherwise
     */
    public boolean isWhiteSpace() {
        return transformer.xmlReader.isWhiteSpace();
    }

    /**
     * Returns the normalized attribute value of the ancestor attribute
     * @param ancestorLevel the level of the ancestor (0 is the current item, 1 is the parent,
     *                      2 is the grandparent etc.
     * @param attributeName the name of the attribute
     * @return returns the value of the attribute, returns null if not found
     */
    public String getAncestorAttributeValue(int ancestorLevel, QName attributeName) throws IllegalStateException {
        if (!trackPathAttributes) {
            throw new IllegalStateException("Cannot read ancestor attribute values when ancestor attributes not tracked");
        }
        int depthLevel = this.pathAttributes.size() - 1 - ancestorLevel;
        String result = null;
        if (attributeName.getNamespaceURI() == null) {
            Map<QName,String> attMap = this.pathAttributes.get(depthLevel);
            String localName = attributeName.getLocalPart();
            for (Map.Entry<QName,String> entry: attMap.entrySet()) {
                if (entry.getKey().getLocalPart().equals(localName)) {
                    result = entry.getValue();
                    break;
                }
            }
        } else {
            result = this.pathAttributes.get(depthLevel).get(attributeName);
        }
        return result;
    }

    /**
     * Returns the normalized attribute value of the ancestor attribute
     * @param ancestorLevel the level of the ancestor (0 is the current item, 1 is the parent,
     *                      2 is the grandparent etc.
     * @param attributeLocalName the name of the attribute
     * @return returns the value of the attribute, returns null if not found
     */
    public String getAncestorAttributeValue(int ancestorLevel, String attributeLocalName) throws IllegalStateException {
        return getAncestorAttributeValue(ancestorLevel, new QName("", attributeLocalName));
    }

    /**
     * Returns the normalized attribute value of the ancestor attribute
     * @param ancestorLevel the level of the ancestor (0 is the current item, 1 is the parent,
     *                      2 is the grandparent etc.
     * @param attributeNamespaceURI the namespace of the attribute
     * @param attributeLocalName the name of the attribute
     * @return returns the value of the attribute, returns null if not found
     */
    public String getAncestorAttributeValue(int ancestorLevel, String attributeNamespaceURI, String attributeLocalName) throws IllegalStateException {
        return getAncestorAttributeValue(ancestorLevel, new QName(attributeNamespaceURI, attributeLocalName));
    }

    /**
     * Returns the normalized attribute value of the
     * current attribute
     * @return returns the value of the attribute , returns null if not found
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public String getAttributeValue() {
        if (eventType != EventType.ATTRIBUTE) {
            throw new IllegalStateException("Cannot read attribute value when not processing an attribute");
        }
        return transformer.xmlReader.getAttributeValue(index);
    }

    /**
     * Returns the normalized attribute value of the
     * attribute with the namespace and localName
     * If the namespaceURI is null the namespace
     * is not checked for equality
     * @param namespaceURI the namespace of the attribute
     * @param localName the local name of the attribute, cannot be null
     * @return returns the value of the attribute , returns null if not found
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public String getAttributeValue(String namespaceURI, String localName) {
        return transformer.xmlReader.getAttributeValue(namespaceURI, localName);
    }

    /**
     * Returns the normalized attribute value of the
     * attribute with the localName (bound to no namespace - as most attributes usually are)
     * @param localName localName the local name of the attribute, cannot be null
     * @return returns the value of the attribute , returns null if not found
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public String getAttributeValue(String localName) {
        return getAttributeValue("", localName);
    }

    /**
     * Returns the value of the attribute at the
     * index
     * @param index the position of the attribute
     * @return the attribute value
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public String getAttributeValue(int index) {
        return transformer.xmlReader.getAttributeValue(index);
    }

    /**
     * Returns the count of attributes on this START_ELEMENT,
     * this method is only valid on a START_ELEMENT or ATTRIBUTE.  This
     * count excludes namespace definitions.  Attribute indices are
     * zero-based.
     * @return returns the number of attributes
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public int getAttributeCount() {
        return transformer.xmlReader.getAttributeCount();
    }

    /** Returns the qname of the attribute at the provided index
     *
     * @param index the position of the attribute
     * @return the QName of the attribute
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public QName getAttributeName(int index) {
        return transformer.xmlReader.getAttributeName(index);
    }

    /**
     * Returns the namespace of the attribute at the provided
     * index
     * @param index the position of the attribute
     * @return the namespace URI (can be null)
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public String getAttributeNamespace(int index) {
        return transformer.xmlReader.getAttributeNamespace(index);
    }

    /**
     * Returns the localName of the attribute at the provided
     * index
     * @param index the position of the attribute
     * @return the localName of the attribute
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public String getAttributeLocalName(int index) {
        return transformer.xmlReader.getAttributeLocalName(index);
    }

    /**
     * Returns the prefix of this attribute at the
     * provided index
     * @param index the position of the attribute
     * @return the prefix of the attribute
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public String getAttributePrefix(int index) {
        return transformer.xmlReader.getAttributePrefix(index);
    }

    /**
     * Returns a boolean which indicates if this
     * attribute was created by default
     * @param index the position of the attribute
     * @return true if this is a default attribute
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    public boolean isAttributeSpecified(int index) {
        return transformer.xmlReader.isAttributeSpecified(index);
    }

    /**
     * Returns the count of namespaces declared on this START_ELEMENT or END_ELEMENT,
     * this method is only valid on a START_ELEMENT, END_ELEMENT or NAMESPACE. On
     * an END_ELEMENT the count is of the namespaces that are about to go
     * out of scope.  This is the equivalent of the information reported
     * by SAX callback for an end element event.
     * @return returns the number of namespace declarations on this specific element
     * @throws IllegalStateException if this is not a START_ELEMENT, END_ELEMENT or NAMESPACE
     */
    public int getNamespaceCount() {
        return transformer.xmlReader.getNamespaceCount();
    }

    /**
     * Returns the prefix for the namespace declared at the
     * index.  Returns null if this is the default namespace
     * declaration
     *
     * @param index the position of the namespace declaration
     * @return returns the namespace prefix
     * @throws IllegalStateException if this is not a START_ELEMENT, END_ELEMENT or NAMESPACE
     */
    public String getNamespacePrefix(int index) {
        return transformer.xmlReader.getNamespacePrefix(index);
    }

    /**
     * Returns a read only namespace context for the current
     * position.  The context is transient and only valid until
     * a call to next() changes the state of the reader.
     * @return return a namespace context
     */
    public NamespaceContext getCurrentNamespaceContext() {
        return transformer.xmlReader.getNamespaceContext();
    }

    /**
     * Return true if the current event has text, false otherwise
     * The following events have text:
     * CHARACTERS,DTD ,ENTITY_REFERENCE, COMMENT, SPACE
     *
     * @return whether the current event has text
     */
    public boolean hasText() {
        return transformer.xmlReader.hasText();
    }

    /**
     * Returns the current value of the parse event as a string,
     * this returns the string value of a CHARACTERS event,
     * returns the value of a COMMENT, the replacement value
     * for an ENTITY_REFERENCE, the string value of a CDATA section,
     * the string value for a SPACE event,
     * or the String value of the internal subset of the DTD.
     * If an ENTITY_REFERENCE has been resolved, any character data
     * will be reported as CHARACTERS events.
     * @return the current text or null
     * @throws java.lang.IllegalStateException if this state is not a valid text state.
     */
    public String getText() {
        return transformer.xmlReader.getText();
    }

    /**
     * Return input encoding if known or null if unknown.
     * @return the encoding of this instance or null
     */
    public String getEncoding() {
        return transformer.xmlReader.getEncoding();
    }

    /**
     * Return the current location of the processor.
     * If the Location is unknown the processor should return
     * an implementation of Location that returns -1 for the
     * location and null for the publicId and systemId.
     * The location information is only valid until next() is
     * called.
     *
     * @return the current location of the processor (XML reader)
     */
    public Location getLocation() {
        return transformer.xmlReader.getLocation();
    }

    /**
     * Get the xml version declared on the xml declaration
     * Returns null if none was declared
     * @return the XML version or null
     */
    public String getVersion() {
        return transformer.xmlReader.getVersion();
    }

    /**
     * Get the standalone declaration from the xml declaration
     * @return true if this is standalone, or false otherwise
     */
    public boolean isStandalone() {
        return transformer.xmlReader.isStandalone();
    }

    /**
     * Checks if standalone was set in the document
     * @return true if standalone was set in the document, or false otherwise
     */
    public boolean standaloneSet() {
        return transformer.xmlReader.standaloneSet();
    }

    /**
     * Returns the character encoding declared on the xml declaration
     * Returns null if none was declared
     * @return the encoding declared in the document or null
     */
    public String getCharacterEncodingScheme() {
        return transformer.xmlReader.getCharacterEncodingScheme();
    }

    /**
     * Get the target of a processing instruction
     * @return the target or null
     */
    public String getPITarget() {
        return transformer.xmlReader.getPITarget();
    }

    /**
     * Get the data section of a processing instruction
     * @return the data or null
     */
    public String getPIData() {
        return transformer.xmlReader.getPIData();
    }

    /**
     * Get the current event type being processed
     *
     * @return the current event type being processed
     */
    public EventType getEventType() {
        return eventType;
    }
    // </editor-fold>

    // <editor-fold desc="Public overriding accessors">
    public QName getOverrideName() {
        return overrideName;
    }

    public void setOverrideName(QName overrideName) {
        this.overrideName = overrideName;
    }

    public String getOverrideAttributeValue() {
        return  overrideAttributeValue;
    }

    public void setOverrideAttributeValue(String overrideAttributeValue) {
        this.overrideAttributeValue = overrideAttributeValue;
    }

    public String getOverrideText() {
        return overrideText;
    }

    public void setOverrideText(String overrideText) {
        this.overrideText = overrideText;
    }

    public String getOverridePITarget() {
        return overridePITarget;
    }

    public void setOverridePITarget(String overridePITarget) {
        this.overridePITarget = overridePITarget;
    }

    public String getOverridePIData() {
        return overridePIData;
    }

    public void setOverridePIData(String overridePIData) {
        this.overridePIData = overridePIData;
    }

    public String getOverrideNamespacePrefix() {
        return overrideNamespacePrefix;
    }

    public void setOverrideNamespacePrefix(String overrideNamespacePrefix) {
        this.overrideNamespacePrefix = overrideNamespacePrefix;
    }

    public String getOverrideNamespaceURI() {
        return overrideNamespaceURI;
    }

    public void setOverrideNamespaceURI(String overrideNamespaceURI) {
        this.overrideNamespaceURI = overrideNamespaceURI;
    }

    public TransformXMLStreamWriter switchWriter(TransformXMLStreamWriter writer) {
        TransformXMLStreamWriter result = transformer.xmlWriter;
        transformer.xmlWriter = writer;
        return result;
    }
    // </editor-fold>

    // <editor-fold desc="Public read and apply methods">

    /**
     * Reads the current element and it's child node
     *
     * <p>IMPORTANT NOTES:-
     *   1. Can only be called when current event type is START_ELEMENT and when not applying
     *   2. Reads the entire element (inc. start tag, end tag, attributes, namespaces and all child nodes
     *      of the current element)
     *   3. During reading, all default output of the xml read is suppressed - i.e. it won't appear in
     *      the output!  Use apply() method to re-output read xml
     *   4. During reading, all event firing is suppressed (if events need firing - then use the
     *      apply() method on the returned element (or its childNodes)</p>
     *
     * @return the Element (containing attributes, namespaces and child nodes)
     * @throws java.lang.IllegalStateException if this state is not a valid text state - i.e. not on a start element
     */
    public Element readElement() throws IllegalStateException, XMLStreamException, TransformException {
        if (this.eventType != EventType.START_ELEMENT) {
            throw new IllegalStateException("readElement can only be called at START_ELEMENT");
        } else if (transformer.applyingLevel > 0) {
            throw new IllegalStateException("readElement cannot be called whilst applying");
        }
        elementHasBeenRead = true;
        return readElementAndChildren();
    }

    private Element readElementAndChildren() throws XMLStreamException, TransformException {
        Element result = new Element(transformer.xmlReader.getName());
        AttributeMap attributes = result.getAttributes();
        NodeCollection namespaces = result.getNamespaces();
        // read attributes...
        for (int a = 0, amax = transformer.xmlReader.getAttributeCount(); a < amax; a++) {
            QName attName = transformer.xmlReader.getAttributeName(a);
            attributes.put(attName, new Attribute(attName, transformer.xmlReader.getAttributeValue(a)));
        }
        // read namespaces...
        for (int n = 0, nmax = transformer.xmlReader.getNamespaceCount(); n < nmax; n++) {
            String uri = transformer.xmlReader.getNamespaceURI(n);
            String prefix = transformer.xmlReader.getNamespacePrefix(n);
            if (prefix != null && !prefix.isEmpty()) {
                namespaces.add(new Namespace(prefix, uri));
            } else {
                namespaces.add(new Namespace(uri));
            }
        }
        // read child nodes...
        int nextEvent;
        NodeCollection childNodes = result.getChildNodes();
        while (transformer.xmlReader.hasNext()) {
            nextEvent = transformer.xmlReader.next();
            switch (nextEvent) {
                case XMLStreamConstants.END_ELEMENT:
                case XMLStreamConstants.END_DOCUMENT:
                    return result;
                case XMLStreamReader.START_ELEMENT:
                    childNodes.add(readElementAndChildren());
                    break;
                case XMLStreamConstants.SPACE:
                case XMLStreamConstants.CHARACTERS:
                    childNodes.add(new Text(transformer.xmlReader.getText()));
                    break;
                case XMLStreamConstants.CDATA:
                    childNodes.add(new CData(transformer.xmlReader.getText()));
                    break;
                case XMLStreamConstants.COMMENT:
                    childNodes.add(new Comment(transformer.xmlReader.getText()));
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    childNodes.add(new ProcessingInstruction(transformer.xmlReader.getPITarget(), transformer.xmlReader.getPIData()));
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    String name = transformer.xmlReader.getLocalName();
                    if (transformer.entityReferenceResolver != null) {
                        childNodes.add(new Text(transformer.entityReferenceResolver.resolveEntityReference(name)));
                    } else {
                        childNodes.add(new EntityRef(name));
                    }
                    break;
            }
        }
        return result;
    }

    public void apply(NodeCollection nodes) throws XMLStreamException, TransformException {
        transformer.apply(nodes);
    }
    // </editor-fold>
}
