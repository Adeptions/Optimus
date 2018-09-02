package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.io.*;

import static org.junit.Assert.*;

public class TransformerIdentityTransformTests {
    private static final String testXml =
            "<root>" +
            "<xxx:yyy xmlns:xxx='http://www.xxx.com'/>" +
            "<aaa id='1'>" +
            "<bbb id='1.1'>" +
            "<ccc id='1.1.1'/>" +
            "</bbb>" +
            "</aaa>" +
            "<aaa id='2'/>" +
            "<bbb id='3'>" +
            "<ccc id='3.1'/>" +
            "</bbb>" +
            "<ccc id='4'/>" +
            "</root>";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testOptimusIdentityTransform1() {
        String compareOutput = "<root><xxx:yyy xmlns:xxx=\"http://www.xxx.com\"/><aaa id=\"1\"><bbb id=\"1.1\"><ccc id=\"1.1.1\"/></bbb></aaa><aaa id=\"2\"/><bbb id=\"3\"><ccc id=\"3.1\"/></bbb><ccc id=\"4\"/></root>";
        Transformer transformer = new Transformer(testXml);
        transformer.setOmitXmlDeclaration(true);
        try {
            String outputXml = transformer.transform();
            // not the greatest way to test the result :(...
            assertEquals(compareOutput, outputXml);
        } catch (TransformException e) {
            fail("Unexpected TransformException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

    @Test
    public void testOptimusIdentityTransform2() {
        try {
            Transformer transformer = new Transformer(testXml);
            transformer.setOmitXmlDeclaration(false); // we want an xml decl in files!
            Writer writer = new OutputStreamWriter(new FileOutputStream(testFolder.newFile("out.xml")), "UTF-8");
            transformer.transform(writer);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

    @Test
    public void testOptimusIdentityTransform4() {
        try {
            Transformer transformer = new Transformer("<root><foo><bar>text node</bar></foo></root>");
            transformer.registerStartElementHandler("foo", (context, cargo, writer) -> {
                context.setOverrideName(new QName("baz"));
                return null;
            });
            String outputXml = transformer.transform();
            assertEquals("<root><baz><bar>text node</bar></baz></root>", outputXml);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

    @Test
    public void testOptimusIdentityTransform5() {
        try {
            Transformer transformer = new Transformer("<root><foo><bar>text node</bar></foo></root>");
            transformer.registerStartElementHandler("foo", (context, cargo, writer) -> ContinueState.SKIP_THIS);
            String outputXml = transformer.transform();
            assertEquals("<root><bar>text node</bar></root>", outputXml);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

    @Test
    public void testOptimusIdentityTransform6() {
        try {
            Transformer transformer = new Transformer("<root><foo><bar>text node</bar></foo></root>");
            transformer.registerStartElementHandler("foo", (context, cargo, writer) -> ContinueState.SKIP_THIS_AND_DESCENDANTS);
            String outputXml = transformer.transform();
            assertEquals("<root/>", outputXml);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

    @Test
    public void testOptimusIdentityTransform7() {
        try {
            Transformer transformer = new Transformer("<root><foo><bar>text node</bar></foo></root>");
            transformer.registerStartElementHandler("foo", (context, cargo, writer) -> {
                writer.writeStartElement("baz"); // wrap start before <foo> gets written
                return null;
            });
            transformer.registerEndElementHandler("foo", (context, cargo, writer) -> {
                writer.writeEndElement(); // end the <foo>
                writer.writeEndElement(); // end our new <baz> element
                // tell transformer that we've done what needs to be done...
                return ContinueState.HANDLED;
            });
            String outputXml = transformer.transform();
            assertEquals("<root><baz><foo><bar>text node</bar></foo></baz></root>", outputXml);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

    @Test
    public void testOptimusIdentityTransform8() {
        try {
            Transformer transformer = new Transformer("<root><foo><bar>text node</bar></foo></root>");
            transformer.registerAfterStartElementHandler("foo", (context, cargo, writer) -> {
                writer.writeStartElement("baz"); // wrap start before <foo> gets written
                return null;
            });
            transformer.registerEndElementHandler("foo", (context, cargo, writer) -> {
                writer.writeEndElement(); // end our new <baz> element
                return ContinueState.CONTINUE; // let transformer end the <foo> element as normal
            });
            String outputXml = transformer.transform();
            assertEquals("<root><foo><baz><bar>text node</bar></baz></foo></root>", outputXml);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

    @Test
    public void testOptimusIdentityTransform9() {
        try {
            class CounterHolder {
                Long counter = 0L;
            }
            Transformer<CounterHolder> transformer = new Transformer<>("<root><foo><bar>text node</bar></foo></root>");
            transformer.setCargo(new CounterHolder());
            transformer.registerBeforeAttributesHandler("*", (context, cargo, writer) -> {
                cargo.counter++;
                writer.writeAttribute("id", cargo.counter.toString());
                return null;
            });
            String outputXml = transformer.transform();
            assertEquals("<root id=\"1\"><foo id=\"2\"><bar id=\"3\">text node</bar></foo></root>", outputXml);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

    @Test
    public void testOptimusIdentityTransform10() {
        try {
            class CounterHolder {
                Long counter = 0L;
            }
            Transformer<CounterHolder> transformer = new Transformer<>("<root id='x'><foo><bar>text node</bar></foo></root>");
            transformer.setCargo(new CounterHolder());
            transformer.registerBeforeAttributesHandler("*", (context, cargo, writer) -> {
                cargo.counter++;
                writer.writeAttribute("id", cargo.counter.toString());
                return null;
            });
            transformer.registerAttributeHandler("@id", (context, cargo, writer) -> {
                // suppress any existing @id attrtibutes...
                return ContinueState.SKIP_THIS;
            });
            String outputXml = transformer.transform();
            assertEquals("<root id=\"1\"><foo id=\"2\"><bar id=\"3\">text node</bar></foo></root>", outputXml);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e);
        } catch (XMLStreamException e) {
            fail("Unexpected XMLStreamException : " + e);
        }
    }

}
