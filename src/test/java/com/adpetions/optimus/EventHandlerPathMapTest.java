package com.adpetions.optimus;

import com.adpetions.optimus.exceptions.BadMatchPathException;
import com.adpetions.optimus.exceptions.TransformException;
import com.adpetions.optimus.namespaces.TransformNamespaceContext;
import com.adpetions.optimus.templates.MyInheritedTransformTemplate;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EventHandlerPathMapTest {
    // define some namespace prefixes and URIs...
    private static String uriDefault = "urn:my-default-namespace";
    private static String uriFoo = "http://www.foo.com";
    private static String uriBar = "http://www.bar.com";
    private static String prefixDef = "def";
    private static String prefixFoo = "foo";
    private static String prefixBar = "bar";

    @Test
    public void testBasicPathMatching() {
        try {
            List<EventHandlerHolder> holders;
            // create namespace context...
            TransformNamespaceContext nsContext = new TransformNamespaceContext();
            nsContext.setDefaultNamespaceURI(uriDefault);
            nsContext.addNamespace(prefixFoo, uriFoo);
            nsContext.addNamespace(prefixBar, uriBar);
            nsContext.addNamespace(prefixDef, uriDefault);
            // create our path mapper...
            Transformer transformer = new Transformer("");
            transformer.setPathMapCaching(false);
            EventHandlerPathMap pathMap = new EventHandlerPathMap(transformer, nsContext, EventHandlerPathMap.NodeType.ELEMENT);
            // and register some handlers...
            pathMap.add(new EventHandlerHolder(prefixBar + ":*", 1, testHandler1));
            pathMap.add(new EventHandlerHolder(prefixFoo + ":*", 5, testHandler2));
            pathMap.add(new EventHandlerHolder(prefixDef + ":*", 10, testHandler3));
            pathMap.add(new EventHandlerHolder("*", 15, testHandler4));

            // create a QName path to match against...
            List<QName> testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));

            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on 'def:*' and one for match on '*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals(testHandler4, holders.get(0).handler);
            assertEquals(testHandler3, holders.get(1).handler);

            // add another item to path and get the handlers...
            testMatchPath.add(new QName(uriFoo, "item"));
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on 'foo:*' and one for match on '*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals(testHandler4, holders.get(0).handler);
            assertEquals(testHandler2, holders.get(1).handler);

            // add another item to path and get the handlers...
            testMatchPath.add(new QName(uriBar, "item"));
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on 'bar:*' and one for match on '*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals(testHandler4, holders.get(0).handler);
            assertEquals(testHandler1, holders.get(1).handler);

        } catch (BadMatchPathException ex) {
            fail("Unexpected Transformer exception - Error: " + ex.getMessage());
        } catch (Exception ex) {
            fail("Unexpected exception - Error: " + ex.getMessage());
        }
    }

    @Test
    public void testAttributeMatching() {
        try {
            List<EventHandlerHolder> holders;
            // create namespace context...
            TransformNamespaceContext nsContext = new TransformNamespaceContext();
            nsContext.setDefaultNamespaceURI(uriDefault);
            nsContext.addNamespace(prefixFoo, uriFoo);
            nsContext.addNamespace(prefixBar, uriBar);
            nsContext.addNamespace(prefixDef, uriDefault);
            // create our path mapper...
            Transformer transformer = new Transformer("");
            transformer.setPathMapCaching(false);
            EventHandlerPathMap pathMap = new EventHandlerPathMap(transformer, nsContext, EventHandlerPathMap.NodeType.ATTRIBUTE);
            // and register some handlers...
            pathMap.add(new EventHandlerHolder("@*", 10, testHandler1));
            pathMap.add(new EventHandlerHolder("@foo:*", 9, testHandler2));
            pathMap.add(new EventHandlerHolder("/root/@*", 8, testHandler3));
            pathMap.add(new EventHandlerHolder("foo:*/@*", 7, testHandler4));

            // create a QName path to match against and just add a root...
            List<QName> testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName("@att1"));
            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on '/root/@*' and one for match on '@*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals(testHandler1, holders.get(0).handler);
            assertEquals(testHandler3, holders.get(1).handler);

            // create a QName path to match against...
            testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName(uriFoo, "elem"));
            testMatchPath.add(new QName("@att1"));
            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on 'foo:*/@*' and one for match on '@*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals(testHandler1, holders.get(0).handler);
            assertEquals(testHandler4, holders.get(1).handler);

            // create a QName path to match against...
            testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName(uriFoo, "@att1"));
            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should find 3 handlers - one for match on '@*', one for '@foo:*' and one for match on '/root/@*'...
            assertEquals(3, holders.size());
            // and check they were in correct priority order...
            assertEquals(testHandler1, holders.get(0).handler);
            assertEquals(testHandler2, holders.get(1).handler);
            assertEquals(testHandler3, holders.get(2).handler);

            // create a QName path to match against...
            testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName(uriFoo, "@att1"));
            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should only find 2 handlers - one for match on '@*' and one for '@foo:*' (but we shouldn't match '/root/@*')...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals(testHandler1, holders.get(0).handler);
            assertEquals(testHandler2, holders.get(1).handler);

        } catch (BadMatchPathException ex) {
            fail("Unexpected Transformer exception - Error: " + ex.getMessage());
        } catch (Exception ex) {
            fail("Unexpected exception - Error: " + ex.getMessage());
        }
    }

    @Test
    public void testAttributeMatching2() {
        try {
            List<EventHandlerHolder> holders;
            // create namespace context...
            TransformNamespaceContext nsContext = new TransformNamespaceContext();
            nsContext.setDefaultNamespaceURI(uriFoo);
            nsContext.addNamespace(prefixFoo, uriFoo);
            // create our path mapper...
            Transformer transformer = new Transformer("");
            transformer.setPathMapCaching(false);
            EventHandlerPathMap pathMap = new EventHandlerPathMap(transformer, nsContext, EventHandlerPathMap.NodeType.ATTRIBUTE);
            // register handlers...
            pathMap.add(new EventHandlerHolder("root/@foo:att1", 10, testHandler1));

            // create a QName path to match against and just add a root...
            List<QName> testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriFoo, "root"));
            testMatchPath.add(new QName(uriFoo, "@att1"));
            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should find 1 handler...
            assertEquals(1, holders.size());
            // and check they were in correct priority order...
            assertEquals(testHandler1, holders.get(0).handler);

        } catch (BadMatchPathException ex) {
            fail("Unexpected Transformer exception - Error: " + ex.getMessage());
        } catch (Exception ex) {
            fail("Unexpected exception - Error: " + ex.getMessage());
        }
    }

    @Test
    public void testRegisteringFromTemplate() {
        try {
            List<EventHandlerHolder> holders;
            //MyTestTransformTemplate template = new MyTestTransformTemplate();
            MyInheritedTransformTemplate template = new MyInheritedTransformTemplate();
            Transformer transformer = new Transformer("<root/>");
            // build template...
            transformer.buildTemplating(template);

            // ELEMENTS...
            // grab the registered element handlers from Transformer...
            EventHandlerPathMap pathMap = transformer.startElementHandlers;

            // create a QName path to match against...
            List<QName> testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));

            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on 'def:*' and one for match on '*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals("handleAllElements", holders.get(0).method.getName());
            assertEquals("handleAllDefaultNsElements", holders.get(1).method.getName());

            // add another item to path and get the handlers...
            testMatchPath.add(new QName(uriFoo, "item"));
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on 'foo:*' and one for match on '*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals("handleAllElements", holders.get(0).method.getName());
            assertEquals("handleFooElements", holders.get(1).method.getName());

            // add another item to path and get the handlers...
            testMatchPath.add(new QName(uriBar, "item"));
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on 'bar:*' and one for match on '*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals("handleAllElements", holders.get(0).method.getName());
            assertEquals("handleBarElements", holders.get(1).method.getName());

            // ATTRIBUTES...
            // grab the registered attribute handlers from Transformer...
            pathMap = transformer.attributeHandlers;
            // create a QName path to match against...
            testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName(uriFoo, "elem"));
            testMatchPath.add(new QName("@att1"));
            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should find 2 handlers - one for match on 'foo:*/@*' and one for match on '@*'...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals("handleAttributes", holders.get(0).method.getName());
            assertEquals("handleAttributesOnFooElements", holders.get(1).method.getName());

            // create a QName path to match against...
            testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName(uriFoo, "@att1"));
            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should find 3 handlers - one for match on '@*', one for '@foo:*' and one for match on '/root/@*'...
            assertEquals(3, holders.size());
            // and check they were in correct priority order...
            assertEquals("handleAttributes", holders.get(0).method.getName());
            assertEquals("handleFooAttributes", holders.get(1).method.getName());
            assertEquals("handleRootAttributes", holders.get(2).method.getName());

            // create a QName path to match against...
            testMatchPath = new ArrayList<>();
            testMatchPath.add(new QName(null, "/")); // simulate - document root is always the start of path!
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName(uriDefault, "root"));
            testMatchPath.add(new QName(uriFoo, "@att1"));
            // get handlers mapped for path...
            holders = pathMap.getHolders(testMatchPath);
            // we should only find 2 handlers - one for match on '@*' and one for '@foo:*' (but we shouldn't match '/root/@*')...
            assertEquals(2, holders.size());
            // and check they were in correct priority order...
            assertEquals("handleAttributes", holders.get(0).method.getName());
            assertEquals("handleFooAttributes", holders.get(1).method.getName());

        } catch (Exception ex) {
            fail("Unexpected exception - Error: " + ex.getMessage());
        }
    }

    private static final EventHandler testHandler1 = (context, cargo, writer) -> null;
    private static final EventHandler testHandler2 = (context, cargo, writer) -> null;
    private static final EventHandler testHandler3 = (context, cargo, writer) -> null;
    private static final EventHandler testHandler4 = (context, cargo, writer) -> null;
}
