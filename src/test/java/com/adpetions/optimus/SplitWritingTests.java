package com.adpetions.optimus;

import com.adpetions.optimus.writers.TransformSimpleWriter;
import com.adpetions.optimus.writers.TransformXMLStreamWriter;
import org.junit.Test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SplitWritingTests {
    private static final String inputXml = "<root>" +
            "<foo><bar>text 1</bar></foo>" +
            "<foo><bar>text 2</bar></foo>" +
            "<foo><bar>text 3</bar></foo>" +
            "</root>";

    @Test
    public void testSplittingOutput() {
        try {
            class WritersHolder {
                List<StringWriter> stringWriters = new ArrayList<>();
                TransformXMLStreamWriter wasWriter;
            }
            Transformer<WritersHolder> transformer = new Transformer<>(inputXml);
            WritersHolder writersHolder = new WritersHolder();
            transformer.setCargo(writersHolder);
            transformer.registerStartElementHandler("foo", (context, cargo, writer) -> {
                // create new string writer and xml writer...
                StringWriter stringWriter = new StringWriter();
                TransformSimpleWriter xmlWriter = new TransformSimpleWriter(stringWriter);
                // store the new string writer...
                cargo.stringWriters.add(stringWriter);
                // tell transformer to use the new xml writer...
                cargo.wasWriter = context.switchWriter(xmlWriter);
                return null;
            });
            transformer.registerEndElementHandler("foo", (context, cargo, writer) -> {
                // write the end element to the current writer...
                writer.writeEndElement();
                writer.close(); // and we're done with this writer now
                // tell transformer to use the writer it was using...
                context.switchWriter(cargo.wasWriter);
                // tell transformer that we've already written the end tag...
                return ContinueState.HANDLED;
            });
            transformer.nullTransform();
            for (int i = 0, imax = writersHolder.stringWriters.size(); i < imax; i++) {
                String actual = writersHolder.stringWriters.get(i).toString();
                assertEquals("<foo><bar>text " + (i + 1) + "</bar></foo>", actual);
            }
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

}
