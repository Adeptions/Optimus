package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EventHandlingTests {
    private static final String testXml =
            "<?pi-in-doc blah ?>" +
            "<?pi-in-doc blah blah ?>" +
            "<!-- some comment in document root -->" +
            "<!-- another comment in document root -->" +
            "<root>" +
            "  <xxx:yyy xmlns:xxx='urn:xxx'/>" +
            "  <aaa id='1'>" +
            "    <bbb id='1.1'>" +
            "      <ccc id='1.1.1'/>" +
            "    </bbb>" +
            "  </aaa>" +
            "  <aaa id='2'/>" +
            "  <bbb id='3'>" +
            "    <ccc id='3.1'/>" +
            "  </bbb>" +
            "  <ccc id='4'/>" +
            "  <ccc id='5'/>" +
            "<!-- some comment in root element -->" +
            "<!-- another comment in root element -->" +
            "<?some-pi blah blah blah ?>" +
            "<?some-pi blah blah blah blah ?>" +
            "SOME TEXT IN ROOT" +
            "<![CDATA[SOME MORE TEXT IN ROOT]]>" +
            "</root>";

    @Test
    public void testOptimusBasicHandlerMatching1() {
        try {
            StringBuilder resultBuilder = new StringBuilder();
            Transformer<StringBuilder> transformer = new Transformer(testXml);
            transformer.setCargo(resultBuilder);
            transformer.registerStartElementHandler("aaa", (context, cargo, writer) -> {
                cargo.append("[aaa]");
                return null; }
            );
            transformer.registerStartElementHandler("bbb", (context, cargo, writer) -> {
                cargo.append("[bbb]");
                return null; }
            );
            transformer.registerStartElementHandler("ccc", (context, cargo, writer) -> {
                cargo.append("[ccc]");
                return null; });
            transformer.nullTransform();
            assertEquals("[aaa][bbb][ccc][aaa][bbb][ccc][ccc][ccc]", resultBuilder.toString());
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testOptimusBasicHandlerMatching2() {
        try {
            StringBuilder resultBuilder = new StringBuilder();
            Transformer<StringBuilder> transformer = new Transformer(testXml);
            transformer.setCargo(resultBuilder);
            transformer.registerStartElementHandler("aaa", (context, cargo, writer) -> {
                        cargo.append("[aaa]");
                        return null;
                    }
            );
            transformer.registerStartElementHandler("aaa/bbb", (context, cargo, writer) -> {
                        cargo.append("[bbb]");
                        return null; }
            );
            transformer.registerStartElementHandler("bbb/ccc", (context, cargo, writer) -> {
                        cargo.append("[ccc]");
                        return null; }
            );
            transformer.nullTransform();
            assertEquals("[aaa][bbb][ccc][aaa][ccc]", resultBuilder.toString());
        } catch (Exception ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    @Test
    public void testOptimusPathTests1() {
        try {
            StringBuilder resultBuilder = new StringBuilder();
            Transformer<StringBuilder> transformer = new Transformer(testXml);
            transformer.setCargo(resultBuilder);
            EventHandler<StringBuilder> lambda = (context, cargo, writer) -> {
                int depth = context.getPathDepth();
                for (int i = depth - 1; i >= 0; i--) {
                    cargo.append("/").append(context.getAncestorName(i).getLocalPart()).append("[").append(context.getAncestorIndex(i)).append("]");
                }
                cargo.append("\n");
                return null;
            };
            transformer.registerStartElementHandler(lambda);
            transformer.registerAttributeHandler(lambda);
            transformer.registerCharactersHandler(lambda);
            transformer.registerWhitespaceHandler(lambda);
            transformer.registerCdataHandler(lambda);
            transformer.registerCommentHandler(lambda);
            transformer.registerProcessingInstructionHandler(lambda);
            transformer.nullTransform();
            String expectedResult = "/?pi-in-doc[1]\n" +
                    "/?pi-in-doc[2]\n" +
                    "/!comment()[1]\n" +
                    "/!comment()[2]\n" +
                    "/root[1]\n" +
                    "/root[1]/#text()[1]\n" +
                    "/root[1]/yyy[1]\n" +
                    "/root[1]/#text()[2]\n" +
                    "/root[1]/aaa[1]\n" +
                    "/root[1]/aaa[1]/@id[1]\n" +
                    "/root[1]/aaa[1]/#text()[1]\n" +
                    "/root[1]/aaa[1]/bbb[1]\n" +
                    "/root[1]/aaa[1]/bbb[1]/@id[1]\n" +
                    "/root[1]/aaa[1]/bbb[1]/#text()[1]\n" +
                    "/root[1]/aaa[1]/bbb[1]/ccc[1]\n" +
                    "/root[1]/aaa[1]/bbb[1]/ccc[1]/@id[1]\n" +
                    "/root[1]/aaa[1]/bbb[1]/#text()[2]\n" +
                    "/root[1]/aaa[1]/#text()[2]\n" +
                    "/root[1]/#text()[3]\n" +
                    "/root[1]/aaa[2]\n" +
                    "/root[1]/aaa[2]/@id[1]\n" +
                    "/root[1]/#text()[4]\n" +
                    "/root[1]/bbb[1]\n" +
                    "/root[1]/bbb[1]/@id[1]\n" +
                    "/root[1]/bbb[1]/#text()[1]\n" +
                    "/root[1]/bbb[1]/ccc[1]\n" +
                    "/root[1]/bbb[1]/ccc[1]/@id[1]\n" +
                    "/root[1]/bbb[1]/#text()[2]\n" +
                    "/root[1]/#text()[5]\n" +
                    "/root[1]/ccc[1]\n" +
                    "/root[1]/ccc[1]/@id[1]\n" +
                    "/root[1]/#text()[6]\n" +
                    "/root[1]/ccc[2]\n" +
                    "/root[1]/ccc[2]/@id[1]\n" +
                    "/root[1]/!comment()[1]\n" +
                    "/root[1]/!comment()[2]\n" +
                    "/root[1]/?some-pi[1]\n" +
                    "/root[1]/?some-pi[2]\n" +
                    "/root[1]/#text()[7]\n" +
                    "/root[1]/#text()[8]\n";
            assertEquals(expectedResult, resultBuilder.toString());
        } catch (TransformException e) {
            fail("Unexpected TransformException: " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException: " + e);
        }
    }

    @Test
    public void testOptimusPathTests2() {
        try {
            StringBuilder resultBuilder = new StringBuilder();
            Transformer<StringBuilder> transformer = new Transformer(testXml);
            transformer.setCargo(resultBuilder);
            EventHandler<StringBuilder> lambda = (context, cargo, writer) -> {
                int depth = context.getPathDepth();
                for (int i = depth - 1; i >= 0; i--) {
                    cargo.append("/").append(context.getAncestorName(i).getLocalPart()).append("[").append(context.getAncestorIndex(i)).append("]");
                }
                cargo.append("\n");
                return null;
            };
            transformer.registerProcessingInstructionHandler(lambda);
            transformer.nullTransform();
            String expectedResult = "/?pi-in-doc[1]\n" +
                    "/?pi-in-doc[2]\n" +
                    "/root[1]/?some-pi[1]\n" +
                    "/root[1]/?some-pi[2]\n";
            assertEquals(expectedResult, resultBuilder.toString());
        } catch (TransformException e) {
            fail("Unexpected TransformException: " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException: " + e);
        }
    }
}
