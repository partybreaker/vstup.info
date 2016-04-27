/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.select;

import java.util.Collection;
import java.util.LinkedHashSet;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.select.Collector;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Selector {
    private final Evaluator evaluator;
    private final Element root;

    private Selector(String query, Element root) {
        Validate.notNull(query);
        query = query.trim();
        Validate.notEmpty(query);
        Validate.notNull(root);
        this.evaluator = QueryParser.parse(query);
        this.root = root;
    }

    private Selector(Evaluator evaluator, Element root) {
        Validate.notNull(evaluator);
        Validate.notNull(root);
        this.evaluator = evaluator;
        this.root = root;
    }

    public static Elements select(String query, Element root) {
        return new Selector(query, root).select();
    }

    public static Elements select(Evaluator evaluator, Element root) {
        return new Selector(evaluator, root).select();
    }

    public static Elements select(String query, Iterable<Element> roots) {
        Validate.notEmpty(query);
        Validate.notNull(roots);
        Evaluator evaluator = QueryParser.parse(query);
        LinkedHashSet<Element> elements = new LinkedHashSet<Element>();
        for (Element root : roots) {
            elements.addAll(Selector.select(evaluator, root));
        }
        return new Elements(elements);
    }

    private Elements select() {
        return Collector.collect(this.evaluator, this.root);
    }

    static Elements filterOut(Collection<Element> elements, Collection<Element> outs) {
        Elements output = new Elements();
        for (Element el : elements) {
            boolean found = false;
            for (Element out : outs) {
                if (!el.equals(out)) continue;
                found = true;
                break;
            }
            if (found) continue;
            output.add(el);
        }
        return output;
    }

    public static class SelectorParseException
    extends IllegalStateException {
        public /* varargs */ SelectorParseException(String msg, Object ... params) {
            super(String.format(msg, params));
        }
    }

}

