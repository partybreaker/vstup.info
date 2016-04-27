/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class Node
implements Cloneable {
    Node parentNode;
    List<Node> childNodes;
    Attributes attributes;
    String baseUri;
    int siblingIndex;

    protected Node(String baseUri, Attributes attributes) {
        Validate.notNull(baseUri);
        Validate.notNull(attributes);
        this.childNodes = new ArrayList<Node>(4);
        this.baseUri = baseUri.trim();
        this.attributes = attributes;
    }

    protected Node(String baseUri) {
        this(baseUri, new Attributes());
    }

    protected Node() {
        this.childNodes = Collections.emptyList();
        this.attributes = null;
    }

    public abstract String nodeName();

    public String attr(String attributeKey) {
        Validate.notNull(attributeKey);
        if (this.attributes.hasKey(attributeKey)) {
            return this.attributes.get(attributeKey);
        }
        if (attributeKey.toLowerCase().startsWith("abs:")) {
            return this.absUrl(attributeKey.substring("abs:".length()));
        }
        return "";
    }

    public Attributes attributes() {
        return this.attributes;
    }

    public Node attr(String attributeKey, String attributeValue) {
        this.attributes.put(attributeKey, attributeValue);
        return this;
    }

    public boolean hasAttr(String attributeKey) {
        String key;
        Validate.notNull(attributeKey);
        if (attributeKey.startsWith("abs:") && this.attributes.hasKey(key = attributeKey.substring("abs:".length())) && !this.absUrl(key).equals("")) {
            return true;
        }
        return this.attributes.hasKey(attributeKey);
    }

    public Node removeAttr(String attributeKey) {
        Validate.notNull(attributeKey);
        this.attributes.remove(attributeKey);
        return this;
    }

    public String baseUri() {
        return this.baseUri;
    }

    public void setBaseUri(final String baseUri) {
        Validate.notNull(baseUri);
        this.traverse(new NodeVisitor(){

            public void head(Node node, int depth) {
                node.baseUri = baseUri;
            }

            public void tail(Node node, int depth) {
            }
        });
    }

    public String absUrl(String attributeKey) {
        Validate.notEmpty(attributeKey);
        String relUrl = this.attr(attributeKey);
        if (!this.hasAttr(attributeKey)) {
            return "";
        }
        try {
            URL base;
            try {
                base = new URL(this.baseUri);
            }
            catch (MalformedURLException e) {
                URL abs = new URL(relUrl);
                return abs.toExternalForm();
            }
            if (relUrl.startsWith("?")) {
                relUrl = base.getPath() + relUrl;
            }
            URL abs = new URL(base, relUrl);
            return abs.toExternalForm();
        }
        catch (MalformedURLException e) {
            return "";
        }
    }

    public Node childNode(int index) {
        return this.childNodes.get(index);
    }

    public List<Node> childNodes() {
        return Collections.unmodifiableList(this.childNodes);
    }

    public List<Node> childNodesCopy() {
        ArrayList<Node> children = new ArrayList<Node>(this.childNodes.size());
        for (Node node : this.childNodes) {
            children.add(node.clone());
        }
        return children;
    }

    public final int childNodeSize() {
        return this.childNodes.size();
    }

    protected Node[] childNodesAsArray() {
        return this.childNodes.toArray(new Node[this.childNodeSize()]);
    }

    public Node parent() {
        return this.parentNode;
    }

    public final Node parentNode() {
        return this.parentNode;
    }

    public Document ownerDocument() {
        if (this instanceof Document) {
            return (Document)this;
        }
        if (this.parentNode == null) {
            return null;
        }
        return this.parentNode.ownerDocument();
    }

    public void remove() {
        Validate.notNull(this.parentNode);
        this.parentNode.removeChild(this);
    }

    public Node before(String html) {
        this.addSiblingHtml(this.siblingIndex, html);
        return this;
    }

    public Node before(Node node) {
        Validate.notNull(node);
        Validate.notNull(this.parentNode);
        this.parentNode.addChildren(this.siblingIndex, node);
        return this;
    }

    public Node after(String html) {
        this.addSiblingHtml(this.siblingIndex + 1, html);
        return this;
    }

    public Node after(Node node) {
        Validate.notNull(node);
        Validate.notNull(this.parentNode);
        this.parentNode.addChildren(this.siblingIndex + 1, node);
        return this;
    }

    private void addSiblingHtml(int index, String html) {
        Validate.notNull(html);
        Validate.notNull(this.parentNode);
        Element context = this.parent() instanceof Element ? (Element)this.parent() : null;
        List<Node> nodes = Parser.parseFragment(html, context, this.baseUri());
        this.parentNode.addChildren(index, nodes.toArray(new Node[nodes.size()]));
    }

    public Node wrap(String html) {
        Validate.notEmpty(html);
        Element context = this.parent() instanceof Element ? (Element)this.parent() : null;
        List<Node> wrapChildren = Parser.parseFragment(html, context, this.baseUri());
        Node wrapNode = wrapChildren.get(0);
        if (wrapNode == null || !(wrapNode instanceof Element)) {
            return null;
        }
        Element wrap = (Element)wrapNode;
        Element deepest = this.getDeepChild(wrap);
        this.parentNode.replaceChild(this, wrap);
        deepest.addChildren(this);
        if (wrapChildren.size() > 0) {
            for (int i = 0; i < wrapChildren.size(); ++i) {
                Node remainder = wrapChildren.get(i);
                remainder.parentNode.removeChild(remainder);
                wrap.appendChild(remainder);
            }
        }
        return this;
    }

    public Node unwrap() {
        Validate.notNull(this.parentNode);
        Node firstChild = this.childNodes.size() > 0 ? this.childNodes.get(0) : null;
        this.parentNode.addChildren(this.siblingIndex, this.childNodesAsArray());
        this.remove();
        return firstChild;
    }

    private Element getDeepChild(Element el) {
        Elements children = el.children();
        if (children.size() > 0) {
            return this.getDeepChild((Element)children.get(0));
        }
        return el;
    }

    public void replaceWith(Node in) {
        Validate.notNull(in);
        Validate.notNull(this.parentNode);
        this.parentNode.replaceChild(this, in);
    }

    protected void setParentNode(Node parentNode) {
        if (this.parentNode != null) {
            this.parentNode.removeChild(this);
        }
        this.parentNode = parentNode;
    }

    protected void replaceChild(Node out, Node in) {
        Validate.isTrue(out.parentNode == this);
        Validate.notNull(in);
        if (in.parentNode != null) {
            in.parentNode.removeChild(in);
        }
        int index = out.siblingIndex;
        this.childNodes.set(index, in);
        in.parentNode = this;
        in.setSiblingIndex(index);
        out.parentNode = null;
    }

    protected void removeChild(Node out) {
        Validate.isTrue(out.parentNode == this);
        int index = out.siblingIndex;
        this.childNodes.remove(index);
        this.reindexChildren(index);
        out.parentNode = null;
    }

    protected /* varargs */ void addChildren(Node ... children) {
        for (Node child : children) {
            this.reparentChild(child);
            this.childNodes.add(child);
            child.setSiblingIndex(this.childNodes.size() - 1);
        }
    }

    protected /* varargs */ void addChildren(int index, Node ... children) {
        Validate.noNullElements(children);
        for (int i = children.length - 1; i >= 0; --i) {
            Node in = children[i];
            this.reparentChild(in);
            this.childNodes.add(index, in);
        }
        this.reindexChildren(index);
    }

    protected void reparentChild(Node child) {
        if (child.parentNode != null) {
            child.parentNode.removeChild(child);
        }
        child.setParentNode(this);
    }

    private void reindexChildren(int start) {
        for (int i = start; i < this.childNodes.size(); ++i) {
            this.childNodes.get(i).setSiblingIndex(i);
        }
    }

    public List<Node> siblingNodes() {
        if (this.parentNode == null) {
            return Collections.emptyList();
        }
        List<Node> nodes = this.parentNode.childNodes;
        ArrayList<Node> siblings = new ArrayList<Node>(nodes.size() - 1);
        for (Node node : nodes) {
            if (node == this) continue;
            siblings.add(node);
        }
        return siblings;
    }

    public Node nextSibling() {
        if (this.parentNode == null) {
            return null;
        }
        List<Node> siblings = this.parentNode.childNodes;
        int index = this.siblingIndex + 1;
        if (siblings.size() > index) {
            return siblings.get(index);
        }
        return null;
    }

    public Node previousSibling() {
        if (this.parentNode == null) {
            return null;
        }
        if (this.siblingIndex > 0) {
            return this.parentNode.childNodes.get(this.siblingIndex - 1);
        }
        return null;
    }

    public int siblingIndex() {
        return this.siblingIndex;
    }

    protected void setSiblingIndex(int siblingIndex) {
        this.siblingIndex = siblingIndex;
    }

    public Node traverse(NodeVisitor nodeVisitor) {
        Validate.notNull(nodeVisitor);
        NodeTraversor traversor = new NodeTraversor(nodeVisitor);
        traversor.traverse(this);
        return this;
    }

    public String outerHtml() {
        StringBuilder accum = new StringBuilder(128);
        this.outerHtml(accum);
        return accum.toString();
    }

    protected void outerHtml(StringBuilder accum) {
        new NodeTraversor(new OuterHtmlVisitor(accum, this.getOutputSettings())).traverse(this);
    }

    Document.OutputSettings getOutputSettings() {
        return this.ownerDocument() != null ? this.ownerDocument().outputSettings() : new Document("").outputSettings();
    }

    abstract void outerHtmlHead(StringBuilder var1, int var2, Document.OutputSettings var3);

    abstract void outerHtmlTail(StringBuilder var1, int var2, Document.OutputSettings var3);

    public String toString() {
        return this.outerHtml();
    }

    protected void indent(StringBuilder accum, int depth, Document.OutputSettings out) {
        accum.append("\n").append(StringUtil.padding(depth * out.indentAmount()));
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) return false;
        if (this.getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node)o;
        if (this.childNodes != null ? !this.childNodes.equals(node.childNodes) : node.childNodes != null) {
            return false;
        }
        if (this.attributes == null) {
            if (node.attributes != null) return false;
            return true;
        }
        if (this.attributes.equals(node.attributes)) return true;
        return false;
    }

    public int hashCode() {
        int result = this.childNodes != null ? this.childNodes.hashCode() : 0;
        result = 31 * result + (this.attributes != null ? this.attributes.hashCode() : 0);
        return result;
    }

    public Node clone() {
        Node thisClone = this.doClone(null);
        LinkedList<Node> nodesToProcess = new LinkedList<Node>();
        nodesToProcess.add(thisClone);
        while (!nodesToProcess.isEmpty()) {
            Node currParent = (Node)nodesToProcess.remove();
            for (int i = 0; i < currParent.childNodes.size(); ++i) {
                Node childClone = currParent.childNodes.get(i).doClone(currParent);
                currParent.childNodes.set(i, childClone);
                nodesToProcess.add(childClone);
            }
        }
        return thisClone;
    }

    protected Node doClone(Node parent) {
        Node clone;
        try {
            clone = (Node)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.parentNode = parent;
        clone.siblingIndex = parent == null ? 0 : this.siblingIndex;
        clone.attributes = this.attributes != null ? this.attributes.clone() : null;
        clone.baseUri = this.baseUri;
        clone.childNodes = new ArrayList<Node>(this.childNodes.size());
        for (Node child : this.childNodes) {
            clone.childNodes.add(child);
        }
        return clone;
    }

    private static class OuterHtmlVisitor
    implements NodeVisitor {
        private StringBuilder accum;
        private Document.OutputSettings out;

        OuterHtmlVisitor(StringBuilder accum, Document.OutputSettings out) {
            this.accum = accum;
            this.out = out;
        }

        public void head(Node node, int depth) {
            node.outerHtmlHead(this.accum, depth, this.out);
        }

        public void tail(Node node, int depth) {
            if (!node.nodeName().equals("#text")) {
                node.outerHtmlTail(this.accum, depth, this.out);
            }
        }
    }

}

