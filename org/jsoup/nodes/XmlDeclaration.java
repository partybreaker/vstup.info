/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

public class XmlDeclaration
extends Node {
    static final String DECL_KEY = "declaration";
    private final boolean isProcessingInstruction;

    public XmlDeclaration(String data, String baseUri, boolean isProcessingInstruction) {
        super(baseUri);
        this.attributes.put("declaration", data);
        this.isProcessingInstruction = isProcessingInstruction;
    }

    public String nodeName() {
        return "#declaration";
    }

    public String getWholeDeclaration() {
        String decl = this.attributes.get("declaration");
        if (decl.equals("xml") && this.attributes.size() > 1) {
            String encoding;
            StringBuilder sb = new StringBuilder(decl);
            String version = this.attributes.get("version");
            if (version != null) {
                sb.append(" version=\"").append(version).append("\"");
            }
            if ((encoding = this.attributes.get("encoding")) != null) {
                sb.append(" encoding=\"").append(encoding).append("\"");
            }
            return sb.toString();
        }
        return this.attributes.get("declaration");
    }

    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        accum.append("<").append(this.isProcessingInstruction ? "!" : "?").append(this.getWholeDeclaration()).append(">");
    }

    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {
    }

    public String toString() {
        return this.outerHtml();
    }
}

