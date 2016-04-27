/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;

public class DataNode
extends Node {
    private static final String DATA_KEY = "data";

    public DataNode(String data, String baseUri) {
        super(baseUri);
        this.attributes.put("data", data);
    }

    public String nodeName() {
        return "#data";
    }

    public String getWholeData() {
        return this.attributes.get("data");
    }

    public DataNode setWholeData(String data) {
        this.attributes.put("data", data);
        return this;
    }

    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        accum.append(this.getWholeData());
    }

    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {
    }

    public String toString() {
        return this.outerHtml();
    }

    public static DataNode createFromEncoded(String encodedData, String baseUri) {
        String data = Entities.unescape(encodedData);
        return new DataNode(data, baseUri);
    }
}

