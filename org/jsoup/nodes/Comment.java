/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

public class Comment
extends Node {
    private static final String COMMENT_KEY = "comment";

    public Comment(String data, String baseUri) {
        super(baseUri);
        this.attributes.put("comment", data);
    }

    public String nodeName() {
        return "#comment";
    }

    public String getData() {
        return this.attributes.get("comment");
    }

    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        if (out.prettyPrint()) {
            this.indent(accum, depth, out);
        }
        accum.append("<!--").append(this.getData()).append("-->");
    }

    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {
    }

    public String toString() {
        return this.outerHtml();
    }
}

