/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class Collector {
    private Collector() {
    }

    public static Elements collect(Evaluator eval, Element root) {
        Elements elements = new Elements();
        new NodeTraversor(new Accumulator(root, elements, eval)).traverse(root);
        return elements;
    }

    private static class Accumulator
    implements NodeVisitor {
        private final Element root;
        private final Elements elements;
        private final Evaluator eval;

        Accumulator(Element root, Elements elements, Evaluator eval) {
            this.root = root;
            this.elements = elements;
            this.eval = eval;
        }

        public void head(Node node, int depth) {
            Element el;
            if (node instanceof Element && this.eval.matches(this.root, el = (Element)node)) {
                this.elements.add(el);
            }
        }

        public void tail(Node node, int depth) {
        }
    }

}

