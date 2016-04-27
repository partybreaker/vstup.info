/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.examples;

import java.io.IOException;
import java.io.PrintStream;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class HtmlToPlainText {
    private static final String userAgent = "Mozilla/5.0 (jsoup)";
    private static final int timeout = 5000;

    public static /* varargs */ void main(String ... args) throws IOException {
        Validate.isTrue(args.length == 1 || args.length == 2, "usage: java -cp jsoup.jar org.jsoup.examples.HtmlToPlainText url [selector]");
        String url = args[0];
        String selector = args.length == 2 ? args[1] : null;
        Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (jsoup)").timeout(5000).get();
        HtmlToPlainText formatter = new HtmlToPlainText();
        if (selector != null) {
            Elements elements = doc.select(selector);
            for (Element element : elements) {
                String plainText = formatter.getPlainText(element);
                System.out.println(plainText);
            }
        } else {
            String plainText = formatter.getPlainText(doc);
            System.out.println(plainText);
        }
    }

    public String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor traversor = new NodeTraversor(formatter);
        traversor.traverse(element);
        return formatter.toString();
    }

    private class FormattingVisitor
    implements NodeVisitor {
        private static final int maxWidth = 80;
        private int width;
        private StringBuilder accum;

        private FormattingVisitor() {
            this.width = 0;
            this.accum = new StringBuilder();
        }

        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                this.append(((TextNode)node).text());
            } else if (name.equals("li")) {
                this.append("\n * ");
            } else if (name.equals("dt")) {
                this.append("  ");
            } else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr")) {
                this.append("\n");
            }
        }

        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")) {
                this.append("\n");
            } else if (name.equals("a")) {
                this.append(String.format(" <%s>", node.absUrl("href")));
            }
        }

        private void append(String text) {
            if (text.startsWith("\n")) {
                this.width = 0;
            }
            if (text.equals(" ") && (this.accum.length() == 0 || StringUtil.in(this.accum.substring(this.accum.length() - 1), " ", "\n"))) {
                return;
            }
            if (text.length() + this.width > 80) {
                String[] words = text.split("\\s+");
                for (int i = 0; i < words.length; ++i) {
                    boolean last;
                    String word = words[i];
                    boolean bl = last = i == words.length - 1;
                    if (!last) {
                        word = word + " ";
                    }
                    if (word.length() + this.width > 80) {
                        this.accum.append("\n").append(word);
                        this.width = word.length();
                        continue;
                    }
                    this.accum.append(word);
                    this.width += word.length();
                }
            } else {
                this.accum.append(text);
                this.width += text.length();
            }
        }

        public String toString() {
            return this.accum.toString();
        }
    }

}

