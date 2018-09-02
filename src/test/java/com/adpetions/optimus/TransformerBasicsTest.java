package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.nodes.Element;
import com.adpetions.optimus.nodes.NodeCollection;
import com.adpetions.optimus.nodes.WriterNode;
import com.adpetions.optimus.writers.TransformSimpleWriter;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static com.adpetions.optimus.writers.TransformSimpleWriter.*;

public class TransformerBasicsTest {

    @Test
    public void testOptimusConstructors() {
        try {
            Transformer transformer = new Transformer("<root/>");
            transformer.nullTransform();
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testOptimusNullTransform() {
        try {
            String inputXml = "<root><foo>some text</foo><foo>some more text</foo><foo>yet more text</foo><bar>not this though</bar></root>";
            StringBuilder textCollector = new StringBuilder();
            Transformer<StringBuilder> transformer = new Transformer<>(inputXml);
            transformer.setCargo(textCollector);
            transformer.registerCharactersHandler("foo/*", (context, cargo, writer) -> {
                cargo.append(context.getText()).append("\n");
                return null;
            });
            transformer.nullTransform();
            assertEquals("some text\nsome more text\nyet more text\n", textCollector.toString());        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testSkipping() {
        try {
            String inputXml = "<root foo='a'><!-- test --><root2>blah blah</root2></root>";
            Transformer transformer = new Transformer(inputXml);
            transformer.registerStartElementHandler("/root", (context, cargo, writer) -> ContinueState.SKIP_THIS);
            String outputXml = transformer.transform();
            assertEquals("<!-- test --><root2>blah blah</root2>", outputXml);
        } catch (TransformException | XMLStreamException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testOptimusNewWriting() {
        try {
            // old way...
            TransformSimpleWriter writer = new TransformSimpleWriter();
            writer.writeStartElement("root")
                    .writeAttribute("att", "foo")
                    .writeCharacters("bar")
                    .writeComment("buzz")
                    .writeEndElement();
            // new way...
            TransformSimpleWriter writer2 = new TransformSimpleWriter();
            writer2.write(
                    start("root"), att("att", "foo"),
                        text("bar"),
                        comment("buzz"),
                    end()
            );
            assertEquals(writer.getXmlString(), writer2.getXmlString());
        } catch (TransformException | XMLStreamException e) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testReadElement() {
        String inputXml = "<root><test att=\"foo\" xmlns:x=\"urn:xxx\">TEXT<!-- comment --><?pi bar?></test><test att=\"foo\"><foo att=\"1\">FOO-1<bar>...BAR...</bar>FOO-2</foo></test></root>";
        Transformer<Collector> transformer = new Transformer(inputXml);
        Collector collector = new Collector();
        transformer.setCargo(collector);
        try {
            transformer.registerStartElementHandler("test", (context, cargo, writer) -> {
                cargo.elements.add(context.readElement());
                return null;
            });
            transformer.registerEndElementHandler("root", (context, cargo, writer) -> {
                StringBuilder builder = new StringBuilder();
                List<QName> path = context.getPath();
                for (QName qname: path) {
                    builder.append("/").append(qname);
                }
                cargo.endRootPath = builder.toString();
                return null;
            });
            String outputXml = transformer.transform();
            // everything inside root should have been stripped because it was read...
            assertEquals("<root/>", outputXml);
            // we should have two elements collected...
            assertEquals(2, collector.elements.size());
            // check the size of the collected elements children...
            assertEquals(3, collector.elements.get(0).getChildNodes().size());
            assertEquals(1, collector.elements.get(1).getChildNodes().size());
            // check that getText/getAllText on elements is correct...
            assertEquals("TEXT", collector.elements.get(0).getText());
            assertEquals(null, collector.elements.get(1).getText());
            assertEquals("FOO-1...BAR...FOO-2", collector.elements.get(1).getAllText());
            // the path at the end of root should have been fixed up (and the event handler should have been called!)...
            assertEquals("///root", collector.endRootPath);
        } catch (TransformException | XMLStreamException e) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testReadElementAndApply() {
        String inputXml = "<root><test xmlns:x=\"urn:xxx\" att=\"foo\" att2=\"bar\">TEXT<!-- comment --><?pi bar?></test><test att=\"foo\"><foo att=\"1\">FOO-1<bar>...BAR...</bar>FOO-2</foo></test></root>";
        Transformer<Collector> transformer = new Transformer(inputXml);
        Collector collector = new Collector();
        transformer.setCargo(collector);
        try {
            transformer.registerStartElementHandler("test", (context, cargo, writer) -> {
                if (!context.isApplying()) {
                    Element element = context.readElement();
                    cargo.elements.add(element);
                    context.apply(new NodeCollection(element));
                }
                return null;
            });
            String outputXml = transformer.transform();
            // everything inside root should have been stripped because it was read...
            assertEquals(inputXml, outputXml);
            // we should have two elements collected...
            assertEquals(2, collector.elements.size());
            // check the size of the collected elements children...
            assertEquals(3, collector.elements.get(0).getChildNodes().size());
            assertEquals(1, collector.elements.get(1).getChildNodes().size());
        } catch (TransformException | XMLStreamException e) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testReadElementAndApply2() {
        String inputXml = "<root><test att=\"foo\" xmlns:x=\"urn:xxx\">TEXT<!-- comment --><?pi bar?></test><test att=\"foo\"><foo att=\"1\">FOO-1<bar>...BAR...</bar>FOO-2</foo></test></root>";
        Transformer transformer = new Transformer(inputXml);
        try {
            transformer.registerStartElementHandler("test", (context, cargo, writer) -> {
                if (!context.isApplying()) {
                    Element element = context.readElement();
                    context.apply(element.getChildNodes());
                }
                return null;
            });
            String outputXml = transformer.transform();
            // everything inside root should have been stripped because it was read...
            assertEquals("<root>TEXT<!-- comment --><?pi bar?><foo att=\"1\">FOO-1<bar>...BAR...</bar>FOO-2</foo></root>", outputXml);
        } catch (TransformException | XMLStreamException e) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testReadElementAndApply3() {
        String inputXml = "<root><test att=\"foo\" xmlns:x=\"urn:xxx\">TEXT<!-- comment --><?pi bar?></test><test att=\"foo\"><foo att=\"1\">FOO-1<bar>...BAR...</bar>FOO-2</foo></test></root>";
        Transformer transformer = new Transformer(inputXml);
        try {
            transformer.registerStartElementHandler("test", (context, cargo, writer) -> {
                if (!context.isApplying()) {
                    Element element = context.readElement();
                    NodeCollection textCollection = new NodeCollection();
                    for (WriterNode node: element.getChildNodes()) {
                        if (node.getNodeType() == WriterNode.NodeType.TEXT) {
                            textCollection.add(node);
                        }
                    }
                    context.apply(textCollection);
                }
                return null;
            });
            String outputXml = transformer.transform();
            // everything inside root should have been stripped because it was read...
            assertEquals("<root>TEXT</root>", outputXml);
        } catch (TransformException | XMLStreamException e) {
            fail("Unexpected exception");
        }
    }

    public class Collector {
        public List<Element> elements = new ArrayList<>();
        public String endRootPath;
    }

    @Test
    public void testOverrideAttributeWriting() {
        String inputXml = "<root><test att1='1' att2='2' att3='3'>TEXT</test></root>";
        try {
            Transformer transformer = new Transformer(inputXml);
            transformer.registerAttributeHandler("test/@*", (context, cargo, writer) -> {
                writer.writeAttribute("att", context.getAttributeValue());
                return null;
            });
            String outputXml = transformer.transform();
            // there should be now be 4 attributes - the @att should have a value of '3' as that would have been
            // the last value set for that attribute...
            assertEquals("<root><test att=\"3\" att1=\"1\" att2=\"2\" att3=\"3\">TEXT</test></root>", outputXml);
        } catch (TransformException | XMLStreamException e) {
            fail("Unexpected exception");
        }
    }
}