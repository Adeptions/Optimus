package com.adpetions.optimus;

/**
 * Enum for values returned from OptimusEventHandler.handle() methods
 */
public enum ContinueState {
    CONTINUE, // continue (same as null) - perform default action
    SKIP_THIS, // skip this node
    SKIP_THIS_AND_DESCENDANTS, // skip this node and its descendants
    HANDLED, // don't perform default action - the handler has done any required output
    QUIT // quite the transform (only to be used on null transforms)
}
