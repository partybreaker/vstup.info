/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Attributes
implements Iterable<Attribute>,
Cloneable {
    protected static final String dataPrefix = "data-";
    private LinkedHashMap<String, Attribute> attributes = null;

    public String get(String key) {
        Validate.notEmpty(key);
        if (this.attributes == null) {
            return "";
        }
        Attribute attr = this.attributes.get(key.toLowerCase());
        return attr != null ? attr.getValue() : "";
    }

    public void put(String key, String value) {
        Attribute attr = new Attribute(key, value);
        this.put(attr);
    }

    public void put(Attribute attribute) {
        Validate.notNull(attribute);
        if (this.attributes == null) {
            this.attributes = new LinkedHashMap(2);
        }
        this.attributes.put(attribute.getKey(), attribute);
    }

    public void remove(String key) {
        Validate.notEmpty(key);
        if (this.attributes == null) {
            return;
        }
        this.attributes.remove(key.toLowerCase());
    }

    public boolean hasKey(String key) {
        return this.attributes != null && this.attributes.containsKey(key.toLowerCase());
    }

    public int size() {
        if (this.attributes == null) {
            return 0;
        }
        return this.attributes.size();
    }

    public void addAll(Attributes incoming) {
        if (incoming.size() == 0) {
            return;
        }
        if (this.attributes == null) {
            this.attributes = new LinkedHashMap(incoming.size());
        }
        this.attributes.putAll(incoming.attributes);
    }

    @Override
    public Iterator<Attribute> iterator() {
        return this.asList().iterator();
    }

    public List<Attribute> asList() {
        if (this.attributes == null) {
            return Collections.emptyList();
        }
        ArrayList<Attribute> list = new ArrayList<Attribute>(this.attributes.size());
        for (Map.Entry<String, Attribute> entry : this.attributes.entrySet()) {
            list.add(entry.getValue());
        }
        return Collections.unmodifiableList(list);
    }

    public Map<String, String> dataset() {
        return new Dataset();
    }

    public String html() {
        StringBuilder accum = new StringBuilder();
        this.html(accum, new Document("").outputSettings());
        return accum.toString();
    }

    void html(StringBuilder accum, Document.OutputSettings out) {
        if (this.attributes == null) {
            return;
        }
        for (Map.Entry<String, Attribute> entry : this.attributes.entrySet()) {
            Attribute attribute = entry.getValue();
            accum.append(" ");
            attribute.html(accum, out);
        }
    }

    public String toString() {
        return this.html();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Attributes)) {
            return false;
        }
        Attributes that = (Attributes)o;
        if (this.attributes == null) {
            if (that.attributes != null) return false;
            return true;
        }
        if (this.attributes.equals(that.attributes)) return true;
        return false;
    }

    public int hashCode() {
        return this.attributes != null ? this.attributes.hashCode() : 0;
    }

    public Attributes clone() {
        Attributes clone;
        if (this.attributes == null) {
            return new Attributes();
        }
        try {
            clone = (Attributes)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.attributes = new LinkedHashMap(this.attributes.size());
        for (Attribute attribute : this) {
            clone.attributes.put(attribute.getKey(), attribute.clone());
        }
        return clone;
    }

    private static String dataKey(String key) {
        return "data-" + key;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private class Dataset
    extends AbstractMap<String, String> {
        private Dataset() {
            if (Attributes.this.attributes == null) {
                Attributes.this.attributes = new LinkedHashMap(2);
            }
        }

        @Override
        public Set<Map.Entry<String, String>> entrySet() {
            return new EntrySet();
        }

        @Override
        public String put(String key, String value) {
            String dataKey = Attributes.dataKey(key);
            String oldValue = Attributes.this.hasKey(dataKey) ? ((Attribute)Attributes.this.attributes.get(dataKey)).getValue() : null;
            Attribute attr = new Attribute(dataKey, value);
            Attributes.this.attributes.put(dataKey, attr);
            return oldValue;
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        private class DatasetIterator
        implements Iterator<Map.Entry<String, String>> {
            private Iterator<Attribute> attrIter;
            private Attribute attr;

            private DatasetIterator() {
                this.attrIter = Attributes.this.attributes.values().iterator();
            }

            @Override
            public boolean hasNext() {
                while (this.attrIter.hasNext()) {
                    this.attr = this.attrIter.next();
                    if (!this.attr.isDataAttribute()) continue;
                    return true;
                }
                return false;
            }

            @Override
            public Map.Entry<String, String> next() {
                return new Attribute(this.attr.getKey().substring("data-".length()), this.attr.getValue());
            }

            @Override
            public void remove() {
                Attributes.this.attributes.remove(this.attr.getKey());
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        private class EntrySet
        extends AbstractSet<Map.Entry<String, String>> {
            private EntrySet() {
            }

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                return new DatasetIterator();
            }

            @Override
            public int size() {
                int count = 0;
                DatasetIterator iter = new DatasetIterator();
                while (iter.hasNext()) {
                    ++count;
                }
                return count;
            }
        }

    }

}

