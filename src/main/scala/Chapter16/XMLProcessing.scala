package Chapter16

import scala.xml._

object XMLProcessing {
// topics:
    // XML literals
    // XML nodes
    // element attributes
    // embedded expressions
    // expressions in attributes
    // uncommon node types
    // XPath-like expressions
    // pattern matching
    // modifying elements and attributes
    // transforming XML
    // loading and saving
    // namespaces

    // built-in support for XML in scala
    // https://www.scala-lang.org/files/archive/api/current/scala-xml/scala/xml/index.html

    //NodeSeq type '<like>this</like>
    // scala code inside xml literals
    // node.child: yilds child nodes
    // node attributes yields metadata object
    // '\' and '\\' carry out xpath-like matches
    // match node with xml literals in 'case' clauses
    // RuleTransformer, RewriteRule to transform subnodes
    // java xml methods for loading/saving
    // ConstructingParser preserves comments and CDATA

    // XML literals
    def XMLLiterals = {
        val doc: scala.xml.Elem = <html><head><title>Fred's Memoirs</title></head><body></body></html>
        // or sequence of nodes
        val items: scala.xml.NodeSeq = <li>Fred</li><li>Wilma</li>

        // problem with xml literals
        val (x, y) = (1, 2)
        x < y // ok
        // x <y // error
    }

    // XML nodes
    def XMLNodes = {
        // Seq[Node] <- NodeSeq <- Node
        // NodeSeq <- Document
        // Node <- Elem
        // Node <- SpecialNode ... Atom, EntityRef, ProcInstr, Comment
        // Atom <- Text, PCData, Unparced

        val elem: Elem = <a href="http://www">The <em>Scala</em> language</a>
        elem.label // String = a
        elem.child // Seq[scala.xml.Node] = ArrayBuffer(The , <em>Scala</em>,  language)
        // no info about parent

        // NodeSeq supports xpath-like operators; any Seq operators
        for (n <- elem.child) print(s"'$n'") // 'The ''<em>Scala</em>'' language'

        // Node extends NodeSeq, a single node is a seq of length 1;
        // it actually creates as many problems as it solves, not really good design

        // also classes for comments
        val comments: Comment = <!-- ... -->
        // entity references
        val eref: EntityRef = EntityRef("eacute;") // <li>&eacute;</li>.child(0)
        // process instructions
        val procinstr: ProcInstr = ProcInstr(proctext = "foo", target = "bar") // scala.xml.ProcInstr = <?bar foo?>

        // NodeBuffer extends ArrayBuffer[Node] for building node seq
        val items = new NodeBuffer
        items += <li>Fred</li>
        items += <li>Wilma</li>
        val nodes: NodeSeq = items
        // n.b. node seq are supposed to be immutable, don't mutate buffer
    }

    // element attributes
    def elementAttributes = {
        // attributes: MetaData, like a Map
        val elem: Elem = <a href="http://www">The <em>Scala</em> language</a>
        val url = elem.attributes("href") // Seq[scala.xml.Node] = http://www

        // attribute might contain entity references
        // there is no way to know what &eacute; means
        // in xhtml it means &#233; but in another doctype -- who knows?
        val image: Elem = <img alt="San Jos&eacute; State University Logo" src="logo.jpg"/>
        val alt = image.attributes("alt") // Seq[scala.xml.Node] = ArrayBuffer(San Jos, &eacute;,  State University Logo)
        // you can use character references: &#233;

        // you can call text method
        image.attributes("alt").text // res12: String = San Jos&eacute; State University Logo
        elem.attributes("href").text //res13: String = http://www

        // null if no such attrib
        image.attributes("href") //res15: Seq[scala.xml.Node] = null
        // use get
        image.attributes.get("href") //res17: Option[Seq[scala.xml.Node]] = None
        image.attributes.get("href").getOrElse(Text(""))

        // iterate over all attribs
        for (a <- elem.attributes) print(s"'${a.key} -> ${a.value}'") // 'href -> http://www'

        // or call asAttrMap
        image.attributes.asAttrMap // Map[String,String] = Map(alt -> San Jos&eacute; State University Logo, src -> logo.jpg)
    }

    // embedded expressions
    def embeddedExpressions = {
        // scala code inside xml literals
        val items = Seq("foo", "bar")
        <ul><li>{items(0)}</li><li>{items(1)}</li></ul> // scala.xml.Elem = <ul><li>foo</li><li>bar</li></ul>
        // node seq added to xml; other elems turned into an Atom[T]; atom.data retrieve the value
        // atom.data.toString is called on save xml

        // embedded strings turned into Atom[String], not Text: beware using pattern matching
        // one way to fix that:
        <li>{Text("foo")}</li> // scala.xml.Elem = <li>foo</li>

        // nested scala code can contain xml literals
        <ul>{for (i <- items) yield <li>{i}</li>}</ul> // scala.xml.Elem = <ul><li>foo</li><li>bar</li></ul>

        // escape brace using double braces
        <h1>The Natural Numbers {{1, 2, 3, ...}}</h1> // scala.xml.Elem = <h1>The Natural Numbers {1, 2, 3, ...}</h1>
    }

    // expressions in attributes
    def expressionsInAttributes = {
        def getURL() = "some url"
        <img src={getURL()}/> // scala.xml.Elem = <img src="some url"/>

        // braces inside quoted strings are not evaluated
        <img src="{getURL()}"/> // scala.xml.Elem = <img src="{getURL()}"/>

        // block can yield a node seq
        <a id={new Atom(42)}/> // scala.xml.Elem = <a id="42"/>
        // or
        val image: Elem = <img alt="San Jos&eacute; State University Logo" src="logo.jpg"/>
        <a id={image.attributes("alt")}/> // scala.xml.Elem = <a id="San Jos&eacute; State University Logo"/>

        // null attribute will be omitted
        <img alt={if (getURL == "some url") null else getURL}/> // scala.xml.Elem = <img/>
        // Option
        <img alt={if (getURL == "some url") None else Some(Text(getURL))}/> // scala.xml.Elem = <img/>

        // block should yield: String, Seq[Node], Option[Seq[Node]]
        // n.b. block inside element wrapped in Atom
    }

    // uncommon node types
    def uncommonNodeTypes = {
        // javascript?

        val js = <script><![CDATA[if (temp < 0) alert("cold")]]></script>
        // not really
        // node with a text child
        // js: scala.xml.Elem = <script><![CDATA[if (temp < 0) alert("cold")]]></script>
        //scala> js.child //res36: Seq[scala.xml.Node] = ArrayBuffer(<![CDATA[if (temp < 0) alert("cold")]]>)

        // add a PCData node
        val code = """ if (temp < 0) alert("cold") """
        val js2 = <script>{PCData(code)}</script>

        // arbitrary text in an Unparsed node
        // val n1 = <xml:unparsed><&></xml:unparsed> // scala.xml.Unparsed = <&>
        // val n2 = Unparsed("<&>") // scala.xml.Unparsed = <&>

        // you can group a node seq
        val g1 = <xml:group><li>item1</li><li>item2</li></xml:group> // g1: scala.xml.Group = <li>item1</li><li>item2</li>
        val g2 = Group(Seq(
            <li>item1</li>,
            <li>item2</li>
        )) // g2: scala.xml.Group = <li>item1</li><li>item2</li>

        // group nodes are 'ungrouped' when you iterate over them:
        val items = <li>item1</li><li>item2</li> // scala.xml.NodeBuffer = ArrayBuffer(<li>item1</li>, <li>item2</li>)
        // two elements
        for (n <- <xml:group>{items}</xml:group>) yield n // scala.xml.NodeSeq = NodeSeq(<li>item1</li>, <li>item2</li>)
        // one element
        for (n <- <ol>{items}</ol>) yield n // scala.xml.NodeSeq = NodeSeq(<ol><li>item1</li><li>item2</li></ol>)

    }

    // XPath-like expressions
    def XPathLikeExpressions = {
        // NodeSeq provides operators like XPath '/' and '//'
        // only '\' and '\\'
        // \ direct descendants
        // \\ descendants of any depth

        val list = <dl><dt>Java</dt><dd>Gosling</dd><dt>Scala</dt><dd>Odersky</dd></dl> // scala.xml.Elem = <dl><dt>Java</dt><dd>Gosling</dd><dt>Scala</dt><dd>Odersky</dd></dl>
        val language = list \ "dt" //scala.xml.NodeSeq = NodeSeq(<dt>Java</dt>, <dt>Scala</dt>)

        // a wildcard for any element (ul or ol)
        val doc: scala.xml.Elem = <html><body><ul><li alt="oof">foo</li></ul><ol><li alt="rab">bar</li></ol></body></html>
        doc \ "body" \ "_" \ "li" // scala.xml.NodeSeq = NodeSeq(<li>foo</li>, <li>bar</li>)

        // \\ any depth
        doc \\ "li" // scala.xml.NodeSeq = NodeSeq(<li>foo</li>, <li>bar</li>)

        // attributes (no wildcard for attribs)
        doc \\ "@alt" // scala.xml.NodeSeq = NodeSeq(oof, rab)

        // unlike xpath, you can't use '\' to extract attribs from multiple nodes
        doc \\ "li" \\ "@alt" // should work
        // scala.xml.NodeSeq = NodeSeq(oof, rab)

        // as a sequence, use traversing
        for (n <- doc \\ "li") print(s"'${n}'") // '<li alt="oof">foo</li>''<li alt="rab">bar</li>'

        // text will concatenate
        (doc \\ "@alt").text // String = oofrab
    }

    // pattern matching
    def patternMatching = {
        ???
    }

    // modifying elements and attributes
    def modifyingElementsAndAttributes = {
        ???
    }

    // transforming XML
    def transformingXML = {
        ???
    }

    // loading and saving
    def loadingAndSaving = {
        ???
    }

    // namespaces
    def namespaces = {
        ???
    }

}

object XMLProcessing_Exercises {

    // 1. What is
    // <fred/>(0)
    // <fred/>(0)(0)
    // Why?
    def ex1 = {
        ???
    }

    // 2. What is the result of
    //  <ul>
    //      <li>Opening bracket: [</li>
    //      <li>Closing bracket: ]</li>
    //      <li>Opening brace: {</li>
    //      <li>Closing brace: }</li>
    //  </ul>
    //How do you fix it?
    def ex2 = {
        ???
    }

    // 3. Contrast
    //  <li>Fred</li> match { case <li>{Text(t)}</li> => t }
    //and
    //  <li>{"Fred"}</li> match { case <li>{Text(t)}</li> => t }
    // Why do they act differently?
    def ex3 = {
        ???
    }

    // 4. Read an XHTML file and print all 'img' elements that don’t have an 'alt' attribute.
    def ex4 = {
        ???
    }

    // 5. Print the names of all images in an XHTML file.
    // That is, print all 'src' attribute values inside 'img' elements.
    def ex5 = {
        ???
    }

    // 6. Read an XHTML file and print a table of all hyperlinks in the file, together with their URLs.
    // That is, print the 'child' text and the 'href' attribute of each a element.
    def ex6 = {
        ???
    }

    // 7. Write a function that has a parameter of type Map[String, String] and returns a 'dl'
    // element with a 'dt' for each key and 'dd' for each value. For example,
    //  Map("A" -> "1", "B" -> "2")
    // should yield
    //  <dl>
    //      <dt>A</dt> <dd>1</dd>
    //      <dt>B</dt> <dd>2</dd>
    //  </dl>
    def ex7 = {
        ???
    }

    // 8. Write a function that takes a 'dl' element and turns it into a Map[String, String].
    // This function should be the inverse of the function in the preceding exercise,
    // provided all 'dt' children are distinct.
    def ex8 = {
        ???
    }

    // 9. Transform an XHTML document by adding an
    // alt="TODO"
    // attribute to all 'img' elements without an alt attribute, preserving everything else.
    def ex9 = {
        ???
    }

    // 10. Write a function that reads an XHTML document, carries out the transformation of the
    // preceding exercise, and saves the result.
    // Be sure to preserve the DTD and any CDATA sections.
    def ex10 = {
        ???
    }

}