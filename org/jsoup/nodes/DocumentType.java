/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

public class DocumentType
extends Node {
    private static final String NAME = "name";
    private static final String PUBLIC_ID = "publicId";
    private static final String SYSTEM_ID = "systemId";

    public DocumentType(String name, String publicId, String systemId, String baseUri) {
        super(baseUri);
        this.attr("name", name);
        this.attr("publicId", publicId);
        this.attr("systemId", systemId);
    }

    public String nodeName() {
        return "#doctype";
    }

    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        if (out.syntax() == Document.OutputSettings.Syntax.html && !this.has("publicId") && !this.has("systemId")) {
            accum.append("<!doctype");
        } else {
            accum.append("<!DOCTYPE");
        }
        if (this.has("name")) {
            accum.append(" ").append(this.attr("name"));
        }
        if (this.has("publicId")) {
            accum.append(" PUBLIC \"").append(this.attr("publicId")).append('\"');
        }
        if (this.has("systemId")) {
            accum.append(" \"").append(this.attr("systemId")).append('\"');
        }
        accum.append('>');
    }

    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {
    }

    private boolean has(String attribute) {
        return !StringUtil.isBlank(this.attr(attribute));
    }
}

