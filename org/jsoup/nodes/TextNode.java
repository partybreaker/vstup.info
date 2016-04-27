/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import java.util.List;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

public class TextNode
extends Node {
    private static final String TEXT_KEY = "text";
    String text;

    public TextNode(String text, String baseUri) {
        this.baseUri = baseUri;
        this.text = text;
    }

    public String nodeName() {
        return "#text";
    }

    public String text() {
        return TextNode.normaliseWhitespace(this.getWholeText());
    }

    public TextNode text(String text) {
        this.text = text;
        if (this.attributes != null) {
            this.attributes.put("text", text);
        }
        return this;
    }

    public String getWholeText() {
        return this.attributes == null ? this.text : this.attributes.get("text");
    }

    public boolean isBlank() {
        return StringUtil.isBlank(this.getWholeText());
    }

    public TextNode splitText(int offset) {
        Validate.isTrue(offset >= 0, "Split offset must be not be negative");
        Validate.isTrue(offset < this.text.length(), "Split offset must not be greater than current text length");
        String head = this.getWholeText().substring(0, offset);
        String tail = this.getWholeText().substring(offset);
        this.text(head);
        TextNode tailNode = new TextNode(tail, this.baseUri());
        if (this.parent() != null) {
            this.parent().addChildren(this.siblingIndex() + 1, tailNode);
        }
        return tailNode;
    }

    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        if (out.prettyPrint() && (this.siblingIndex() == 0 && this.parentNode instanceof Element && ((Element)this.parentNode).tag().formatAsBlock() && !this.isBlank() || out.outline() && this.siblingNodes().size() > 0 && !this.isBlank())) {
            this.indent(accum, depth, out);
        }
        boolean normaliseWhite = out.prettyPrint() && this.parent() instanceof Element && !Element.preserveWhitespace(this.parent());
        Entities.escape(accum, this.getWholeText(), out, false, normaliseWhite, false);
    }

    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {
    }

    public String toString() {
        return this.outerHtml();
    }

    public static TextNode createFromEncoded(String encodedText, String baseUri) {
        String text = Entities.unescape(encodedText);
        return new TextNode(text, baseUri);
    }

    static String normaliseWhitespace(String text) {
        text = StringUtil.normaliseWhitespace(text);
        return text;
    }

    static String stripLeadingWhitespace(String text) {
        return text.replaceFirst("^\\s+", "");
    }

    static boolean lastCharIsWhitespace(StringBuilder sb) {
        return sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ';
    }

    private void ensureAttributes() {
        if (this.attributes == null) {
            this.attributes = new Attributes();
            this.attributes.put("text", this.text);
        }
    }

    public String attr(String attributeKey) {
        this.ensureAttributes();
        return super.attr(attributeKey);
    }

    public Attributes attributes() {
        this.ensureAttributes();
        return super.attributes();
    }

    public Node attr(String attributeKey, String attributeValue) {
        this.ensureAttributes();
        return super.attr(attributeKey, attributeValue);
    }

    public boolean hasAttr(String attributeKey) {
        this.ensureAttributes();
        return super.hasAttr(attributeKey);
    }

    public Node removeAttr(String attributeKey) {
        this.ensureAttributes();
        return super.removeAttr(attributeKey);
    }

    public String absUrl(String attributeKey) {
        this.ensureAttributes();
        return super.absUrl(attributeKey);
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
        if (!super.equals(o)) {
            return false;
        }
        TextNode textNode = (TextNode)o;
        if (this.text == null) {
            if (textNode.text != null) return false;
            return true;
        }
        if (this.text.equals(textNode.text)) return true;
        return false;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.text != null ? this.text.hashCode() : 0);
        return result;
    }
}

