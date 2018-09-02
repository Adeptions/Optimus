package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;

import javax.xml.stream.XMLStreamException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provides a holder wrapper around either an event handler (lambda/interface method) or
 * template method (which can be invoked)
 *
 * <p>Provides a normalized calling method to the handler which deals with the different way
 * of calling depending on whether its a lambda or method to be invoked.</p>
 */
class EventHandlerHolder {
    private enum HandlerType {
        HANDLER,
        TEMPLATE_METHOD
    }

    HandlerType handlerType;
    int priority = 0;
    String matchPath;
    EventHandler handler;
    Object ownerObject;
    Method method;

    EventHandlerHolder(String matchPath, int priority, EventHandler handler) {
        this.matchPath = matchPath;
        this.priority = priority;
        this.handlerType = HandlerType.HANDLER;
        this.handler = handler;
    }

    EventHandlerHolder(String matchPath, int priority, Method method, Object ownerObject) {
        this.matchPath = matchPath;
        this.priority = priority;
        this.handlerType = HandlerType.TEMPLATE_METHOD;
        this.ownerObject = ownerObject;
        this.method = method;
    }

    ContinueState call(TransformContext context) throws TransformException, XMLStreamException {
        ContinueState result = ContinueState.CONTINUE;
        if (handlerType == HandlerType.HANDLER) {
            result = handler.handle(context, context.transformer.cargo, context.transformer.xmlWriter);
        } else {
            try {
                result = (ContinueState) method.invoke(ownerObject);
            } catch (IllegalAccessException e) {
                throw new TransformException("Illegal access exception calling method '" + method.getName() + "'", e);
            } catch (InvocationTargetException e) {
                throw new TransformException("Invocation target exception in method '" + method.getName() + "'", e);
            }
        }
        return result;
    }
}
