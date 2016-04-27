/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Collector;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.Selector;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Element
extends Node {
    private Tag tag;
    private static final Pattern classSplit = Pattern.compile("\\s+");

    public Element(Tag tag, String baseUri, Attributes attributes) {
        super(baseUri, attributes);
        Validate.notNull(tag);
        this.tag = tag;
    }

    public Element(Tag tag, String baseUri) {
        this(tag, baseUri, new Attributes());
    }

    @Override
    public String nodeName() {
        return this.tag.getName();
    }

    public String tagName() {
        return this.tag.getName();
    }

    public Element tagName(String tagName) {
        Validate.notEmpty(tagName, "Tag name must not be empty.");
        this.tag = Tag.valueOf(tagName);
        return this;
    }

    public Tag tag() {
        return this.tag;
    }

    public boolean isBlock() {
        return this.tag.isBlock();
    }

    public String id() {
        return this.attributes.get("id");
    }

    @Override
    public Element attr(String attributeKey, String attributeValue) {
        super.attr(attributeKey, attributeValue);
        return this;
    }

    public Map<String, String> dataset() {
        return this.attributes.dataset();
    }

    @Override
    public final Element parent() {
        return (Element)this.parentNode;
    }

    public Elements parents() {
        Elements parents = new Elements();
        Element.accumulateParents(this, parents);
        return parents;
    }

    private static void accumulateParents(Element el, Elements parents) {
        Element parent = el.parent();
        if (parent != null && !parent.tagName().equals("#root")) {
            parents.add(parent);
            Element.accumulateParents(parent, parents);
        }
    }

    public Element child(int index) {
        return (Element)this.children().get(index);
    }

    public Elements children() {
        ArrayList<Element> elements = new ArrayList<Element>(this.childNodes.size());
        for (Node node : this.childNodes) {
            if (!(node instanceof Element)) continue;
            elements.add((Element)node);
        }
        return new Elements((List<Element>)elements);
    }

    public List<TextNode> textNodes() {
        ArrayList<TextNode> textNodes = new ArrayList<TextNode>();
        for (Node node : this.childNodes) {
            if (!(node instanceof TextNode)) continue;
            textNodes.add((TextNode)node);
        }
        return Collections.unmodifiableList(textNodes);
    }

    public List<DataNode> dataNodes() {
        ArrayList<DataNode> dataNodes = new ArrayList<DataNode>();
        for (Node node : this.childNodes) {
            if (!(node instanceof DataNode)) continue;
            dataNodes.add((DataNode)node);
        }
        return Collections.unmodifiableList(dataNodes);
    }

    public Elements select(String cssQuery) {
        return Selector.select(cssQuery, this);
    }

    public Element appendChild(Node child) {
        Validate.notNull(child);
        this.reparentChild(child);
        this.childNodes.add(child);
        child.setSiblingIndex(this.childNodes.size() - 1);
        return this;
    }

    public Element prependChild(Node child) {
        Validate.notNull(child);
        this.addChildren(0, child);
        return this;
    }

    public Element insertChildren(int index, Collection<? extends Node> children) {
        Validate.notNull(children, "Children collection to be inserted must not be null.");
        int currentSize = this.childNodeSize();
        if (index < 0) {
            index += currentSize + 1;
        }
        Validate.isTrue(index >= 0 && index <= currentSize, "Insert position out of bounds.");
        ArrayList<? extends Node> nodes = new ArrayList<Node>(children);
        Node[] nodeArray = nodes.toArray(new Node[nodes.size()]);
        this.addChildren(index, nodeArray);
        return this;
    }

    public Element appendElement(String tagName) {
        Element child = new Element(Tag.valueOf(tagName), this.baseUri());
        this.appendChild(child);
        return child;
    }

    public Element prependElement(String tagName) {
        Element child = new Element(Tag.valueOf(tagName), this.baseUri());
        this.prependChild(child);
        return child;
    }

    public Element appendText(String text) {
        TextNode node = new TextNode(text, this.baseUri());
        this.appendChild(node);
        return this;
    }

    public Element prependText(String text) {
        TextNode node = new TextNode(text, this.baseUri());
        this.prependChild(node);
        return this;
    }

    public Element append(String html) {
        Validate.notNull(html);
        List<Node> nodes = Parser.parseFragment(html, this, this.baseUri());
        this.addChildren(nodes.toArray(new Node[nodes.size()]));
        return this;
    }

    public Element prepend(String html) {
        Validate.notNull(html);
        List<Node> nodes = Parser.parseFragment(html, this, this.baseUri());
        this.addChildren(0, nodes.toArray(new Node[nodes.size()]));
        return this;
    }

    @Override
    public Element before(String html) {
        return (Element)super.before(html);
    }

    @Override
    public Element before(Node node) {
        return (Element)super.before(node);
    }

    @Override
    public Element after(String html) {
        return (Element)super.after(html);
    }

    @Override
    public Element after(Node node) {
        return (Element)super.after(node);
    }

    public Element empty() {
        this.childNodes.clear();
        return this;
    }

    @Override
    public Element wrap(String html) {
        return (Element)super.wrap(html);
    }

    public String cssSelector() {
        if (this.id().length() > 0) {
            return "#" + this.id();
        }
        StringBuilder selector = new StringBuilder(this.tagName());
        String classes = StringUtil.join(this.classNames(), ".");
        if (classes.length() > 0) {
            selector.append('.').append(classes);
        }
        if (this.parent() == null || this.parent() instanceof Document) {
            return selector.toString();
        }
        selector.insert(0, " > ");
        if (this.parent().select(selector.toString()).size() > 1) {
            selector.append(String.format(":nth-child(%d)", this.elementSiblingIndex() + 1));
        }
        return this.parent().cssSelector() + selector.toString();
    }

    public Elements siblingElements() {
        if (this.parentNode == null) {
            return new Elements(0);
        }
        Elements elements = this.parent().children();
        Elements siblings = new Elements(elements.size() - 1);
        for (Element el : elements) {
            if (el == this) continue;
            siblings.add(el);
        }
        return siblings;
    }

    public Element nextElementSibling() {
        if (this.parentNode == null) {
            return null;
        }
        Elements siblings = this.parent().children();
        Integer index = Element.indexInList(this, siblings);
        Validate.notNull(index);
        if (siblings.size() > index + 1) {
            return (Element)siblings.get(index + 1);
        }
        return null;
    }

    public Element previousElementSibling() {
        if (this.parentNode == null) {
            return null;
        }
        Elements siblings = this.parent().children();
        Integer index = Element.indexInList(this, siblings);
        Validate.notNull(index);
        if (index > 0) {
            return (Element)siblings.get(index - 1);
        }
        return null;
    }

    public Element firstElementSibling() {
        Elements siblings = this.parent().children();
        return siblings.size() > 1 ? (Element)siblings.get(0) : null;
    }

    public Integer elementSiblingIndex() {
        if (this.parent() == null) {
            return 0;
        }
        return Element.indexInList(this, this.parent().children());
    }

    public Element lastElementSibling() {
        Elements siblings = this.parent().children();
        return siblings.size() > 1 ? (Element)siblings.get(siblings.size() - 1) : null;
    }

    private static <E extends Element> Integer indexInList(Element search, List<E> elements) {
        Validate.notNull(search);
        Validate.notNull(elements);
        for (int i = 0; i < elements.size(); ++i) {
            Element element = (Element)elements.get(i);
            if (!element.equals(search)) continue;
            return i;
        }
        return null;
    }

    public Elements getElementsByTag(String tagName) {
        Validate.notEmpty(tagName);
        tagName = tagName.toLowerCase().trim();
        return Collector.collect(new Evaluator.Tag(tagName), this);
    }

    public Element getElementById(String id) {
        Validate.notEmpty(id);
        Elements elements = Collector.collect(new Evaluator.Id(id), this);
        if (elements.size() > 0) {
            return (Element)elements.get(0);
        }
        return null;
    }

    public Elements getElementsByClass(String className) {
        Validate.notEmpty(className);
        return Collector.collect(new Evaluator.Class(className), this);
    }

    public Elements getElementsByAttribute(String key) {
        Validate.notEmpty(key);
        key = key.trim().toLowerCase();
        return Collector.collect(new Evaluator.Attribute(key), this);
    }

    public Elements getElementsByAttributeStarting(String keyPrefix) {
        Validate.notEmpty(keyPrefix);
        keyPrefix = keyPrefix.trim().toLowerCase();
        return Collector.collect(new Evaluator.AttributeStarting(keyPrefix), this);
    }

    public Elements getElementsByAttributeValue(String key, String value) {
        return Collector.collect(new Evaluator.AttributeWithValue(key, value), this);
    }

    public Elements getElementsByAttributeValueNot(String key, String value) {
        return Collector.collect(new Evaluator.AttributeWithValueNot(key, value), this);
    }

    public Elements getElementsByAttributeValueStarting(String key, String valuePrefix) {
        return Collector.collect(new Evaluator.AttributeWithValueStarting(key, valuePrefix), this);
    }

    public Elements getElementsByAttributeValueEnding(String key, String valueSuffix) {
        return Collector.collect(new Evaluator.AttributeWithValueEnding(key, valueSuffix), this);
    }

    public Elements getElementsByAttributeValueContaining(String key, String match) {
        return Collector.collect(new Evaluator.AttributeWithValueContaining(key, match), this);
    }

    public Elements getElementsByAttributeValueMatching(String key, Pattern pattern) {
        return Collector.collect(new Evaluator.AttributeWithValueMatching(key, pattern), this);
    }

    public Elements getElementsByAttributeValueMatching(String key, String regex) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        }
        catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Pattern syntax error: " + regex, e);
        }
        return this.getElementsByAttributeValueMatching(key, pattern);
    }

    public Elements getElementsByIndexLessThan(int index) {
        return Collector.collect(new Evaluator.IndexLessThan(index), this);
    }

    public Elements getElementsByIndexGreaterThan(int index) {
        return Collector.collect(new Evaluator.IndexGreaterThan(index), this);
    }

    public Elements getElementsByIndexEquals(int index) {
        return Collector.collect(new Evaluator.IndexEquals(index), this);
    }

    public Elements getElementsContainingText(String searchText) {
        return Collector.collect(new Evaluator.ContainsText(searchText), this);
    }

    public Elements getElementsContainingOwnText(String searchText) {
        return Collector.collect(new Evaluator.ContainsOwnText(searchText), this);
    }

    public Elements getElementsMatchingText(Pattern pattern) {
        return Collector.collect(new Evaluator.Matches(pattern), this);
    }

    public Elements getElementsMatchingText(String regex) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        }
        catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Pattern syntax error: " + regex, e);
        }
        return this.getElementsMatchingText(pattern);
    }

    public Elements getElementsMatchingOwnText(Pattern pattern) {
        return Collector.collect(new Evaluator.MatchesOwn(pattern), this);
    }

    public Elements getElementsMatchingOwnText(String regex) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        }
        catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Pattern syntax error: " + regex, e);
        }
        return this.getElementsMatchingOwnText(pattern);
    }

    public Elements getAllElements() {
        return Collector.collect(new Evaluator.AllElements(), this);
    }

    public String text() {
        final StringBuilder accum = new StringBuilder();
        new NodeTraversor(new NodeVisitor(){

            public void head(Node node, int depth) {
                if (node instanceof TextNode) {
                    TextNode textNode = (TextNode)node;
                    Element.appendNormalisedText(accum, textNode);
                } else if (node instanceof Element) {
                    Element element = (Element)node;
                    if (accum.length() > 0 && (element.isBlock() || element.tag.getName().equals("br")) && !TextNode.lastCharIsWhitespace(accum)) {
                        accum.append(" ");
                    }
                }
            }

            public void tail(Node node, int depth) {
            }
        }).traverse(this);
        return accum.toString().trim();
    }

    public String ownText() {
        StringBuilder sb = new StringBuilder();
        this.ownText(sb);
        return sb.toString().trim();
    }

    private void ownText(StringBuilder accum) {
        for (Node child : this.childNodes) {
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode)child;
                Element.appendNormalisedText(accum, textNode);
                continue;
            }
            if (!(child instanceof Element)) continue;
            Element.appendWhitespaceIfBr((Element)child, accum);
        }
    }

    private static void appendNormalisedText(StringBuilder accum, TextNode textNode) {
        String text = textNode.getWholeText();
        if (Element.preserveWhitespace(textNode.parentNode)) {
            accum.append(text);
        } else {
            StringUtil.appendNormalisedWhitespace(accum, text, TextNode.lastCharIsWhitespace(accum));
        }
    }

    private static void appendWhitespaceIfBr(Element element, StringBuilder accum) {
        if (element.tag.getName().equals("br") && !TextNode.lastCharIsWhitespace(accum)) {
            accum.append(" ");
        }
    }

    static boolean preserveWhitespace(Node node) {
        if (node != null && node instanceof Element) {
            Element element = (Element)node;
            return element.tag.preserveWhitespace() || element.parent() != null && element.parent().tag.preserveWhitespace();
        }
        return false;
    }

    public Element text(String text) {
        Validate.notNull(text);
        this.empty();
        TextNode textNode = new TextNode(text, this.baseUri);
        this.appendChild(textNode);
        return this;
    }

    public boolean hasText() {
        for (Node child : this.childNodes) {
            Element el;
            TextNode textNode;
            if (!(child instanceof TextNode ? !(textNode = (TextNode)child).isBlank() : child instanceof Element && (el = (Element)child).hasText())) continue;
            return true;
        }
        return false;
    }

    public String data() {
        StringBuilder sb = new StringBuilder();
        for (Node childNode : this.childNodes) {
            if (childNode instanceof DataNode) {
                DataNode data = (DataNode)childNode;
                sb.append(data.getWholeData());
                continue;
            }
            if (!(childNode instanceof Element)) continue;
            Element element = (Element)childNode;
            String elementData = element.data();
            sb.append(elementData);
        }
        return sb.toString();
    }

    public String className() {
        return this.attr("class").trim();
    }

    public Set<String> classNames() {
        String[] names = classSplit.split(this.className());
        LinkedHashSet<String> classNames = new LinkedHashSet<String>(Arrays.asList(names));
        classNames.remove("");
        return classNames;
    }

    public Element classNames(Set<String> classNames) {
        Validate.notNull(classNames);
        this.attributes.put("class", StringUtil.join(classNames, " "));
        return this;
    }

    public boolean hasClass(String className) {
        String[] classes;
        String classAttr = this.attributes.get("class");
        if (classAttr.equals("") || classAttr.length() < className.length()) {
            return false;
        }
        for (String name : classes = classSplit.split(classAttr)) {
            if (!className.equalsIgnoreCase(name)) continue;
            return true;
        }
        return false;
    }

    public Element addClass(String className) {
        Validate.notNull(className);
        Set<String> classes = this.classNames();
        classes.add(className);
        this.classNames(classes);
        return this;
    }

    public Element removeClass(String className) {
        Validate.notNull(className);
        Set<String> classes = this.classNames();
        classes.remove(className);
        this.classNames(classes);
        return this;
    }

    public Element toggleClass(String className) {
        Validate.notNull(className);
        Set<String> classes = this.classNames();
        if (classes.contains(className)) {
            classes.remove(className);
        } else {
            classes.add(className);
        }
        this.classNames(classes);
        return this;
    }

    public String val() {
        if (this.tagName().equals("textarea")) {
            return this.text();
        }
        return this.attr("value");
    }

    public Element val(String value) {
        if (this.tagName().equals("textarea")) {
            this.text(value);
        } else {
            this.attr("value", value);
        }
        return this;
    }

    @Override
    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        if (accum.length() > 0 && out.prettyPrint() && (this.tag.formatAsBlock() || this.parent() != null && this.parent().tag().formatAsBlock() || out.outline())) {
            this.indent(accum, depth, out);
        }
        accum.append("<").append(this.tagName());
        this.attributes.html(accum, out);
        if (this.childNodes.isEmpty() && this.tag.isSelfClosing()) {
            if (out.syntax() == Document.OutputSettings.Syntax.html && this.tag.isEmpty()) {
                accum.append('>');
            } else {
                accum.append(" />");
            }
        } else {
            accum.append(">");
        }
    }

    @Override
    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {
        if (!this.childNodes.isEmpty() || !this.tag.isSelfClosing()) {
            if (out.prettyPrint() && !this.childNodes.isEmpty() && (this.tag.formatAsBlock() || out.outline() && (this.childNodes.size() > 1 || this.childNodes.size() == 1 && !(this.childNodes.get(0) instanceof TextNode)))) {
                this.indent(accum, depth, out);
            }
            accum.append("</").append(this.tagName()).append(">");
        }
    }

    public String html() {
        StringBuilder accum = new StringBuilder();
        this.html(accum);
        return this.getOutputSettings().prettyPrint() ? accum.toString().trim() : accum.toString();
    }

    private void html(StringBuilder accum) {
        for (Node node : this.childNodes) {
            node.outerHtml(accum);
        }
    }

    public Element html(String html) {
        this.empty();
        this.append(html);
        return this;
    }

    @Override
    public String toString() {
        return this.outerHtml();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Element element = (Element)o;
        return this.tag.equals(element.tag);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.tag != null ? this.tag.hashCode() : 0);
        return result;
    }

    @Override
    public Element clone() {
        return (Element)super.clone();
    }

}

