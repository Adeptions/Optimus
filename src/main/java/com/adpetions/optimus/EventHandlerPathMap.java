package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.BadMatchPathException;
import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.namespaces.TransformNamespaceContext;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Class used to map a path to matching handlers
 */
class EventHandlerPathMap {
    /**
     * Enumerator of node types used by handler mapping
     */
    enum NodeType {
        ELEMENT,
        ATTRIBUTE,
        TEXT,
        COMMENT,
        PROCESSING_INSTRUCTION
    }

    private NodeType nodeType;
    private Transformer ownerTransformer;
    private Map<String,EventHandlerList> patchMatchCache;
    private String wildcard = "*";
    private boolean isSpecial = false;
    private String specialWildcard;
    private String nodeTypePrefix;
    private Map<QName, EventHandlerPathMap> pathMap;
    private Object mappedHere;
    private TransformNamespaceContext namespaceContext;

    /**
     * Constructs a new instance of a EventHandlerPathMap
     *
     * @param namespaceContext the namespace context used to resolve prefixes to URIs
     * @param nodeType the type of the node
     */
    EventHandlerPathMap(Transformer transformer, TransformNamespaceContext namespaceContext, NodeType nodeType) {
        this.nodeType = nodeType;
        this.ownerTransformer = transformer;
        this.patchMatchCache = new HashMap<>();
        switch (nodeType) {
            case ELEMENT:
                nodeTypePrefix = null;
                wildcard = "*";
                isSpecial = false;
                break;
            case ATTRIBUTE:
                nodeTypePrefix = "@";
                wildcard = nodeTypePrefix + "*";
                isSpecial = false;
                break;
            case TEXT:
                nodeTypePrefix = "#";
                wildcard = nodeTypePrefix + "*";
                isSpecial = true;
                specialWildcard = "#text()";
                break;
            case PROCESSING_INSTRUCTION:
                nodeTypePrefix = "?";
                wildcard = nodeTypePrefix + "*";
                isSpecial = false;
                break;
            case COMMENT:
                nodeTypePrefix = "!";
                wildcard = nodeTypePrefix + "*";
                isSpecial = true;
                specialWildcard = "!comment()";
                break;
            default:
                throw new BadMatchPathException("Unexpected node type (" + nodeType + ")");
        }
        pathMap = new HashMap<>();
        this.namespaceContext = namespaceContext;
    }

    ContinueState callAll(TransformContext context) throws XMLStreamException, TransformException {
        Stack<EventHandlerHolder> stack = this.getHolders(context.path).toStack();
        return context.callStack(stack);
    }

    /**
     * Constructs a new instance of a EventHandlerPathMap
     * Used internally by EventHandlerPathMap - to map ancestry path
     * NB. Because this is used as ancestry path, the node type is always
     * an element because all other nodes can only ever be parented by an element
     *
     * @param namespaceContext the namespace context used to resolve prefixes to URIs
     */
    private EventHandlerPathMap(TransformNamespaceContext namespaceContext) {
        pathMap = new HashMap<>();
        this.namespaceContext = namespaceContext;
    }

    /**
     * Adds a handler to the map - tracking the specified pathing ancestry
     *
     * @param holder the event handler holder
     */
    void add(EventHandlerHolder holder) throws BadMatchPathException {
        String pathMatch = holder.matchPath;
        // break the path match by unions...
        String[] pathUnions = pathMatch.split("\\|");
        String unionPart;
        String[] pathParts;
        String[] qNameParts;
        String prefix;
        String localName;
        String namespaceURI;
        QName qname;
        ///List<String> resolvedPath;
        List<QName> resolvedPath;
        String defaultNamespaceURI = namespaceContext.getDefaultNamespaceURI();
        for (String pathUnion : pathUnions) {
            unionPart = pathUnion.replaceAll(" ", "");
            // break the path apart...
            pathParts = unionPart.split("/");
            // process each path part...
            resolvedPath = new ArrayList<>();
            int imax = pathParts.length - 1;
            for (int i = imax; i >= 0; i--) {
                localName = pathParts[i];
                if (!localName.equals("")) {
                    prefix = "";
                    namespaceURI = null;
                    // resolve any namespace prefixes...
                    if (localName.contains(":")) {
                        qNameParts = localName.split(":");
                        prefix = qNameParts[0];
                        localName = qNameParts[1];
                        if (nodeTypePrefix != null && prefix.startsWith(nodeTypePrefix)) {
                            prefix = prefix.substring(1);
                            localName = nodeTypePrefix + localName;
                        }
                        namespaceURI = namespaceContext.getNamespaceURI(prefix);
                    } else if (i == imax && localName.equals("*")) {
                        localName = (isSpecial ? specialWildcard : wildcard);
                    } else if (defaultNamespaceURI != null && !localName.equals(wildcard) && !(nodeTypePrefix != null && localName.startsWith(nodeTypePrefix))) {
                        namespaceURI = defaultNamespaceURI;
                    }
                    qname = new QName(namespaceURI, localName, prefix);
                } else if (i == 0) {
                    // an empty path part at the beginning means root...
                    qname = new QName("/");
                } else {
                    // an empty path part anywhere else is invalid
                    // (descendant path isn't yet supported)
                    throw new BadMatchPathException("Descendant path not supported by Transformer '" + unionPart + "'");
                }
                // build the reverse path...
                resolvedPath.add(qname);
            }
            // and map the resolved path...
            mapHandler(holder, resolvedPath, 0, resolvedPath.size() - 1);
        }
    }

    /**
     * Maps a specified resolved path and handler into the mapping tree
     *
     * @param holder the event handler holder
     * @param resolvedPath the resolved path
     * @param depth the current depth in the resolved path
     * @param maxDepth the maximum path depth
     */
    private void mapHandler(EventHandlerHolder holder, List<QName> resolvedPath, int depth, int maxDepth) throws BadMatchPathException {
        EventHandlerPathMap ancestorMap;
        QName pathAtDepth = resolvedPath.get(depth);
        if (pathMap.containsKey(pathAtDepth)) {
            ancestorMap = pathMap.get(pathAtDepth);
        } else {
            ancestorMap = new EventHandlerPathMap(namespaceContext);
            pathMap.put(pathAtDepth, ancestorMap);
        }
        if (depth == maxDepth) {
            if (ancestorMap.mappedHere == null) {
                // nothing yet - set the handler for here...
                ancestorMap.mappedHere = holder;
            } else if (ancestorMap.mappedHere instanceof EventHandlerPathMap) {
                // already have a handler here - make it into a list...
                Object wasMapped = ancestorMap.mappedHere;
                EventHandlerList handlerList = new EventHandlerList();
                ancestorMap.mappedHere = handlerList;
                handlerList.add((EventHandlerHolder)wasMapped);
                handlerList.add(holder);
            } else if (ancestorMap.mappedHere instanceof EventHandlerList) {
                // a list already...
                ((EventHandlerList)ancestorMap.mappedHere).add(holder);
            } else {
                // whoops, multiple handlers for same path...
                throw new BadMatchPathException("Multiple handlers for same path (" + holder.matchPath + ")");
            }
        } else {
            ancestorMap.mapHandler(holder, resolvedPath, depth + 1, maxDepth);
        }
    }

    /**
     * Obtains the list of holders that match the given path
     *
     * @param path the path list
     * @return the list of matching handlers for the path
     */
    EventHandlerList getHolders(List<QName> path) {
        if (ownerTransformer != null && ownerTransformer.pathMapCaching) {
            EventHandlerList cachedResult = patchMatchCache.get(path.toString());
            if (cachedResult != null) {
                return cachedResult;
            }
        }
        // create a set for the found holders and populate it...
        Set<EventHandlerHolder> holders = new HashSet<>();
        getHandlersForPathItem(holders, path, path.size() - 1);
        // return the actual event handlers sorted by priority...
        EventHandlerList result = new EventHandlerList(holders).ensurePrioritySorted();
        if (ownerTransformer != null && ownerTransformer.pathMapCaching) {
            patchMatchCache.put(path.toString(), result);
        }
        return result;
    }

    /**
     * Recursively walk path to find matching handlers
     *
     * @param holders the list of holders found that match (to be populated)
     * @param path the path list
     * @param index the current index in the path
     */
    private void getHandlersForPathItem(Set<EventHandlerHolder> holders, List<QName> path, int index) {
        QName currentPart = path.get(index);
        EventHandlerPathMap currentMap;
        // look for exact matches...
        if (pathMap.containsKey(currentPart)) {
            currentMap = pathMap.get(currentPart);
            if (currentMap.mappedHere != null) {
                if (currentMap.mappedHere instanceof EventHandlerHolder) {
                    holders.add((EventHandlerHolder) currentMap.mappedHere);
                } else {
                    holders.addAll((EventHandlerList) currentMap.mappedHere);
                }
            }
            if (index > 0) {
                currentMap.getHandlersForPathItem(holders, path, index - 1);
            }
        }
        // if we're not at the root of the path - look for wildcards...
        if (index > 0) {
            // look for any namespace + wildcard matches...
            currentPart = new QName(currentPart.getNamespaceURI(), wildcard);
            if (pathMap.containsKey(currentPart)) {
                currentMap = pathMap.get(currentPart);
                if (currentMap.mappedHere != null) {
                    if (currentMap.mappedHere instanceof EventHandlerHolder) {
                        holders.add((EventHandlerHolder) currentMap.mappedHere);
                    } else {
                        holders.addAll((EventHandlerList) currentMap.mappedHere);
                    }
                }
                // recursive...
                currentMap.getHandlersForPathItem(holders, path, index - 1);
            }
            // look for any total wildcards...
            currentPart = new QName(wildcard);
            if (pathMap.containsKey(currentPart)) {
                currentMap = pathMap.get(currentPart);
                if (currentMap.mappedHere != null) {
                    if (currentMap.mappedHere instanceof EventHandlerHolder) {
                        holders.add((EventHandlerHolder) currentMap.mappedHere);
                    } else {
                        holders.addAll((EventHandlerList) currentMap.mappedHere);
                    }
                }
                // recursive...
                currentMap.getHandlersForPathItem(holders, path, index - 1);
            }
        }
    }

}
