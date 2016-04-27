/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.helper;

import java.io.StringWriter;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class W3CDom {
    protected DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    public org.w3c.dom.Document fromJsoup(Document in) {
        Validate.notNull(in);
        try {
            DocumentBuilder builder = this.factory.newDocumentBuilder();
            org.w3c.dom.Document out = builder.newDocument();
            this.convert(in, out);
            return out;
        }
        catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public void convert(Document in, org.w3c.dom.Document out) {
        if (!StringUtil.isBlank(in.location())) {
            out.setDocumentURI(in.location());
        }
        org.jsoup.nodes.Element rootEl = in.child(0);
        NodeTraversor traversor = new NodeTraversor(new W3CBuilder(out));
        traversor.traverse(rootEl);
    }

    public String asString(org.w3c.dom.Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        }
        catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }

    protected class W3CBuilder
    implements NodeVisitor {
        private final org.w3c.dom.Document doc;
        private Element dest;

        public W3CBuilder(org.w3c.dom.Document doc) {
            this.doc = doc;
        }

        public void head(Node source, int depth) {
            if (source instanceof org.jsoup.nodes.Element) {
                org.jsoup.nodes.Element sourceEl = (org.jsoup.nodes.Element)source;
                Element el = this.doc.createElement(sourceEl.tagName());
                this.copyAttributes(sourceEl, el);
                if (this.dest == null) {
                    this.doc.appendChild(el);
                } else {
                    this.dest.appendChild(el);
                }
                this.dest = el;
            } else if (source instanceof TextNode) {
                TextNode sourceText = (TextNode)source;
                Text text = this.doc.createTextNode(sourceText.getWholeText());
                this.dest.appendChild(text);
            } else if (source instanceof org.jsoup.nodes.Comment) {
                org.jsoup.nodes.Comment sourceComment = (org.jsoup.nodes.Comment)source;
                Comment comment = this.doc.createComment(sourceComment.getData());
                this.dest.appendChild(comment);
            } else if (source instanceof DataNode) {
                DataNode sourceData = (DataNode)source;
                Text node = this.doc.createTextNode(sourceData.getWholeData());
                this.dest.appendChild(node);
            }
        }

        public void tail(Node source, int depth) {
            if (source instanceof org.jsoup.nodes.Element && this.dest.getParentNode() instanceof Element) {
                this.dest = (Element)this.dest.getParentNode();
            }
        }

        private void copyAttributes(Node source, Element el) {
            for (Attribute attribute : source.attributes()) {
                el.setAttribute(attribute.getKey(), attribute.getValue());
            }
        }
    }

}

