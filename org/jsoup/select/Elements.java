/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.Selector;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Elements
extends ArrayList<Element> {
    public Elements() {
    }

    public Elements(int initialCapacity) {
        super(initialCapacity);
    }

    public Elements(Collection<Element> elements) {
        super(elements);
    }

    public Elements(List<Element> elements) {
        super(elements);
    }

    public /* varargs */ Elements(Element ... elements) {
        super(Arrays.asList(elements));
    }

    @Override
    public Elements clone() {
        Elements clone = new Elements(this.size());
        for (Element e : this) {
            clone.add(e.clone());
        }
        return clone;
    }

    public String attr(String attributeKey) {
        for (Element element : this) {
            if (!element.hasAttr(attributeKey)) continue;
            return element.attr(attributeKey);
        }
        return "";
    }

    public boolean hasAttr(String attributeKey) {
        for (Element element : this) {
            if (!element.hasAttr(attributeKey)) continue;
            return true;
        }
        return false;
    }

    public Elements attr(String attributeKey, String attributeValue) {
        for (Element element : this) {
            element.attr(attributeKey, attributeValue);
        }
        return this;
    }

    public Elements removeAttr(String attributeKey) {
        for (Element element : this) {
            element.removeAttr(attributeKey);
        }
        return this;
    }

    public Elements addClass(String className) {
        for (Element element : this) {
            element.addClass(className);
        }
        return this;
    }

    public Elements removeClass(String className) {
        for (Element element : this) {
            element.removeClass(className);
        }
        return this;
    }

    public Elements toggleClass(String className) {
        for (Element element : this) {
            element.toggleClass(className);
        }
        return this;
    }

    public boolean hasClass(String className) {
        for (Element element : this) {
            if (!element.hasClass(className)) continue;
            return true;
        }
        return false;
    }

    public String val() {
        if (this.size() > 0) {
            return this.first().val();
        }
        return "";
    }

    public Elements val(String value) {
        for (Element element : this) {
            element.val(value);
        }
        return this;
    }

    public String text() {
        StringBuilder sb = new StringBuilder();
        for (Element element : this) {
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(element.text());
        }
        return sb.toString();
    }

    public boolean hasText() {
        for (Element element : this) {
            if (!element.hasText()) continue;
            return true;
        }
        return false;
    }

    public String html() {
        StringBuilder sb = new StringBuilder();
        for (Element element : this) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append(element.html());
        }
        return sb.toString();
    }

    public String outerHtml() {
        StringBuilder sb = new StringBuilder();
        for (Element element : this) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append(element.outerHtml());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.outerHtml();
    }

    public Elements tagName(String tagName) {
        for (Element element : this) {
            element.tagName(tagName);
        }
        return this;
    }

    public Elements html(String html) {
        for (Element element : this) {
            element.html(html);
        }
        return this;
    }

    public Elements prepend(String html) {
        for (Element element : this) {
            element.prepend(html);
        }
        return this;
    }

    public Elements append(String html) {
        for (Element element : this) {
            element.append(html);
        }
        return this;
    }

    public Elements before(String html) {
        for (Element element : this) {
            element.before(html);
        }
        return this;
    }

    public Elements after(String html) {
        for (Element element : this) {
            element.after(html);
        }
        return this;
    }

    public Elements wrap(String html) {
        Validate.notEmpty(html);
        for (Element element : this) {
            element.wrap(html);
        }
        return this;
    }

    public Elements unwrap() {
        for (Element element : this) {
            element.unwrap();
        }
        return this;
    }

    public Elements empty() {
        for (Element element : this) {
            element.empty();
        }
        return this;
    }

    public Elements remove() {
        for (Element element : this) {
            element.remove();
        }
        return this;
    }

    public Elements select(String query) {
        return Selector.select(query, this);
    }

    public Elements not(String query) {
        Elements out = Selector.select(query, this);
        return Selector.filterOut(this, out);
    }

    public Elements eq(int index) {
        return this.size() > index ? new Elements((Element)this.get(index)) : new Elements();
    }

    public boolean is(String query) {
        Elements children = this.select(query);
        return !children.isEmpty();
    }

    public Elements parents() {
        LinkedHashSet<Element> combo = new LinkedHashSet<Element>();
        for (Element e : this) {
            combo.addAll(e.parents());
        }
        return new Elements(combo);
    }

    public Element first() {
        return this.isEmpty() ? null : (Element)this.get(0);
    }

    public Element last() {
        return this.isEmpty() ? null : (Element)this.get(this.size() - 1);
    }

    public Elements traverse(NodeVisitor nodeVisitor) {
        Validate.notNull(nodeVisitor);
        NodeTraversor traversor = new NodeTraversor(nodeVisitor);
        for (Element el : this) {
            traversor.traverse(el);
        }
        return this;
    }

    public List<FormElement> forms() {
        ArrayList<FormElement> forms = new ArrayList<FormElement>();
        for (Element el : this) {
            if (!(el instanceof FormElement)) continue;
            forms.add((FormElement)el);
        }
        return forms;
    }
}

