/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class FormElement
extends Element {
    private final Elements elements = new Elements();

    public FormElement(Tag tag, String baseUri, Attributes attributes) {
        super(tag, baseUri, attributes);
    }

    public Elements elements() {
        return this.elements;
    }

    public FormElement addElement(Element element) {
        this.elements.add(element);
        return this;
    }

    public Connection submit() {
        String action = this.hasAttr("action") ? this.absUrl("action") : this.baseUri();
        Validate.notEmpty(action, "Could not determine a form action URL for submit. Ensure you set a base URI when parsing.");
        Connection.Method method = this.attr("method").toUpperCase().equals("POST") ? Connection.Method.POST : Connection.Method.GET;
        Connection con = Jsoup.connect(action).data(this.formData()).method(method);
        return con;
    }

    public List<Connection.KeyVal> formData() {
        ArrayList<Connection.KeyVal> data = new ArrayList<Connection.KeyVal>();
        for (Element el : this.elements) {
            String name;
            if (!el.tag().isFormSubmittable() || el.hasAttr("disabled") || (name = el.attr("name")).length() == 0) continue;
            String type = el.attr("type");
            if ("select".equals(el.tagName())) {
                Element option;
                Elements options = el.select("option[selected]");
                boolean set = false;
                for (Element option2 : options) {
                    data.add(HttpConnection.KeyVal.create(name, option2.val()));
                    set = true;
                }
                if (set || (option = el.select("option").first()) == null) continue;
                data.add(HttpConnection.KeyVal.create(name, option.val()));
                continue;
            }
            if ("checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type)) {
                if (!el.hasAttr("checked")) continue;
                String val = el.val().length() > 0 ? el.val() : "on";
                data.add(HttpConnection.KeyVal.create(name, val));
                continue;
            }
            data.add(HttpConnection.KeyVal.create(name, el.val()));
        }
        return data;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}

