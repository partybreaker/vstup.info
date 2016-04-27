/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Tokeniser;
import org.jsoup.parser.TreeBuilder;
import org.jsoup.parser.XmlTreeBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Parser {
    private static final int DEFAULT_MAX_ERRORS = 0;
    private TreeBuilder treeBuilder;
    private int maxErrors = 0;
    private ParseErrorList errors;

    public Parser(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
    }

    public Document parseInput(String html, String baseUri) {
        this.errors = this.isTrackErrors() ? ParseErrorList.tracking(this.maxErrors) : ParseErrorList.noTracking();
        Document doc = this.treeBuilder.parse(html, baseUri, this.errors);
        return doc;
    }

    public TreeBuilder getTreeBuilder() {
        return this.treeBuilder;
    }

    public Parser setTreeBuilder(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
        return this;
    }

    public boolean isTrackErrors() {
        return this.maxErrors > 0;
    }

    public Parser setTrackErrors(int maxErrors) {
        this.maxErrors = maxErrors;
        return this;
    }

    public List<ParseError> getErrors() {
        return this.errors;
    }

    public static Document parse(String html, String baseUri) {
        HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
        return treeBuilder.parse(html, baseUri, ParseErrorList.noTracking());
    }

    public static List<Node> parseFragment(String fragmentHtml, Element context, String baseUri) {
        HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
        return treeBuilder.parseFragment(fragmentHtml, context, baseUri, ParseErrorList.noTracking());
    }

    public static List<Node> parseXmlFragment(String fragmentXml, String baseUri) {
        XmlTreeBuilder treeBuilder = new XmlTreeBuilder();
        return treeBuilder.parseFragment(fragmentXml, baseUri, ParseErrorList.noTracking());
    }

    public static Document parseBodyFragment(String bodyHtml, String baseUri) {
        Document doc = Document.createShell(baseUri);
        Element body = doc.body();
        List<Node> nodeList = Parser.parseFragment(bodyHtml, body, baseUri);
        Node[] nodes = nodeList.toArray(new Node[nodeList.size()]);
        for (int i = nodes.length - 1; i > 0; --i) {
            nodes[i].remove();
        }
        for (Node node : nodes) {
            body.appendChild(node);
        }
        return doc;
    }

    public static String unescapeEntities(String string, boolean inAttribute) {
        Tokeniser tokeniser = new Tokeniser(new CharacterReader(string), ParseErrorList.noTracking());
        return tokeniser.unescapeEntities(inAttribute);
    }

    public static Document parseBodyFragmentRelaxed(String bodyHtml, String baseUri) {
        return Parser.parse(bodyHtml, baseUri);
    }

    public static Parser htmlParser() {
        return new Parser(new HtmlTreeBuilder());
    }

    public static Parser xmlParser() {
        return new Parser(new XmlTreeBuilder());
    }
}

