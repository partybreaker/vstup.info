/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import java.util.Arrays;
import java.util.Map;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Attribute
implements Map.Entry<String, String>,
Cloneable {
    private static final String[] booleanAttributes = new String[]{"allowfullscreen", "async", "autofocus", "checked", "compact", "declare", "default", "defer", "disabled", "formnovalidate", "hidden", "inert", "ismap", "itemscope", "multiple", "muted", "nohref", "noresize", "noshade", "novalidate", "nowrap", "open", "readonly", "required", "reversed", "seamless", "selected", "sortable", "truespeed", "typemustmatch"};
    private String key;
    private String value;

    public Attribute(String key, String value) {
        Validate.notEmpty(key);
        Validate.notNull(value);
        this.key = key.trim().toLowerCase();
        this.value = value;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        Validate.notEmpty(key);
        this.key = key.trim().toLowerCase();
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String setValue(String value) {
        Validate.notNull(value);
        String old = this.value;
        this.value = value;
        return old;
    }

    public String html() {
        StringBuilder accum = new StringBuilder();
        this.html(accum, new Document("").outputSettings());
        return accum.toString();
    }

    protected void html(StringBuilder accum, Document.OutputSettings out) {
        accum.append(this.key);
        if (!this.shouldCollapseAttribute(out)) {
            accum.append("=\"");
            Entities.escape(accum, this.value, out, true, false, false);
            accum.append('\"');
        }
    }

    public String toString() {
        return this.html();
    }

    public static Attribute createFromEncoded(String unencodedKey, String encodedValue) {
        String value = Entities.unescape(encodedValue, true);
        return new Attribute(unencodedKey, value);
    }

    protected boolean isDataAttribute() {
        return this.key.startsWith("data-") && this.key.length() > "data-".length();
    }

    protected final boolean shouldCollapseAttribute(Document.OutputSettings out) {
        return ("".equals(this.value) || this.value.equalsIgnoreCase(this.key)) && out.syntax() == Document.OutputSettings.Syntax.html && Arrays.binarySearch(booleanAttributes, this.key) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Attribute)) {
            return false;
        }
        Attribute attribute = (Attribute)o;
        if (this.key != null ? !this.key.equals(attribute.key) : attribute.key != null) {
            return false;
        }
        if (this.value != null ? !this.value.equals(attribute.value) : attribute.value != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = this.key != null ? this.key.hashCode() : 0;
        result = 31 * result + (this.value != null ? this.value.hashCode() : 0);
        return result;
    }

    public Attribute clone() {
        try {
            return (Attribute)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

