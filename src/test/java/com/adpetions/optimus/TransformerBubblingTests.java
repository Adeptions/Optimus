package com.adpetions.optimus;

import com.adpetions.optimus.templates.BubblingTestTransformTemplate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TransformerBubblingTests {
    @Test
    public void testOptimusCancelBubble() {
        try {
            StringBuilder resultBuilder = new StringBuilder();
            Transformer<StringBuilder> transformer = new Transformer("<root></root>");
            transformer.setCargo(resultBuilder);
            transformer.registerHandler(EventType.START_DOCUMENT, 30, (context, cargo, writer) -> {
                cargo.append("[1]");
                return null;
            });
            transformer.registerHandler(EventType.START_DOCUMENT, 20, (context, cargo, writer) -> {
                cargo.append("[2]");
                context.cancelNext();
                return null;
            });
            transformer.registerHandler(EventType.START_DOCUMENT, 10, (context, cargo, writer) -> {
                cargo.append("[3]");
                context.callNext();
                return null;
            });
            transformer.nullTransform();
            assertEquals("[1][2]", resultBuilder.toString());
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testOptimusCallNext() {
        try {
            StringBuilder resultBuilder = new StringBuilder();
            Transformer<StringBuilder> transformer = new Transformer<>("<root></root>");
            transformer.setCargo(resultBuilder);
            transformer.registerHandler(EventType.START_DOCUMENT, 30, (context, cargo, writer) -> {
                cargo.append("[1]");
                return null;
            });
            transformer.registerHandler(EventType.START_DOCUMENT, 20, (context, cargo, writer) -> {
                cargo.append("[3]");
                context.callNext();
                return null;
            });
            transformer.registerHandler(EventType.START_DOCUMENT, 10, (context, cargo, writer) -> {
                cargo.append("[2]");
                context.cancelNext();
                return null;
            });
            transformer.nullTransform();
            assertEquals("[1][3][2]", resultBuilder.toString());
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testOptimusCallNext2() {
        try {
            StringBuilder resultBuilder = new StringBuilder();
            Transformer<StringBuilder> transformer = new Transformer<>("<root></root>");
            transformer.setCargo(resultBuilder);
            transformer.registerHandler(EventType.START_DOCUMENT, 30, (context, cargo, writer) -> {
                cargo.append("[3]");
                context.callNext();
                return null;
            });
            transformer.registerHandler(EventType.START_DOCUMENT, 20, (context, cargo, writer) -> {
                cargo.append("[2]");
                context.cancelNext();
                return null;
            });
            transformer.registerHandler(EventType.START_DOCUMENT, 10, (context, cargo, writer) -> {
                cargo.append("[1]");
                return null;
            });
            transformer.nullTransform();
            assertEquals("[3][2]", resultBuilder.toString());
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testOptimusCancelBubbleTemplate() {
        try {
            Transformer<StringBuilder> transformer = new Transformer<>("<root><blah foo=\"bar\">HERE IS SOME TEXT</blah></root>");
            BubblingTestTransformTemplate template = new BubblingTestTransformTemplate();
            transformer.nullTransform(template);

            assertEquals("[1][2]", template.getResult());
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

}
