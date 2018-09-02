package com.adpetions.optimus;

import com.adpetions.optimus.entities.EntityReferenceResolver;
import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.namespaces.TransformNamespaceContext;
import com.adpetions.optimus.nodes.NodeCollection;
import com.adpetions.optimus.templating.OptimusTransformTemplate;
import com.adpetions.optimus.templating.annotations.EventTemplate;
import com.adpetions.optimus.templating.annotations.ImportPriorityOffset;
import com.adpetions.optimus.templating.annotations.TemplateConfig;
import com.adpetions.optimus.templating.annotations.TemplateDefaultNamespaceURI;
import com.adpetions.optimus.templating.annotations.TemplateNamespace;
import com.adpetions.optimus.templating.annotations.TemplateNamespaces;
import com.adpetions.optimus.writers.TransformNullWriter;
import com.adpetions.optimus.writers.TransformSimpleWriter;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static com.adpetions.optimus.EventHandlerPathMap.NodeType.ATTRIBUTE;
import static com.adpetions.optimus.EventHandlerPathMap.NodeType.COMMENT;
import static com.adpetions.optimus.EventHandlerPathMap.NodeType.ELEMENT;
import static com.adpetions.optimus.EventHandlerPathMap.NodeType.PROCESSING_INSTRUCTION;
import static com.adpetions.optimus.EventHandlerPathMap.NodeType.TEXT;

/**
 * The main Transformer transformer class
 *
 * @param <T> the type of the cargo that will be passed to registered EventHandler.handle() methods
 */
public class Transformer<T> {
    T cargo;
    TransformContext context;
    TransformNamespaceContext namespaceContext;
    EntityReferenceResolver entityReferenceResolver;

    Reader reader;
    Writer writer;
    XMLStreamReader xmlReader;
    TransformXMLStreamWriter xmlWriter;

    // used by apply method...
    Stack<XMLStreamReader> applyReadersStack;
    int applyingLevel = 0;

    boolean suppressWhitespace = false;
    boolean trackAttributes = false;
    boolean forceNonSelfClosing = false;
    Set<QName> allowSelfClosing = new HashSet<>();
    boolean coalescing = false;
    boolean omitXmlDeclaration = true;
    boolean pathMapCaching = true;
    private boolean transformStarted = false;
    private boolean nested = false;
    boolean quit = false;

    boolean templated = false;
    private OptimusTransformTemplate template;
    private List<OptimusTransformTemplate> templateImports;

    EventHandlerPathMap startElementHandlers;
    EventHandlerPathMap endElementHandlers;
    EventHandlerPathMap attributeHandlers;
    EventHandlerPathMap beforeAttributesHandlers;
    EventHandlerPathMap afterAttributesHandlers;
    EventHandlerPathMap beforeNamespacesHandlers;
    EventHandlerPathMap afterNamespacesHandlers;
    EventHandlerPathMap afterStartElementHandlers;
    EventHandlerPathMap processingInstructionHandlers;
    EventHandlerPathMap commentHandlers;
    EventHandlerPathMap charactersHandlers;
    EventHandlerPathMap cDataHandlers;
    EventHandlerPathMap whitespaceHandlers;
    EventHandlerList startDocumentHandlers;
    EventHandlerList endDocumentHandlers;
    EventHandlerList entityReferenceHandlers;
    EventHandlerList namespaceHandlers;

    // <editor-fold desc="Constructors">
    /**
     * Instantiates the Transformer transformer to process the given
     * input XML string
     *
     * @param inputXml the input XML to transform
     */
    public Transformer(String inputXml) {
        namespaceContext = new TransformNamespaceContext();
        initializeHandlerMaps();
        reader = new StringReader(inputXml);
    }

    /**
     * Instantiates the Transformer transformer to process the given
     * input XML string
     *
     * @param inputXml the input XML to transform
     * @param namespaceContext the namespace context to be used to resolve
     *                         namespace prefixes
     */
    public Transformer(String inputXml, TransformNamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
        initializeHandlerMaps();
        reader = new StringReader(inputXml);
    }

    /**
     * Instantiates the Transformer transformer to process the given
     * input XML reader
     *
     * @param reader the input XML reader
     */
    public Transformer(Reader reader) {
        namespaceContext = new TransformNamespaceContext();
        initializeHandlerMaps();
        this.reader = reader;
    }

    /**
     * Instantiates the Transformer transformer to process the given
     * input XML reader
     *
     * @param reader the input XML reader
     * @param namespaceContext the namespace context to be used to resolve
     *                         namespace prefixes
     */
    public Transformer(Reader reader, TransformNamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
        initializeHandlerMaps();
        this.reader = reader;
    }
    // </editor-fold>

    // <editor-fold desc="Transform methods">
    /**
     * Performs the transform with string output
     *
     * @return the string transform result
     */
    public String transform() throws TransformException, XMLStreamException {
        templated = false;
        StringWriter writer = new StringWriter();
        transform(writer);
        return writer.toString();
    }

    /**
     * Performs the transform with string output using a transform template
     *
     * @param template the transform template to use
     * @return the string transform result
     */
    public String transform(OptimusTransformTemplate template) throws TransformException, XMLStreamException {
        templated = true;
        this.template = template;
        StringWriter writer = new StringWriter();
        transform(writer);
        return writer.toString();
    }

    /**
     * Performs the transform outputting to the specified writer
     *
     * @param writer the writer to be used for output
     */
    public void transform(Writer writer) throws TransformException, XMLStreamException {
        templated = false;
        this.writer = writer;
        xmlWriter = new TransformSimpleWriter(writer);
        doTransform(xmlWriter);
    }

    /**
     * Performs the transform outputting to the specified writer, using a transform template
     *
     * @param template the transform template to use
     * @param writer the writer to be used for output
     */
    public void transform(OptimusTransformTemplate template, Writer writer) throws TransformException, XMLStreamException {
        templated = true;
        this.template = template;
        this.writer = writer;
        xmlWriter = new TransformSimpleWriter(writer);
        doTransform(xmlWriter);
    }

    /**
     * Performs a nested transform outputting to the specified writer
     *
     * <p>NB. Passing null as the writer argument means that Transformer will use a null writer (i.e. no output)</p>
     *
     * @param xmlWriter the XML writer to be used for output
     */
    public void transform(TransformXMLStreamWriter xmlWriter) throws TransformException, XMLStreamException {
        nested = true;
        doTransform(xmlWriter);
    }

    /**
     * Performs the transform with a null output output
     */
    public void nullTransform() throws TransformException, XMLStreamException {
        templated = false;
        xmlWriter = new TransformNullWriter();
        doTransform(xmlWriter);
    }

    /**
     * Performs the transform with a null output output
     *
     * @param template the transform template to use
     */
    public void nullTransform(OptimusTransformTemplate template) throws TransformException, XMLStreamException {
        templated = true;
        this.template = template;
        xmlWriter = new TransformNullWriter();
        doTransform(xmlWriter);
    }
    // </editor-fold>

    // <editor-fold desc="Main transform engine methods">
    /**
     * Performs the transform outputting to the specified writer
     *
     * @param xmlWriter the XML writer to be used for output
     */
    private void doTransform(TransformXMLStreamWriter xmlWriter) throws TransformException, XMLStreamException {
        if (templated) {
            try {
                buildTemplating(template);
            } catch (Exception ex) {
                throw new TransformException("Exception reading templates", ex);
            }
        }
        // create the transform context...
        context = new TransformContext(this);
        if (templated) {
            try {
                template.initialize(this, context, xmlWriter);
                for (OptimusTransformTemplate imported : templateImports) {
                    imported.initialize(this, context, xmlWriter);
                }
            } catch (Exception ex) {
                throw new TransformException("Exception initializing templates", ex);
            }
        }
        // set the xml writer...
        this.xmlWriter = xmlWriter;
        // create the xml reader...
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        if (coalescing) {
            inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        } else {
            inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        }
        xmlReader = inputFactory.createXMLStreamReader(reader);
        // needed to move this outside the handler loop - as it doesn't get hit
        handleStartDocument();
        // read to end...
        quit = false;
        int nextEvent;
        while (!quit && xmlReader.hasNext()) {
            nextEvent = xmlReader.next();
            switch (nextEvent) {
                case XMLStreamConstants.END_DOCUMENT:
                    handleEndDocument();
                    break;
                case XMLStreamReader.START_ELEMENT:
                    handleStartElement();
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    handleEndElement();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    handleCharacters();
                    break;
                case XMLStreamConstants.CDATA:
                    handleCData();
                    break;
                case XMLStreamConstants.SPACE:
                    handleWhitespace();
                    break;
                case XMLStreamConstants.COMMENT:
                    handleComment();
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    handleProcessingInstruction();
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    handleEntityReference();
                    break;
            }
        }
    }

    private void handleStartDocument() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.START_DOCUMENT, nested);
        ContinueState continueState = startDocumentHandlers.callAll(context);
        if (continueState == ContinueState.CONTINUE && !nested && !omitXmlDeclaration) {
            xmlWriter.writeStartDocument();
        }
    }

    private void handleEndDocument() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.END_DOCUMENT);
        ContinueState continueState = endDocumentHandlers.callAll(context);
        if (continueState == ContinueState.CONTINUE && !nested) {
            xmlWriter.writeEndDocument();
        }
        context.popPathDocument(nested);
    }

    private void handleStartElement() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.START_ELEMENT);
        ContinueState continueState = startElementHandlers.callAll(context);
        if (context.elementHasBeenRead && applyingLevel == 0) {
            context.elementHasBeenRead = false;
            // fix up context path...
            context.popPathElement();
            // get out (the attributes and namespaces have already been read)...
            return;
        }
        switch (continueState) {
            case SKIP_THIS:
                context.skipThisElement();
                break;
            case SKIP_THIS_AND_DESCENDANTS:
                context.currentlySkipping = true;
                break;
            case CONTINUE:
                if (!context.currentlySkipping) {
                    xmlWriter.writeStartElement(context.overrideName);
                }
                break;
        }
        if (quit || context.currentlySkipping) {
            return;
        }
        // handle before attributes...
        handleBeforeAttributes();
        if (quit) {
            return;
        }
        // handle each attribute...
        for (int a = 0, amax = xmlReader.getAttributeCount(); a < amax; a++) {
            handleAttribute(a);
            if (quit) {
                return;
            }
        }
        // handle after attributes...
        handleAfterAttributes();
        if (quit) {
            return;
        }
        // handle before namespaces...
        handleBeforeNamespaces();
        if (quit) {
            return;
        }
        // handle each namespace...
        for (int n = 0, nmax = xmlReader.getNamespaceCount(); n < nmax; n++) {
            handleNamespace(n);
            if (quit) {
                return;
            }
        }
        // handle after namespaces...
        handleAfterNamespaces();
        if (quit) {
            return;
        }
        // handle after attributes and after namespaces...
        handleAfterStartElement();
    }

    private void handleBeforeAttributes() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.BEFORE_ATTRIBUTES);
        beforeAttributesHandlers.callAll(context);
    }

    private void handleAttribute(int index) throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.ATTRIBUTE, index, nested);
        ContinueState continueState = attributeHandlers.callAll(context);
        if (continueState == ContinueState.CONTINUE && !context.currentlySkipping && !context.isSkippingThisElement()) {
            xmlWriter.writeAttribute(context.overrideName, context.overrideAttributeValue);
        }
        context.popPath();
    }

    private void handleAfterAttributes() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.AFTER_ATTRIBUTES);
        afterAttributesHandlers.callAll(context);
    }

    private void handleBeforeNamespaces() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.BEFORE_NAMESPACES);
        beforeNamespacesHandlers.callAll(context);
    }

    private void handleNamespace(int index) throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.NAMESPACE, index, nested);
        ContinueState continueState = namespaceHandlers.callAll(context);
        if (continueState == ContinueState.CONTINUE && !context.currentlySkipping && !context.isSkippingThisElement()) {
            if (context.overrideNamespacePrefix != null && !context.overrideNamespacePrefix.isEmpty()) {
                xmlWriter.writeNamespace(context.overrideNamespacePrefix, context.overrideNamespaceURI);
            } else {
                xmlWriter.writeDefaultNamespace(context.overrideNamespaceURI);
            }
        }
        context.popPath();
    }

    private void handleAfterNamespaces() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.AFTER_NAMESPACES);
        afterNamespacesHandlers.callAll(context);
    }

    private void handleAfterStartElement() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.AFTER_START_ELEMENT);
        afterStartElementHandlers.callAll(context);
    }

    private void handleEndElement() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.END_ELEMENT);
        Boolean skippedThisElement = context.isSkippingThisElement();
        ContinueState continueState = endElementHandlers.callAll(context);
        if (continueState == ContinueState.CONTINUE && !context.currentlySkipping && !skippedThisElement) {
            if (forceNonSelfClosing) {
                QName elementQname = context.getName();
                if (!allowSelfClosing.contains(elementQname)) {
                    xmlWriter.writeCharacters("");
                }
            }
            xmlWriter.writeEndElement();
        }
        context.popPathElement();
    }

    private void handleCharacters() throws TransformException, XMLStreamException {
        if (xmlReader.isWhiteSpace()) {
            handleWhitespace();
        } else {
            context.initializeForEventHandler(EventType.CHARACTERS);
            ContinueState continueState = charactersHandlers.callAll(context);
            if (continueState == ContinueState.CONTINUE && !context.currentlySkipping) {
                xmlWriter.writeCharacters(context.overrideText);
            }
            context.popPath();
        }
    }

    private void handleCData() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.CDATA);
        ContinueState continueState = cDataHandlers.callAll(context);
        if (continueState == ContinueState.CONTINUE && !context.currentlySkipping) {
            xmlWriter.writeCData(context.overrideText);
        }
        context.popPath();
    }

    private void handleWhitespace() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.WHITE_SPACE);
        EventHandlerList handlers = whitespaceHandlers.getHolders(context.getPath());
        if (handlers.size() > 0) {
            ContinueState continueState = handlers.callAll(context);
            if (continueState == ContinueState.CONTINUE && !context.currentlySkipping) {
                xmlWriter.writeCharacters(context.overrideText);
            }
        } else if (!context.currentlySkipping && !suppressWhitespace) {
            xmlWriter.writeCharacters(xmlReader.getText());
        }
        context.popPath();
    }

    private void handleComment() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.COMMENT);
        ContinueState continueState = commentHandlers.callAll(context);
        if (continueState == ContinueState.CONTINUE && !context.currentlySkipping) {
            xmlWriter.writeComment(context.overrideText);
        }
        context.popPath();
    }

    private void handleProcessingInstruction() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.PROCESSING_INSTRUCTION);
        ContinueState continueState = processingInstructionHandlers.callAll(context);
        if (continueState == ContinueState.CONTINUE && !context.currentlySkipping) {
            xmlWriter.writeProcessingInstruction(context.overridePITarget, context.overridePIData);
        }
        context.popPath();
    }

    private void handleEntityReference() throws TransformException, XMLStreamException {
        context.initializeForEventHandler(EventType.ENTITY_REFERENCE);
        if (entityReferenceHandlers.size() > 0) {
            ContinueState continueState = entityReferenceHandlers.callAll(context);
            if (continueState == ContinueState.CONTINUE && !context.currentlySkipping) {
                xmlWriter.writeCharacters(context.overrideText);
            }
        } else {
            EventHandlerList handlers = charactersHandlers.getHolders(context.getPath());
            if (handlers.size() > 0) {
                ContinueState continueState = handlers.callAll(context);
                if (continueState == ContinueState.CONTINUE && !context.currentlySkipping) {
                    xmlWriter.writeCharacters(context.overrideText);
                }
            } else if (!context.currentlySkipping) {
                if (entityReferenceResolver != null) {
                    xmlWriter.writeCharacters(entityReferenceResolver.resolveEntityReference(xmlReader.getLocalName()));
                } else {
                    xmlWriter.writeEntityRef(xmlReader.getLocalName());
                }
            }
        }
        context.popPath();
    }
    // </editor-fold>

    // <editor-fold desc="Apply transform methods">
    void apply(NodeCollection nodes) throws TransformException, XMLStreamException {
        if (applyReadersStack == null) {
            applyingLevel = 0;
            applyReadersStack = new Stack<>();
        }
        applyingLevel++;
        applyReadersStack.push(xmlReader);
        xmlReader = new ApplyReader(nodes);
        int nextEvent;
        while (!quit && xmlReader.hasNext()) {
            nextEvent = xmlReader.next();
            switch (nextEvent) {
                case XMLStreamConstants.END_DOCUMENT:
                    handleEndDocument();
                    break;
                case XMLStreamReader.START_ELEMENT:
                    handleStartElement();
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    handleEndElement();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    handleCharacters();
                    break;
                case XMLStreamConstants.CDATA:
                    handleCData();
                    break;
                case XMLStreamConstants.SPACE:
                    handleWhitespace();
                    break;
                case XMLStreamConstants.COMMENT:
                    handleComment();
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    handleProcessingInstruction();
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    handleEntityReference();
                    break;
            }
        }
        applyingLevel--;
        xmlReader = applyReadersStack.pop();
    }
    // </editor-fold>

    // <editor-fold desc="Private initialization methods">
    /**
     * Initializes the handler maps
     */
    private void initializeHandlerMaps() {
        startElementHandlers = new EventHandlerPathMap(this, namespaceContext, ELEMENT);
        endElementHandlers = new EventHandlerPathMap(this, namespaceContext, ELEMENT);
        attributeHandlers =  new EventHandlerPathMap(this, namespaceContext, ATTRIBUTE);
        beforeAttributesHandlers = new EventHandlerPathMap(this, namespaceContext, ELEMENT);
        afterAttributesHandlers = new EventHandlerPathMap(this, namespaceContext, ELEMENT);
        beforeNamespacesHandlers = new EventHandlerPathMap(this, namespaceContext, ELEMENT);
        afterNamespacesHandlers = new EventHandlerPathMap(this, namespaceContext, ELEMENT);
        afterStartElementHandlers = new EventHandlerPathMap(this, namespaceContext, ELEMENT);
        processingInstructionHandlers = new EventHandlerPathMap(this, namespaceContext, PROCESSING_INSTRUCTION);
        commentHandlers = new EventHandlerPathMap(this, namespaceContext, COMMENT);
        charactersHandlers = new EventHandlerPathMap(this, namespaceContext, TEXT);
        cDataHandlers = new EventHandlerPathMap(this, namespaceContext, TEXT);
        whitespaceHandlers = new EventHandlerPathMap(this, namespaceContext, TEXT);
        startDocumentHandlers = new EventHandlerList();
        endDocumentHandlers = new EventHandlerList();
        entityReferenceHandlers = new EventHandlerList();
        namespaceHandlers = new EventHandlerList();
    }
    // </editor-fold>

    // <editor-fold desc="Handler register methods">
    public Transformer registerHandler(EventType eventType, EventHandler handler) throws TransformException {
        return registerHandler(eventType, 0, handler);
    }

    /**
     * Registers a handler to be used during transform
     *
     * @param eventType the event type for which the handler is to be registered
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerHandler(EventType eventType, int priority, EventHandler<T> handler) throws TransformException {
        switch (eventType) {
            case START_ELEMENT:
                registerStartElementHandler(priority, handler);
                break;
            case END_ELEMENT:
                registerEndElementHandler(priority, handler);
                break;
            case ATTRIBUTE:
                registerAttributeHandler(priority, handler);
                break;
            case BEFORE_ATTRIBUTES:
                registerBeforeAttributesHandler(priority, handler);
                break;
            case AFTER_ATTRIBUTES:
                registerAfterAttributesHandler(priority, handler);
                break;
            case BEFORE_NAMESPACES:
                registerBeforeNamespacesHandler(priority, handler);
                break;
            case AFTER_NAMESPACES:
                registerAfterNamespacesHandler(priority, handler);
                break;
            case AFTER_START_ELEMENT:
                registerAfterStartElementHandler(priority, handler);
                break;
            case PROCESSING_INSTRUCTION:
                registerProcessingInstructionHandler(priority, handler);
                break;
            case CHARACTERS:
                registerCharactersHandler(priority, handler);
                break;
            case COMMENT:
                registerCommentHandler(priority, handler);
                break;
            case WHITE_SPACE:
                registerWhitespaceHandler(priority, handler);
                break;
            case START_DOCUMENT:
                registerStartDocumentHandler(priority, handler);
                break;
            case END_DOCUMENT:
                registerEndDocumentHandler(priority, handler);
                break;
            case CDATA:
                registerCdataHandler(priority, handler);
                break;
            case NAMESPACE:
                registerNamespaceHandler(priority, handler);
                break;
            case ENTITY_REFERENCE:
                registerEntityReferenceHandler(priority, handler);
                break;
            default:
                throw new TransformException("Unhandled event type register - " + eventType.toString());
        }
        return this;
    }

    /**
     * Registers a handler for start elements
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerStartElementHandler(EventHandler<T> handler) throws TransformException {
        return registerStartElementHandler("*", 0, handler);
    }

    /**
     * Registers a handler for start elements
     *
     * @param handler the handler to be registered
     * @param priority the priority for the event (higher priority handlers are called first)
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerStartElementHandler(int priority, EventHandler<T> handler) throws TransformException {
        return registerStartElementHandler("*", priority, handler);
    }

    /**
     * Registers a handler for start elements
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerStartElementHandler(String pathMatch, EventHandler<T> handler) throws TransformException {
        return registerStartElementHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a handler for start elements
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerStartElementHandler(String pathMatch, int priority, EventHandler<T> handler) {
        startElementHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a handler for end elements
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerEndElementHandler(EventHandler<T> handler)  {
        return registerEndElementHandler("*", 0, handler);
    }

    /**
     * Registers a handler for end elements
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerEndElementHandler(int priority, EventHandler<T> handler) {
        return registerEndElementHandler("*", priority, handler);
    }

    /**
     * Registers a handler for end elements
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerEndElementHandler(String pathMatch, EventHandler<T> handler) {
        return registerEndElementHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a handler for end elements
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerEndElementHandler(String pathMatch, int priority, EventHandler<T> handler) {
        endElementHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a handler for attributes
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAttributeHandler(EventHandler<T> handler) {
        return registerAttributeHandler("@*", 0, handler);
    }

    /**
     * Registers a handler for attributes
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAttributeHandler(int priority, EventHandler<T> handler) {
        return registerAttributeHandler("@*", priority, handler);
    }

    /**
     * Registers a handler for attributes
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAttributeHandler(String pathMatch, EventHandler<T> handler) {
        return registerAttributeHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a handler for attributes
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAttributeHandler(String pathMatch, int priority, EventHandler<T> handler) {
        attributeHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a handler for before attributes
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerBeforeAttributesHandler(EventHandler<T> handler) {
        return registerBeforeAttributesHandler("*", 0, handler);
    }

    /**
     * Registers a handler for before attributes
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerBeforeAttributesHandler(int priority, EventHandler<T> handler) {
        return registerBeforeAttributesHandler("*", priority, handler);
    }

    /**
     * Registers a handler for before attributes
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerBeforeAttributesHandler(String pathMatch, EventHandler<T> handler) {
        return registerBeforeAttributesHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a handler for before attributes
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerBeforeAttributesHandler(String pathMatch, int priority, EventHandler<T> handler) {
        beforeAttributesHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a handler for after attributes
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterAttributesHandler(EventHandler<T> handler) {
        return registerAfterAttributesHandler("*", 0, handler);
    }

    /**
     * Registers a handler for after attributes
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterAttributesHandler(int priority, EventHandler<T> handler) {
        return registerAfterAttributesHandler("*", priority, handler);
    }

    /**
     * Registers a handler for after attributes
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterAttributesHandler(String pathMatch, EventHandler<T> handler) {
        return registerAfterAttributesHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a handler for after attributes
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterAttributesHandler(String pathMatch, int priority, EventHandler<T> handler) {
        afterAttributesHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a handler for before namespaces
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerBeforeNamespacesHandler(EventHandler<T> handler) {
        return registerBeforeNamespacesHandler("*", 0, handler);
    }

    /**
     * Registers a handler for before namespaces
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerBeforeNamespacesHandler(int priority, EventHandler<T> handler) {
        return registerBeforeNamespacesHandler("*", priority, handler);
    }

    /**
     * Registers a handler for before namespaces
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerBeforeNamespacesHandler(String pathMatch, EventHandler<T> handler) {
        return registerBeforeNamespacesHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a handler for before namespaces
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerBeforeNamespacesHandler(String pathMatch, int priority, EventHandler<T> handler) {
        beforeNamespacesHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a handler for after namespaces
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterNamespacesHandler(EventHandler<T> handler) {
        return registerAfterNamespacesHandler("*", 0, handler);
    }

    /**
     * Registers a handler for after namespaces
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterNamespacesHandler(int priority, EventHandler<T> handler) {
        return registerAfterNamespacesHandler("*", priority, handler);
    }

    /**
     * Registers a handler for after namespaces
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterNamespacesHandler(String pathMatch, EventHandler<T> handler) {
        return registerAfterNamespacesHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a handler for after namespaces
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterNamespacesHandler(String pathMatch, int priority, EventHandler<T> handler) {
        afterNamespacesHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a handler for after start element
     * (i.e. after both attributes and namespaces have been handled)
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterStartElementHandler(EventHandler<T> handler) {
        return registerAfterStartElementHandler("*", 0, handler);
    }

    /**
     * Registers a handler for after start element
     * (i.e. after both attributes and namespaces have been handled)
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterStartElementHandler(int priority, EventHandler<T> handler) {
        return registerAfterStartElementHandler("*", priority, handler);
    }

    /**
     * Registers a handler for after start element
     * (i.e. after both attributes and namespaces have been handled)
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterStartElementHandler(String pathMatch, EventHandler<T> handler) {
        return registerAfterStartElementHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a handler for after start element
     * (i.e. after both attributes and namespaces have been handled)
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerAfterStartElementHandler(String pathMatch, int priority, EventHandler<T> handler) {
        afterStartElementHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a processing instruction handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerProcessingInstructionHandler(EventHandler<T> handler) {
        return registerProcessingInstructionHandler("*", 0, handler);
    }

    /**
     * Registers a processing instruction handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerProcessingInstructionHandler(int priority, EventHandler<T> handler) {
        return registerProcessingInstructionHandler("*", priority, handler);
    }

    /**
     * Registers a processing instruction handler
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerProcessingInstructionHandler(String pathMatch, EventHandler<T> handler) {
        return registerProcessingInstructionHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a processing instruction handler
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerProcessingInstructionHandler(String pathMatch, int priority, EventHandler<T> handler) {
        processingInstructionHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a characters (text) handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCharactersHandler(EventHandler<T> handler) {
        return registerCharactersHandler("#*", 0, handler);
    }

    /**
     * Registers a characters (text) handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCharactersHandler(int priority, EventHandler<T> handler) {
        return registerCharactersHandler("#*", priority, handler);
    }

    /**
     * Registers a characters (text) handler
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCharactersHandler(String pathMatch, EventHandler<T> handler) {
        return registerCharactersHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a characters (text) handler
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCharactersHandler(String pathMatch, int priority, EventHandler<T> handler) {
        charactersHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a whitespace handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerWhitespaceHandler(EventHandler<T> handler) {
        return registerWhitespaceHandler("#*", 0, handler);
    }

    /**
     * Registers a whitespace handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerWhitespaceHandler(int priority, EventHandler<T> handler) {
        return registerWhitespaceHandler("#*", priority, handler);
    }

    /**
     * Registers a whitespace handler
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerWhitespaceHandler(String pathMatch, EventHandler<T> handler) {
        return registerWhitespaceHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a whitespace handler
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerWhitespaceHandler(String pathMatch, int priority, EventHandler<T> handler) {
        whitespaceHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a CDATA handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCdataHandler(EventHandler<T> handler) {
        return registerCdataHandler("#*", 0, handler);
    }

    /**
     * Registers a CDATA handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCdataHandler(int priority, EventHandler<T> handler) {
        return registerCdataHandler("#*", priority, handler);
    }

    /**
     * Registers a CDATA handler
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCdataHandler(String pathMatch, EventHandler<T> handler) {
        return registerCdataHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a CDATA handler
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCdataHandler(String pathMatch, int priority, EventHandler<T> handler) {
        cDataHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a comment handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCommentHandler(EventHandler<T> handler) {
        return registerCommentHandler("*", 0, handler);
    }

    /**
     * Registers a comment handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCommentHandler(int priority, EventHandler<T> handler) {
        return registerCommentHandler("*", priority, handler);
    }

    /**
     * Registers a comment handler
     *
     * @param pathMatch the path to match
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCommentHandler(String pathMatch, EventHandler<T> handler) {
        return registerCommentHandler(pathMatch, 0, handler);
    }

    /**
     * Registers a comment handler
     *
     * @param pathMatch the path to match
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerCommentHandler(String pathMatch, int priority, EventHandler<T> handler) {
        commentHandlers.add(new EventHandlerHolder(pathMatch, priority, handler));
        return this;
    }

    /**
     * Registers a start document handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerStartDocumentHandler(EventHandler<T> handler) {
        return registerStartDocumentHandler(0, handler);
    }

    /**
     * Registers a start document handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerStartDocumentHandler(int priority, EventHandler<T> handler) {
        startDocumentHandlers.add(new EventHandlerHolder(null, priority, handler));
        return this;
    }

    /**
     * Registers a start document handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerEndDocumentHandler(EventHandler<T> handler) {
        return registerEndDocumentHandler(0, handler);
    }

    /**
     * Registers a start document handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerEndDocumentHandler(int priority, EventHandler<T> handler) {
        endDocumentHandlers.add(new EventHandlerHolder(null, priority, handler));
        return this;
    }

    /**
     * Registers a namespace handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerNamespaceHandler(EventHandler<T> handler) {
        return registerNamespaceHandler(0, handler);
    }

    /**
     * Registers a namespace handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerNamespaceHandler(int priority, EventHandler<T> handler) {
        namespaceHandlers.add(new EventHandlerHolder(null, priority, handler));
        return this;
    }

    /**
     * Registers an entity reference handler
     *
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerEntityReferenceHandler(EventHandler<T> handler) {
        return registerEntityReferenceHandler(0, handler);
    }

    /**
     * Registers an entity reference handler
     *
     * @param priority the priority for the event (higher priority handlers are called first)
     * @param handler the handler to be registered
     * @return the Transformer transformer (for chained calls)
     */
    public Transformer registerEntityReferenceHandler(int priority, EventHandler<T> handler) {
        entityReferenceHandlers.add(new EventHandlerHolder(null, priority, handler));
        return this;
    }

    // </editor-fold>

    // <editor-fold desc="Template handling methods">
    void buildTemplating(OptimusTransformTemplate template) throws TransformException {
        this.template = template;
        this.templateImports = new ArrayList<>();
        this.allowSelfClosing.clear();
        buildNamespacesFromTemplate(template);
        buildOptionsFromTemplate();
        registerHandlersForTemplate(template, false, 0);
        buildImportedTemplates(template, 0);
    }

    private List<Class> getSuperClasses(OptimusTransformTemplate template) throws TransformException {
        List<Class> result = new ArrayList<>();
        result.add(template.getClass());
        Class sup = template.getClass().getSuperclass();
        while (sup != null) {
            result.add(0, sup);
            sup = sup.getSuperclass();
            if (Object.class.equals(sup)) {
                sup = null;
            }
        }
        return result;
    }

    private void buildNamespacesFromTemplate(OptimusTransformTemplate template) throws TransformException {
        // get all class + super-classes...
        List<Class> clazzes = getSuperClasses(template);
        // go through namespaces on each class in inheritance chain...
        for (Class clazz: clazzes) {
            TemplateDefaultNamespaceURI templateDefaultNamespaceURI = (TemplateDefaultNamespaceURI)clazz.getAnnotation(TemplateDefaultNamespaceURI.class);
            if (templateDefaultNamespaceURI != null) {
                namespaceContext.setDefaultNamespaceURI(templateDefaultNamespaceURI.value());
            }
            TemplateNamespace templateNamespace = (TemplateNamespace)clazz.getAnnotation(TemplateNamespace.class);
            if (templateNamespace != null) {
                namespaceContext.addNamespace(templateNamespace.prefix(), templateNamespace.uri());
            }
            TemplateNamespaces templateNamespaces = (TemplateNamespaces)clazz.getAnnotation(TemplateNamespaces.class);
            if (templateNamespaces != null) {
                for (TemplateNamespace namespace: templateNamespaces.value()) {
                    namespaceContext.addNamespace(namespace.prefix(), namespace.uri());
                }
            }
        }
    }

    private void buildOptionsFromTemplate() throws TransformException {
        // get all class + super-classes...
        List<Class> clazzes = getSuperClasses(template);
        // go through namespaces on each class in inheritance chain...
        for (Class clazz: clazzes) {
            TemplateConfig templateConfig = (TemplateConfig)clazz.getAnnotation(TemplateConfig.class);
            if (templateConfig != null) {
                for (TemplateConfig.ConfigOptions option : templateConfig.options()) {
                    switch (option) {
                        case SUPPRESS_WHITESPACE:
                            this.suppressWhitespace = true;
                            break;
                        case NO_SUPPRESS_WHITESPACE:
                            this.suppressWhitespace = false;
                            break;
                        case TRACK_ATTRIBUTES:
                            this.trackAttributes = true;
                            break;
                        case NO_TRACK_ATTRIBUTES:
                            this.trackAttributes = false;
                            break;
                        case COALESCING:
                            this.coalescing = true;
                            break;
                        case NO_COALESCING:
                            this.coalescing = false;
                            break;
                        case FORCE_NON_SELF_CLOSING:
                            this.forceNonSelfClosing = true;
                            break;
                        case NO_FORCE_NON_SELF_CLOSING:
                            this.forceNonSelfClosing = false;
                            break;
                        case OMIT_XML_DECLARATION:
                            this.omitXmlDeclaration = true;
                            break;
                        case NO_OMIT_XML_DECLARATION:
                            this.omitXmlDeclaration = false;
                            break;
                        case PATH_MAP_CACHING_ON:
                            this.pathMapCaching = true;
                            break;
                        case PATH_MAP_CACHING_OFF:
                            this.pathMapCaching = false;
                            break;
                    }
                }
                String[] configAllowSelfClosers = templateConfig.allowSelfClosing();
                if (configAllowSelfClosers != null) {
                    for (String selfCloser: configAllowSelfClosers) {
                        this.allowSelfClosing.add(nameToQName(selfCloser, namespaceContext));
                    }
                }
            }
        }
    }

    private void buildImportedTemplates(OptimusTransformTemplate template, int importerPriorityOffset) throws TransformException {
        try {
            List<OptimusTransformTemplate> imports = template.getImports();
            if (imports != null) {
                templateImports.addAll(imports);
                for (OptimusTransformTemplate imported : imports) {
                    int priorityOffset = 0;
                    ImportPriorityOffset priorityOffsetAnnotation = imported.getClass().getAnnotation(ImportPriorityOffset.class);
                    if (priorityOffsetAnnotation != null) {
                        priorityOffset = priorityOffsetAnnotation.value();
                    }
                    buildNamespacesFromTemplate(imported);
                    registerHandlersForTemplate(imported, true, priorityOffset + importerPriorityOffset);
                    buildImportedTemplates(imported, priorityOffset + importerPriorityOffset);
                }
            }
        } catch (Exception e) {
            throw new TransformException("Error processing imports", e);
        }
    }

    private void registerHandlersForTemplate(OptimusTransformTemplate template, boolean imported, int priorityOffset) throws TransformException {
        if (!imported) {
            // top level template - clear the current handlers...
            initializeHandlerMaps();
        }
        // get the methods...
        Method[] methods = template.getClass().getMethods();
        EventTemplate eventTemplate;
        for (Method method: methods) {
            eventTemplate = method.getAnnotation(EventTemplate.class);
            if (eventTemplate != null) {
                // only methods with zero arity...
                if (method.getParameterCount() == 0 && method.getReturnType().equals(ContinueState.class)) {
                    registerTemplateMethod(eventTemplate.event(), eventTemplate.matchPath(), eventTemplate.priority() + priorityOffset, method, template);
                } else if (method.getParameterCount() != 0) {
                    throw new TransformException("Incorrect arity on template method '" + method.getName() + "'");
                } else {
                    throw new TransformException("Incorrect return type on template method '" + method.getName() + "'");
                }
            }
        }
    }

    private void registerTemplateMethod(EventType eventType, String matchPath, int priority, Method method, Object ownerObject) throws TransformException {
        EventHandlerHolder holder = new EventHandlerHolder(matchPath, priority, method, ownerObject);
        switch (eventType) {
            case START_ELEMENT:
                startElementHandlers.add(holder);
                break;
            case END_ELEMENT:
                endElementHandlers.add(holder);
                break;
            case ATTRIBUTE:
                attributeHandlers.add(holder);
                break;
            case BEFORE_ATTRIBUTES:
                beforeAttributesHandlers.add(holder);
                break;
            case AFTER_ATTRIBUTES:
                afterAttributesHandlers.add(holder);
                break;
            case BEFORE_NAMESPACES:
                beforeNamespacesHandlers.add(holder);
                break;
            case AFTER_NAMESPACES:
                afterNamespacesHandlers.add(holder);
                break;
            case AFTER_START_ELEMENT:
                afterStartElementHandlers.add(holder);
                break;
            case PROCESSING_INSTRUCTION:
                processingInstructionHandlers.add(holder);
                break;
            case COMMENT:
                commentHandlers.add(holder);
                break;
            case CHARACTERS:
                charactersHandlers.add(holder);
                break;
            case CDATA:
                cDataHandlers.add(holder);
                break;
            case WHITE_SPACE:
                whitespaceHandlers.add(holder);
                break;
            case START_DOCUMENT:
                startDocumentHandlers.add(holder);
                break;
            case END_DOCUMENT:
                endDocumentHandlers.add(holder);
                break;
            case NAMESPACE:
                namespaceHandlers.add(holder);
                break;
            case ENTITY_REFERENCE:
                entityReferenceHandlers.add(holder);
                break;
            default:
                throw new TransformException("Unhandled event type register - " + eventType.toString());
        }
    }
    // </editor-fold>

    // <editor-fold desc="Accessors">
    /**
     * Get the namespace context
     * @return the namespace context
     */
    public TransformNamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    /**
     * Get the entity reference resolver
     * @return the entity reference resolver
     */
    public EntityReferenceResolver getEntityReferenceResolver() {
        return entityReferenceResolver;
    }

    /**
     * Set the entity reference resolver
     * @param entityReferenceResolver the entity reference resolver
     */
    public void setEntityReferenceResolver(EntityReferenceResolver entityReferenceResolver) {
        this.entityReferenceResolver = entityReferenceResolver;
    }

    /**
     * Get the cargo that is to be passed to event handlers
     * @return the cargo that is to be passed to event handlers
     */
    public T getCargo() {
        return cargo;
    }

    /**
     * Set the cargo that is to be passed to event handlers
     * @param cargo the cargo that is to be passed to event handlers
     */
    public void setCargo(T cargo) {
        this.cargo = cargo;
    }

    /**
     * Get whether the transform is tracking attributes
     * @return whether the transform is tracking attributes
     */

    public boolean getTrackAttributes() {
        return trackAttributes;
    }

    /**
     * Set whether the transform is to track attributes
     * @param trackAttributes whether the transform is to track attributes
     */
    public void setTrackAttributes(boolean trackAttributes) {
        if (transformStarted) {
            throw new IllegalStateException("Attribute tracking may not be set once transform has started");
        }
        this.trackAttributes = trackAttributes;
    }

    /**
     * Get whether the transform is to suppress whitespace
     * @return whether the transform is to suppress whitespace
     */
    public boolean getSuppressWhitespace() {
        return suppressWhitespace;
    }

    /**
     * Set whether the tranform is to suppress whitespace
     * @param suppressWhitespace whether the tranform is to suppress whitespace
     */
    public void setSuppressWhitespace(boolean suppressWhitespace) {
        if (transformStarted) {
            throw new IllegalStateException("Suppression of whitespace may not be set once transform has started");
        }
        this.suppressWhitespace = suppressWhitespace;
    }

    /**
     * Get whether the transform is to force non-self closing elements
     * @return whether the transform is to force non-self closing elements
     */
    public boolean getForceNonSelfClosing() {
        return forceNonSelfClosing;
    }

    /**
     * Set whether the transform is to force non-self closing elements
     * @param forceNonSelfClosing whether the transform is to force non-self closing elements
     */
    public void setForceNonSelfClosing(boolean forceNonSelfClosing) {
        if (transformStarted) {
            throw new IllegalStateException("Force non-self closing may not be set once transform has started");
        }
        this.forceNonSelfClosing = forceNonSelfClosing;
    }

    /**
     * Get the set of QNames of elements that are allowed to be self closing
     * @return the set of QNames of elements that are allowed to be self closing
     */
    public Set<QName> getAllowSelfClosing() {
        return new HashSet<>(this.allowSelfClosing);
    }

    /**
     * Set the QNames of elements that are allowed to be self closing
     * @param allowSelfClosing the QNames of elements that are allowed to be self closing
     */
    public void setAllowSelfClosing(Set<QName> allowSelfClosing) {
        if (transformStarted) {
            throw new IllegalStateException("Allow self-closing elements may not be set once transform has started");
        }
        this.allowSelfClosing = allowSelfClosing;
    }

    /**
     * Get whether the XML reader is to coalesce
     * (Coalescing ensures that text nodes are read with one event)
     * @return whether the XML reader is to coalesce
     */
    public boolean getCoalescing() {
        return coalescing;
    }

    /**
     * Set whether the XML reader is to coalesce
     * (Coalescing ensures that text nodes are read with one event)
     * @param coalescing whether the XML reader is to coalesce
     */
    public void setCoalescing(boolean coalescing) {
        if (transformStarted) {
            throw new IllegalStateException("Coalescing may not be set once transform has started");
        }
        this.coalescing = coalescing;
    }


    /**
     * Get whether the XML Declaration should be omitted at the start of the document
     * @return whether the XML Declaration should be omitted at the start of the document
     */
    public boolean getOmitXmlDeclaration() {
        return omitXmlDeclaration;
    }

    /**
     * Set whether the XML Declaration should be omitted at the start of the document
     * @param omitXmlDeclaration whether the XML Declaration should be omitted at the start of the document
     */
    public void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
        this.omitXmlDeclaration = omitXmlDeclaration;
    }

    /**
     * Get whether path map caching is turned on
     * When path map caching is turned on, the handlers for each node path are cached - the transform
     * will be slightly faster but consume more memory
     * @return whether path map caching is turned on
     */
    public boolean getPathMapCaching() {
        return pathMapCaching;
    }

    /**
     * Set whether path map caching is turned on
     * When path map caching is turned on, the handlers for each node path are cached - the transform
     * will be slightly faster but consume more memory
     * @param pathMapCaching whether path map caching is turned on
     */
    public void setPathMapCaching(boolean pathMapCaching) {
        this.pathMapCaching = pathMapCaching;
    }
    // </editor-fold>

    private static QName nameToQName(String name, TransformNamespaceContext namespaceContext) throws TransformException {
        QName result;
        String defaultNamespaceUri = namespaceContext.getDefaultNamespaceURI();
        if (name.contains(":")) {
            String[] parts = name.split(":");
            String prefix = parts[0];
            String localName = parts[1];
            String uri = namespaceContext.getNamespaceURI(prefix);
            if (uri == null) {
                throw new TransformException("Prefix '" + prefix + "' is not bound to uri");
            }
            result = new QName(uri, localName);
        } else if (defaultNamespaceUri != null) {
            result = new QName(defaultNamespaceUri, name);
        } else {
            result = new QName(name);
        }
        return result;
    }
}