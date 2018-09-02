package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.namespaces.TransformNamespaceContext;
import com.adpetions.optimus.nodes.Attribute;
import com.adpetions.optimus.nodes.AttributeMap;
import com.adpetions.optimus.nodes.CData;
import com.adpetions.optimus.nodes.Comment;
import com.adpetions.optimus.nodes.Element;
import com.adpetions.optimus.nodes.EndElement;
import com.adpetions.optimus.nodes.EntityRef;
import com.adpetions.optimus.nodes.Namespace;
import com.adpetions.optimus.nodes.NodeCollection;
import com.adpetions.optimus.nodes.ProcessingInstruction;
import com.adpetions.optimus.nodes.QNamed;
import com.adpetions.optimus.nodes.StartElement;
import com.adpetions.optimus.nodes.Text;
import com.adpetions.optimus.nodes.WriterNode;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * XMLStreamReader implementation to read a collection of nodes
 * (a collection typically obtained from TransformContext.readElement method)
 */
public class ApplyReader implements XMLStreamReader {
    private NodeCollection nodes;
    private int position;
    private int currentEvent;
    private WriterNode currentNode;
    private Map<QName,Attribute> startElementAttributesMap;
    private Map<String,Attribute> startElementLocalAttributesMap;
    private List<Attribute> startElementAttributes;
    private List<Namespace> startElementNamespaces;
    private TransformNamespaceContext currentNamespaceContext;
    private TransformNamespaceContext currentEndElementNamespaceContext;
    private Stack<TransformNamespaceContext> namespaceContextStack;
    // used to track correctness during adding of collection...
    private int depth;
    private boolean inElementStartTag;
    private Stack<QName> elementNameStack;

    ApplyReader(NodeCollection readNodes) throws XMLStreamException, TransformException {
        currentNamespaceContext = new TransformNamespaceContext();
        namespaceContextStack = new Stack<>();
        namespaceContextStack.push(currentNamespaceContext);
        // flatten out the collection into straight start and end tags (as it may contain collections and whole elements)
        // also check that the collection is 'well-formed' (i.e. balanced - no ends without starts etc.)
        // nb. 'well-formed' doesn't mean it must have a single root element
        this.nodes = new NodeCollection();
        depth = 0;
        inElementStartTag = false;
        elementNameStack = new Stack<>();
        addNodes(readNodes);
        if (depth != 0) {
            throw new XMLStreamException("ApplyReader on non-well-formed node collection");
        }
        // set the starting position (i.e. before the very first item)...
        position = -1;
        currentEvent = -1; // nothing!

    }

    private void addNodes(NodeCollection readNodes) throws XMLStreamException, TransformException {
        for (WriterNode node: readNodes) {
            switch (node.getNodeType()) {
                case ELEMENT_START:
                    depth++;
                    inElementStartTag = true;
                    elementNameStack.push(((StartElement)node).getName());
                    nodes.add(node);
                    break;
                case ELEMENT_END:
                    if (elementNameStack.size() == 0) {
                        throw new XMLStreamException("End tag encountered without corresponding start tag in ApplyReader");
                    }
                    depth--;
                    inElementStartTag = false;
                    QName startName = elementNameStack.pop();
                    // store name in end tag and add the end tag...
                    nodes.add(new EndElement(startName));
                    break;
                case DOCUMENT_START:
                case DOCUMENT_END:
                    // document start and end are ignored!
                    break;
                case ATTRIBUTE:
                    if (!inElementStartTag) {
                        throw new XMLStreamException("Attribute encountered outside element start tag");
                    }
                    nodes.add(node);
                    break;
                case NAMESPACE:
                    if (!inElementStartTag) {
                        throw new XMLStreamException("Namespace encountered outside element start tag");
                    }
                    nodes.add(node);
                    break;
                case TEXT:
                case CDATA:
                case COMMENT:
                case PROCESSING_INSTRUCTION:
                case ENTITY_REF:
                    inElementStartTag = false;
                    nodes.add(node);
                    break;
                case COLLECTION:
                    addNodes((NodeCollection)node);
                    break;
                case ELEMENT:
                    // expand out element to start and end tag - with atts, namespaces and child nodes added inside...
                    Element element = (Element)node;
                    nodes.add(new StartElement(element.getName()));
                    depth++;
                    inElementStartTag = true;
                    nodes.addAll(element.getAttributes().values());
                    addNodes(element.getNamespaces());
                    addNodes(element.getChildNodes());
                    nodes.add(new EndElement(element.getName()));
                    depth--;
                    break;
                case ATTRIBUTE_MAP:
                    if (!inElementStartTag) {
                        throw new XMLStreamException("Attribute map encountered outside element start tag");
                    }
                    nodes.addAll(((AttributeMap) node).values());
                    break;
                default:
                    throw new TransformException("Unexpected node type (" + node.getNodeType() + ") adding nodes");
            }
        }
    }

    /**
     * Returns true if there are more parsing events and false
     * if there are no more events.  This method will return
     * false if the current state of the XMLStreamReader is
     * END_DOCUMENT
     *
     * @return true if there are more events, false otherwise
     * @throws XMLStreamException if there is a fatal error detecting the next state
     */
    @Override
    public boolean hasNext() throws XMLStreamException {
        return (position + 1) < nodes.size();
    }

    /**
     * Get next parsing event - a processor may return all contiguous
     * character data in a single chunk, or it may split it into several chunks.
     * If the property javax.xml.stream.isCoalescing is set to true
     * element content must be coalesced and only one CHARACTERS event
     * must be returned for contiguous element content or
     * CDATA Sections.
     *
     * <p>By default entity references must be
     * expanded and reported transparently to the application.
     * An exception will be thrown if an entity reference cannot be expanded.
     * If element content is empty (i.e. content is "") then no CHARACTERS event will be reported.
     * </p>
     *
     * <p>Given the following XML:<br>
     * &lt;foo&gt;&lt;!--description--&gt;content text&lt;![CDATA[&lt;greeting&gt;Hello&lt;/greeting&gt;]]&gt;other content&lt;/foo&gt;&lt;br&gt;
     * The behavior of calling next() when being on foo will be:<br>
     * 1- the comment (COMMENT)<br>
     * 2- then the characters section (CHARACTERS)<br>
     * 3- then the CDATA section (another CHARACTERS)<br>
     * 4- then the next characters section (another CHARACTERS)<br>
     * 5- then the END_ELEMENT<br>
     * </p>
     *
     * <p><b>NOTE:</b> empty element (such as &lt;tag/&gt;) will be reported
     * with  two separate events: START_ELEMENT, END_ELEMENT - This preserves
     * parsing equivalency of empty element to &lt;tag&gt;&lt;/tag&gt;.
     * </p>
     *
     * <p>This method will throw an IllegalStateException if it is called after hasNext() returns false.</p>
     *
     * @return the integer code corresponding to the current parse event
     * @throws NoSuchElementException if this is called when hasNext() returns false
     * @throws XMLStreamException     if there is an error processing the underlying XML source
     * @see XMLEvent
     */
    @Override
    public int next() throws XMLStreamException {
        if (!hasNext()) {
            throw new NoSuchElementException("ApplyReader has no next event");
        }
        position++;
        // work out the current event type...
        currentNode = nodes.get(position);
        switch (currentNode.getNodeType()) {
            case ELEMENT_START:
                currentEvent = START_ELEMENT;
                // skip past attributes and namespaces...
                startElementAttributesMap = new HashMap<>();
                startElementLocalAttributesMap = new HashMap<>();
                startElementAttributes = new ArrayList<>();
                startElementNamespaces = new ArrayList<>();
                namespaceContextStack.push(currentNamespaceContext);
                currentNamespaceContext = new TransformNamespaceContext(currentNamespaceContext);
                currentEndElementNamespaceContext = currentNamespaceContext;
                int aheadCount = 0;
                boolean aheadStop = false;
                for (int ahead = position + 1, amax = nodes.size(); !aheadStop && ahead < amax; ahead++) {
                    WriterNode aheadNode = nodes.get(ahead);
                    switch (aheadNode.getNodeType()) {
                        case ATTRIBUTE:
                            aheadCount++;
                            startElementAttributes.add((Attribute)aheadNode);
                            startElementAttributesMap.put(((Attribute)aheadNode).getName(), (Attribute)aheadNode);
                            startElementLocalAttributesMap.put(((Attribute)aheadNode).getLocalName(), (Attribute)aheadNode);
                            break;
                        case NAMESPACE:
                            Namespace namespace = (Namespace)aheadNode;
                            startElementNamespaces.add(namespace);
                            // update namespace context...
                            if (namespace.getPrefix() != null && !namespace.getPrefix().isEmpty()) {
                                currentNamespaceContext.setDefaultNamespaceURI(namespace.getNamespaceURI());
                            } else {
                                currentNamespaceContext.addNamespace(namespace.getPrefix(), namespace.getNamespaceURI());
                            }
                            aheadCount++;
                            break;
                        default:
                            aheadStop = true;
                    }
                }
                position += aheadCount;
                break;
            case ELEMENT_END:
                currentEvent = END_ELEMENT;
                currentNamespaceContext = namespaceContextStack.pop();
                break;
            case TEXT:
                currentEvent = CHARACTERS;
                break;
            case CDATA:
                currentEvent = CDATA;
                break;
            case COMMENT:
                currentEvent = COMMENT;
                break;
            case PROCESSING_INSTRUCTION:
                currentEvent = PROCESSING_INSTRUCTION;
                break;
            case ENTITY_REF:
                currentEvent = ENTITY_REFERENCE;
                break;
            // following should never be encountered!
            //case ATTRIBUTE:
            //case NAMESPACE:
            //case DOCUMENT_START:
            //case DOCUMENT_END:
            //case COLLECTION:
            //case ELEMENT:
            //case ATTRIBUTE_MAP:
            default:
                throw new XMLStreamException("Unexpected node encountered in ApplyReader");
        }
        return currentEvent;
    }

    /**
     * Returns an integer code that indicates the type
     * of the event the cursor is pointing to.
     */
    @Override
    public int getEventType() {
        return currentEvent;
    }

    /**
     * Returns true if the cursor points to a start tag (otherwise false)
     *
     * @return true if the cursor points to a start tag, false otherwise
     */
    @Override
    public boolean isStartElement() {
        return (currentEvent == START_ELEMENT);
    }

    /**
     * Returns true if the cursor points to an end tag (otherwise false)
     *
     * @return true if the cursor points to an end tag, false otherwise
     */
    @Override
    public boolean isEndElement() {
        return (currentEvent == END_ELEMENT);
    }

    /**
     * Returns true if the cursor points to a character data event
     *
     * @return true if the cursor points to character data, false otherwise
     */
    @Override
    public boolean isCharacters() {
        switch (currentEvent) {
            case CHARACTERS:
            case CDATA:
            case COMMENT:
            case ENTITY_REFERENCE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true if the cursor points to a character data event
     * that consists of all whitespace
     *
     * @return true if the cursor points to all whitespace, false otherwise
     */
    @Override
    public boolean isWhiteSpace() {
        switch (currentEvent) {
            case CHARACTERS:
                return isTextWhitespace(((Text)currentNode).getText());
            case CDATA:
                return isTextWhitespace(((CData)currentNode).getCData());
            case COMMENT:
                return isTextWhitespace(((Comment)currentNode).getData());
            default:
                return false;
        }
    }

    private boolean isTextWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        for (int chpos = 0, maxpos = text.length(); chpos < maxpos;) {
            int ch = text.codePointAt(chpos);
            chpos += Character.charCount(ch);
            if (!Character.isWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the normalized attribute value of the
     * attribute with the namespace and localName
     * If the namespaceURI is null the namespace
     * is not checked for equality
     *
     * @param namespaceURI the namespace of the attribute
     * @param localName    the local name of the attribute, cannot be null
     * @return returns the value of the attribute , returns null if not found
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read attribute when not in start element");
        }
        String result = null;
        if (namespaceURI == null) {
            Attribute attribute = startElementLocalAttributesMap.get(localName);
            if (attribute != null) {
                result = attribute.getValue();
            }
        } else {
            QName attName = new QName(namespaceURI, localName);
            Attribute attribute = startElementAttributesMap.get(attName);
            if (attribute != null) {
                result = attribute.getValue();
            }
        }
        return result;
    }

    /**
     * Returns the value of the attribute at the
     * index
     *
     * @param index the position of the attribute
     * @return the attribute value
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    @Override
    public String getAttributeValue(int index) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read attribute when not in start element");
        }
        return startElementAttributes.get(index).getValue();
    }

    /**
     * Returns the count of attributes on this START_ELEMENT,
     * this method is only valid on a START_ELEMENT or ATTRIBUTE.  This
     * count excludes namespace definitions.  Attribute indices are
     * zero-based.
     *
     * @return returns the number of attributes
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    @Override
    public int getAttributeCount() {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read attribute count when not in start element");
        }
        return startElementAttributes.size();
    }

    /**
     * Returns the qname of the attribute at the provided index
     *
     * @param index the position of the attribute
     * @return the QName of the attribute
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    @Override
    public QName getAttributeName(int index) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read attribute when not in start element");
        }
        return startElementAttributes.get(index).getName();
    }

    /**
     * Returns the namespace of the attribute at the provided
     * index
     *
     * @param index the position of the attribute
     * @return the namespace URI (can be null)
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    @Override
    public String getAttributeNamespace(int index) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read attribute when not in start element");
        }
        return startElementAttributes.get(index).getNamespaceURI();
    }

    /**
     * Returns the localName of the attribute at the provided
     * index
     *
     * @param index the position of the attribute
     * @return the localName of the attribute
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    @Override
    public String getAttributeLocalName(int index) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read attribute when not in start element");
        }
        return startElementAttributes.get(index).getLocalName();
    }

    /**
     * Returns the prefix of this attribute at the
     * provided index
     *
     * @param index the position of the attribute
     * @return the prefix of the attribute
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    @Override
    public String getAttributePrefix(int index) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read attribute when not in start element");
        }
        return startElementAttributes.get(index).getPrefix();
    }

    /**
     * Returns a boolean which indicates if this
     * attribute was created by default
     *
     * @param index the position of the attribute
     * @return true if this is a default attribute
     * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
     */
    @Override
    public boolean isAttributeSpecified(int index) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read attribute when not in start element");
        }
        return false;
    }

    /**
     * Returns the count of namespaces declared on this START_ELEMENT or END_ELEMENT,
     * this method is only valid on a START_ELEMENT, END_ELEMENT or NAMESPACE. On
     * an END_ELEMENT the count is of the namespaces that are about to go
     * out of scope.  This is the equivalent of the information reported
     * by SAX callback for an end element event.
     *
     * @return returns the number of namespace declarations on this specific element
     * @throws IllegalStateException if this is not a START_ELEMENT, END_ELEMENT or NAMESPACE
     */
    @Override
    public int getNamespaceCount() {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read namespace count when not in start element");
        }
        return startElementNamespaces.size();
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
    @Override
    public String getNamespacePrefix(int index) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read namespace when not in start element");
        }
        return startElementNamespaces.get(index).getPrefix();
    }

    /**
     * Returns the uri for the namespace declared at the
     * index.
     *
     * @param index the position of the namespace declaration
     * @return returns the namespace uri
     * @throws IllegalStateException if this is not a START_ELEMENT, END_ELEMENT or NAMESPACE
     */
    @Override
    public String getNamespaceURI(int index) {
        if (currentEvent != START_ELEMENT) {
            throw new IllegalStateException("Cannot read namespace when not in start element");
        }
        return startElementNamespaces.get(index).getNamespaceURI();
    }

    /**
     * Return the uri for the given prefix.
     * The uri returned depends on the current state of the processor.
     *
     * <p><strong>NOTE:</strong>The 'xml' prefix is bound as defined in
     * <a href="http://www.w3.org/TR/REC-xml-names/#ns-using">Namespaces in XML</a>
     * specification to "http://www.w3.org/XML/1998/namespace".
     * </p>
     *
     * <p><strong>NOTE:</strong> The 'xmlns' prefix must be resolved to following namespace
     * <a href="http://www.w3.org/2000/xmlns/">http://www.w3.org/2000/xmlns/</a></p>
     *
     * @param prefix The prefix to lookup, may not be null
     * @return the uri bound to the given prefix or null if it is not bound
     * @throws IllegalArgumentException if the prefix is null
     */
    @Override
    public String getNamespaceURI(String prefix) {
        if (currentEvent == END_ELEMENT) {
            return currentEndElementNamespaceContext.getNamespaceURI(prefix);
        } else {
            return currentNamespaceContext.getNamespaceURI(prefix);
        }
    }

    /**
     * If the current event is a START_ELEMENT or END_ELEMENT  this method
     * returns the URI of the prefix or the default namespace.
     * Returns null if the event does not have a prefix.
     *
     * @return the URI bound to this elements prefix, the default namespace, or null
     */
    @Override
    public String getNamespaceURI() {
        switch (currentEvent) {
            case START_ELEMENT:
            case END_ELEMENT:
                return ((QNamed)currentNode).getNamespaceURI();
            default:
                return null;
        }
    }

    /**
     * Returns a read only namespace context for the current
     * position.  The context is transient and only valid until
     * a call to next() changes the state of the reader.
     *
     * @return return a namespace context
     */
    @Override
    public NamespaceContext getNamespaceContext() {
        if (currentEvent == END_ELEMENT) {
            return currentEndElementNamespaceContext;
        } else {
            return currentNamespaceContext;
        }
    }

    /**
     * Return true if the current event has text, false otherwise
     * The following events have text:
     * CHARACTERS,DTD ,ENTITY_REFERENCE, COMMENT, SPACE
     */
    @Override
    public boolean hasText() {
        switch (currentEvent) {
            case CHARACTERS:
            case CDATA:
            case COMMENT:
            case ENTITY_REFERENCE:
                return true;
            default:
                return false;
        }
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
     *
     * @return the current text or null
     * @throws IllegalStateException if this state is not
     *                               a valid text state.
     */
    @Override
    public String getText() {
        switch (currentEvent) {
            case CHARACTERS:
                return ((Text)currentNode).getText();
            case CDATA:
                return ((CData)currentNode).getCData();
            case COMMENT:
                return ((Comment)currentNode).getData();
            case ENTITY_REFERENCE:
                return ((EntityRef)currentNode).getName();
            default:
                throw new IllegalStateException("Cannot read text when not on node containing text");
        }
    }

    /**
     * Returns the length of the sequence of characters for this
     * Text event within the text character array.
     *
     * @throws IllegalStateException if this state is not
     *                               a valid text state.
     */
    @Override
    public int getTextLength() {
        return getText().length();
    }

    /**
     * Returns a QName for the current START_ELEMENT or END_ELEMENT event
     *
     * @return the QName for the current START_ELEMENT or END_ELEMENT event
     * @throws IllegalStateException if this is not a START_ELEMENT or
     *                               END_ELEMENT
     */
    @Override
    public QName getName() {
        switch (currentEvent) {
            case START_ELEMENT:
            case END_ELEMENT:
                return ((QNamed)currentNode).getName();
            default:
                throw new IllegalStateException("Cannot read name when not on start or end element");
        }
    }

    /**
     * Returns the (local) name of the current event.
     * For START_ELEMENT or END_ELEMENT returns the (local) name of the current element.
     * For ENTITY_REFERENCE it returns entity name.
     * The current event must be START_ELEMENT or END_ELEMENT,
     * or ENTITY_REFERENCE
     *
     * @return the localName
     * @throws IllegalStateException if this not a START_ELEMENT,
     *                               END_ELEMENT or ENTITY_REFERENCE
     */
    @Override
    public String getLocalName() {
        switch (currentEvent) {
            case START_ELEMENT:
            case END_ELEMENT:
                return ((QNamed)currentNode).getLocalName();
            default:
                throw new IllegalStateException("Cannot read name when not on start or end element");
        }
    }

    /**
     * returns true if the current event has a name (is a START_ELEMENT or END_ELEMENT)
     * returns false otherwise
     */
    @Override
    public boolean hasName() {
        switch (currentEvent) {
            case START_ELEMENT:
            case END_ELEMENT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the target of a processing instruction
     *
     * @return the target or null
     */
    @Override
    public String getPITarget() {
        switch (currentEvent) {
            case PROCESSING_INSTRUCTION:
                return ((ProcessingInstruction)currentNode).getTarget();
            default:
                throw new IllegalStateException("Cannot read PI target when not on processing instruction");
        }
    }

    /**
     * Get the data section of a processing instruction
     *
     * @return the data or null
     */
    @Override
    public String getPIData() {
        switch (currentEvent) {
            case PROCESSING_INSTRUCTION:
                return ((ProcessingInstruction)currentNode).getData();
            default:
                throw new IllegalStateException("Cannot read PI data when not on processing instruction");
        }
    }

    /**
     * Returns the prefix of the current event or null if the event does not have a prefix
     *
     * @return the prefix or null
     */
    @Override
    public String getPrefix() {
        switch (currentEvent) {
            case START_ELEMENT:
            case END_ELEMENT:
                return ((QNamed)currentNode).getPrefix();
            default:
                return null;
        }
    }

    /**
     * Frees any resources associated with this Reader.  This method does not close the
     * underlying input source.
     *
     * @throws XMLStreamException if there are errors freeing associated resources
     */
    @Override
    public void close() throws XMLStreamException {
        // do nothing
    }

    // <editor-fold desc="Document information implementation methods">
    /**
     * Return input encoding if known or null if unknown.
     *
     * @return the encoding of this instance or null
     */
    @Override
    public String getEncoding() {
        return null;
    }

    /**
     * Get the xml version declared on the xml declaration
     * Returns null if none was declared
     *
     * @return the XML version or null
     */
    @Override
    public String getVersion() {
        return null;
    }

    /**
     * Get the standalone declaration from the xml declaration
     *
     * @return true if this is standalone, or false otherwise
     */
    @Override
    public boolean isStandalone() {
        return false;
    }

    /**
     * Checks if standalone was set in the document
     *
     * @return true if standalone was set in the document, or false otherwise
     */
    @Override
    public boolean standaloneSet() {
        return false;
    }

    /**
     * Returns the character encoding declared on the xml declaration
     * Returns null if none was declared
     *
     * @return the encoding declared in the document or null
     */
    @Override
    public String getCharacterEncodingScheme() {
        return null;
    }
    // </editor-fold>

    // <editor-fold desc="Unsupported implementation methods">
    /**
     * Return the current location of the processor.
     * If the Location is unknown the processor should return
     * an implementation of Location that returns -1 for the
     * location and null for the publicId and systemId.
     * The location information is only valid until next() is
     * called.
     */
    @Override
    public Location getLocation() {
        return new Location() {
            @Override
            public int getLineNumber() {
                return -1;
            }

            @Override
            public int getColumnNumber() {
                return -1;
            }

            @Override
            public int getCharacterOffset() {
                return -1;
            }

            @Override
            public String getPublicId() {
                return null;
            }

            @Override
            public String getSystemId() {
                return null;
            }
        };
    }

    @Override
    public String getAttributeType(int index) {
        throw new UnsupportedOperationException("getAttributeType method not supported");
    }

    @Override
    public int nextTag() throws XMLStreamException {
        throw new UnsupportedOperationException("nextTag method not supported");
    }

    @Override
    public String getElementText() throws XMLStreamException {
        throw new UnsupportedOperationException("getElementText method not supported");
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return null;
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        throw new UnsupportedOperationException("require method not supported");
    }

    @Override
    public char[] getTextCharacters() {
        throw new UnsupportedOperationException("getTextCharacters method not supported");
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        throw new UnsupportedOperationException("getTextCharacters method not supported");
    }

    @Override
    public int getTextStart() {
        throw new UnsupportedOperationException("getTextStart method not supported");
    }
    // </editor-fold>
}
