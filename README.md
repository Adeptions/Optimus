# Optimus
## A Java XML transformation engine

##### Identity Transforms
At its simplest, Optimus is just a wrapper around a SAX reader and XML writer - which will, given no more instruction, just perform an "identity transform". For example:-
```java
String inputXml = "<root><foo><bar>text node</bar></foo></root>";
Optimus transformer = new Optimus(inputXml);
String outputXml = transformer.transform();
assertEquals(inputXml, outputXml);
```
Doing the same, but instead writing the output to a file...
```java
String inputXml = "<root><foo><bar>text node</bar></foo></root>";
Optimus transformer = new Optimus(testXml);
transformer.setOmitXmlDeclaration(false); // we want an xml decl in files!
Writer writer = new OutputStreamWriter(new FileOutputStream("C:/temp/out.xml"), "UTF-8");
transformer.transform(writer);
```
Or reading from an input XML file and writing to another...
```java
Reader reader = new InputStreamReader(new FileInputStream("C:/temp/in.xml"));
Optimus transformer = new Optimus(reader);
transformer.setOmitXmlDeclaration(false); // we want an xml decl in files!
Writer writer = new OutputStreamWriter(new FileOutputStream("C:/temp/out.xml"), "UTF-8");
transformer.transform(writer);
```

##### Overriding Identity Transforms
Of course, a straight identity transform isn't much use - we generally want to make some changes.  Optimus provides the ability to register handlers that can alter the output.  For example, say we wanted to rename all the `<foo>` elements to `<baz>`...
```java
Optimus transformer = new Optimus("<root><foo><bar>text node</bar></foo></root>");
transformer.registerStartElementHandler("foo", (context, cargo, writer) -> {
    context.setOverrideName(new QName("baz"));
	return null;
});
String outputXml = transformer.transform();
```
Or say we wanted to remove the `<foo>` elements (but keep the nodes within them)...
```java
Optimus transformer = new Optimus("<root><foo><bar>text node</bar></foo></root>");
transformer.registerStartElementHandler("foo", (context, cargo, writer) -> {
	return ContinueState.SKIP_THIS;
});
String outputXml = transformer.transform();
```
Or to remove the `<foo>` elements altogether (including their descendant nodes)...
```java
Optimus transformer = new Optimus("<root><foo><bar>text node</bar></foo></root>");
transformer.registerStartElementHandler("foo", (context, cargo, writer) -> {
	return ContinueState.SKIP_THIS_AND_DESCENDANTS;
});
String outputXml = transformer.transform();
```
Or perhaps wrapping the `<foo>` elements with a `<baz>` element around them...
```java
Optimus transformer = new Optimus("<root><foo><bar>text node</bar></foo></root>");
transformer.registerStartElementHandler("foo", (context, cargo, writer) -> {
	writer.writeStartElement("baz"); // wrap before <foo> gets written
	return null;
});
transformer.registerEndElementHandler("foo", (context, cargo, writer) -> {
	writer.writeEndElement(); // end the <foo>
	writer.writeEndElement(); // end our new <baz> element
	// tell transformer that we've done what needs to be done...
	return ContinueState.HANDLED;
});
String outputXml = transformer.transform();
```
Or wrapping all the descendants of the `<foo>` elements with a `<baz>` element...
```java
Optimus transformer = new Optimus("<root><foo><bar>text node</bar></foo></root>");
transformer.registerAfterStartElementHandler("foo", (context, cargo, writer) -> {
	writer.writeStartElement("baz"); // wrap start before <foo> gets written
	return null;
});
transformer.registerEndElementHandler("foo", (context, cargo, writer) -> {
	writer.writeEndElement(); // end our new <baz> element
	return ContinueState.CONTINUE; // let transformer end the <foo> element as normal
});
String outputXml = transformer.transform();
```
The cargo passed to each handler can be used to track a state during transform.  For example, say we wanted to add an `@id` attribute with a sequential value to each and every element...
```java
class CounterHolder {
	Long counter = 0L;
}

Optimus<CounterHolder> transformer = new Optimus<CounterHolder>("<root><foo><bar>text node</bar></foo></root>");
transformer.setCargo(new CounterHolder());
transformer.registerBeforeAttributesHandler("*", (context, cargo, writer) -> {
	cargo.counter++;
	writer.writeAttribute("id", cargo.counter.toString());
	return null;
});
String outputXml = transformer.transform();
```
Although the above code might be a little dangerous - if any element already has an `@id` attribute.  So we should suppress the existing `@id` attributes...
```java
class CounterHolder {
	Long counter = 0L;
}

Optimus<CounterHolder> transformer = new Optimus<CounterHolder>("<root><foo><bar>text node</bar></foo></root>");
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
```

##### Collecting data from XML
Optimus can also be used to collect data from an input XML, in which case the output XML is of no interest - so a null transform is provided.  For example, collecting all of the text from within `<foo>` elements...
```java
String inputXml = "<root><foo>some text</foo><foo>some more text</foo><foo>yet more text</foo><bar>not this though</bar></root>";
StringBuilder textCollector = new StringBuilder();
Optimus<StringBuilder> transformer = new Optimus<StringBuilder>(inputXml);
transformer.setCargo(textCollector);
transformer.registerCharactersHandler("foo/#text()", (context, cargo, writer) -> {
	cargo.append(context.getText()).append("\n");
	return null;
});
transformer.nullTransform();
assertEquals("some text\nsome more text\nyet more text\n", textCollector.toString());
```

## Javadocs

see [Optimus javadocs](https://adeptions.github.io/Optimus/)