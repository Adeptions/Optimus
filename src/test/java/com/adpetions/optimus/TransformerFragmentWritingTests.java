package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.namespaces.TransformNamespaceContext;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TransformerFragmentWritingTests {
    @Test
    public void testSimpleFragmentWrite() {
        try {
            String inputXml = "<root><aaa>ORIGINAL AAA TEXT</aaa><bbb>ORIGINAL BBB TEXT</bbb></root>";
            String fragmentXml = "<bbb>NEW BBB TEXT</bbb>";
            Transformer transformer = new Transformer(inputXml);
            transformer.registerStartElementHandler("aaa", (context, cargo, writer) -> {
                writer.writeFragment(fragmentXml);
                return ContinueState.SKIP_THIS_AND_DESCENDANTS;
            });
            String resultXml = transformer.transform();
            assertEquals("<root><bbb>NEW BBB TEXT</bbb><bbb>ORIGINAL BBB TEXT</bbb></root>", resultXml);
        } catch (TransformException e) {
            fail("Unexpected TransformException: " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException: " + e);
        }
    }

    @Test
    public void testNamespaceFragmentWrite1() {
        try {
            String defaultNsUri = "urn:some-ns";
            String inputXml = "<root xmlns='" + defaultNsUri + "'><aaa>ORIGINAL AAA TEXT</aaa><bbb>ORIGINAL BBB TEXT</bbb></root>";
            String fragmentXml = "<bbb>NEW BBB TEXT</bbb>";
            TransformNamespaceContext nsContext = new TransformNamespaceContext(defaultNsUri);
            Transformer transformer = new Transformer(inputXml, nsContext);
            transformer.registerStartElementHandler("/root/aaa", (context, cargo, writer) -> {
                writer.writeFragment(fragmentXml);
                return ContinueState.SKIP_THIS_AND_DESCENDANTS;
            });
            String resultXml = transformer.transform();
            assertEquals("<root xmlns=\"urn:some-ns\"><bbb>NEW BBB TEXT</bbb><bbb>ORIGINAL BBB TEXT</bbb></root>", resultXml);
        } catch (TransformException e) {
            fail("Unexpected TransformException: " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException: " + e);
        }
    }

    @Test
    public void testNamespaceFragmentWrite2() {
        try {
            String defaultNsUri = "urn:some-ns";
            String inputXml = "<root><aaa>ORIGINAL AAA TEXT</aaa><bbb>ORIGINAL BBB TEXT</bbb></root>";
            String fragmentXml = "<bbb xmlns='" + defaultNsUri + "'>NEW BBB TEXT</bbb>";
            Transformer transformer = new Transformer(inputXml);
            transformer.registerStartElementHandler("/root/aaa", (context, cargo, writer) -> {
                writer.writeFragment(fragmentXml);
                return ContinueState.SKIP_THIS_AND_DESCENDANTS;
            });
            String resultXml = transformer.transform();
            assertEquals("<root><bbb xmlns=\"urn:some-ns\">NEW BBB TEXT</bbb><bbb>ORIGINAL BBB TEXT</bbb></root>", resultXml);
        } catch (TransformException e) {
            fail("Unexpected TransformException: " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException: " + e);
        }
    }

    @Test
    public void testNamespaceFragmentWrite3() {
        try {
            String defaultNsUri = "urn:some-ns";
            String anotherNsUri = "urn:some-other-ns";
            String inputXml = "<root xmlns='" + defaultNsUri + "'><aaa>ORIGINAL AAA TEXT</aaa><bbb>ORIGINAL BBB TEXT</bbb></root>";
            String fragmentXml = "<bbb xmlns='" + anotherNsUri + "'>NEW BBB TEXT</bbb>";
            TransformNamespaceContext nsContext = new TransformNamespaceContext(defaultNsUri);
            Transformer transformer = new Transformer(inputXml, nsContext);
            transformer.registerStartElementHandler("/root/aaa", (context, cargo, writer) -> {
                writer.writeFragment(fragmentXml);
                return ContinueState.SKIP_THIS_AND_DESCENDANTS;
            });
            String resultXml = transformer.transform();
            assertEquals("<root xmlns=\"urn:some-ns\"><bbb xmlns=\"urn:some-other-ns\">NEW BBB TEXT</bbb><bbb>ORIGINAL BBB TEXT</bbb></root>", resultXml);
        } catch (TransformException e) {
            fail("Unexpected TransformException: " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException: " + e);
        }
    }

    @Test
    public void testNamespaceFragmentWrite4() {
        try {
            String defaultNsUri = "urn:some-ns";
            String anotherNsUri = "urn:some-other-ns";
            String inputXml = "<foo:root xmlns:foo='" + defaultNsUri + "'><foo:aaa>ORIGINAL AAA TEXT</foo:aaa><foo:bbb>ORIGINAL BBB TEXT</foo:bbb></foo:root>";
            String fragmentXml = "<foo:bbb xmlns:foo='" + anotherNsUri + "'>NEW BBB TEXT</foo:bbb>";
            TransformNamespaceContext nsContext = new TransformNamespaceContext(defaultNsUri);
            Transformer transformer = new Transformer(inputXml, nsContext);
            transformer.registerStartElementHandler("/root/aaa", (context, cargo, writer) -> {
                writer.writeFragment(fragmentXml);
                return ContinueState.SKIP_THIS_AND_DESCENDANTS;
            });
            String resultXml = transformer.transform();
            assertEquals("<foo:root xmlns:foo=\"urn:some-ns\"><foo:bbb xmlns:foo=\"urn:some-other-ns\">NEW BBB TEXT</foo:bbb><foo:bbb>ORIGINAL BBB TEXT</foo:bbb></foo:root>", resultXml);
        } catch (TransformException e) {
            fail("Unexpected TransformException: " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException: " + e);
        }
    }
}