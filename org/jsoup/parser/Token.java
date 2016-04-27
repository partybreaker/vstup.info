/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;

abstract class Token {
    TokenType type;

    private Token() {
    }

    String tokenType() {
        return this.getClass().getSimpleName();
    }

    abstract Token reset();

    static void reset(StringBuilder sb) {
        if (sb != null) {
            sb.delete(0, sb.length());
        }
    }

    final boolean isDoctype() {
        return this.type == TokenType.Doctype;
    }

    final Doctype asDoctype() {
        return (Doctype)this;
    }

    final boolean isStartTag() {
        return this.type == TokenType.StartTag;
    }

    final StartTag asStartTag() {
        return (StartTag)this;
    }

    final boolean isEndTag() {
        return this.type == TokenType.EndTag;
    }

    final EndTag asEndTag() {
        return (EndTag)this;
    }

    final boolean isComment() {
        return this.type == TokenType.Comment;
    }

    final Comment asComment() {
        return (Comment)this;
    }

    final boolean isCharacter() {
        return this.type == TokenType.Character;
    }

    final Character asCharacter() {
        return (Character)this;
    }

    final boolean isEOF() {
        return this.type == TokenType.EOF;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    static enum TokenType {
        Doctype,
        StartTag,
        EndTag,
        Comment,
        Character,
        EOF;
        

        private TokenType() {
        }
    }

    static final class EOF
    extends Token {
        EOF() {
            super();
            this.type = TokenType.EOF;
        }

        Token reset() {
            return this;
        }
    }

    static final class Character
    extends Token {
        private String data;

        Character() {
            super();
            this.type = TokenType.Character;
        }

        Token reset() {
            this.data = null;
            return this;
        }

        Character data(String data) {
            this.data = data;
            return this;
        }

        String getData() {
            return this.data;
        }

        public String toString() {
            return this.getData();
        }
    }

    static final class Comment
    extends Token {
        final StringBuilder data = new StringBuilder();
        boolean bogus = false;

        Token reset() {
            Comment.reset(this.data);
            this.bogus = false;
            return this;
        }

        Comment() {
            super();
            this.type = TokenType.Comment;
        }

        String getData() {
            return this.data.toString();
        }

        public String toString() {
            return "<!--" + this.getData() + "-->";
        }
    }

    static final class EndTag
    extends Tag {
        EndTag() {
            this.type = TokenType.EndTag;
        }

        public String toString() {
            return "</" + this.name() + ">";
        }
    }

    static final class StartTag
    extends Tag {
        StartTag() {
            this.attributes = new Attributes();
            this.type = TokenType.StartTag;
        }

        Tag reset() {
            super.reset();
            this.attributes = new Attributes();
            return this;
        }

        StartTag nameAttr(String name, Attributes attributes) {
            this.tagName = name;
            this.attributes = attributes;
            return this;
        }

        public String toString() {
            if (this.attributes != null && this.attributes.size() > 0) {
                return "<" + this.name() + " " + this.attributes.toString() + ">";
            }
            return "<" + this.name() + ">";
        }
    }

    static abstract class Tag
    extends Token {
        protected String tagName;
        private String pendingAttributeName;
        private StringBuilder pendingAttributeValue = new StringBuilder();
        private boolean hasPendingAttributeValue = false;
        boolean selfClosing = false;
        Attributes attributes;

        Tag() {
            super();
        }

        Tag reset() {
            this.tagName = null;
            this.pendingAttributeName = null;
            Tag.reset(this.pendingAttributeValue);
            this.hasPendingAttributeValue = false;
            this.selfClosing = false;
            this.attributes = null;
            return this;
        }

        final void newAttribute() {
            if (this.attributes == null) {
                this.attributes = new Attributes();
            }
            if (this.pendingAttributeName != null) {
                Attribute attribute = !this.hasPendingAttributeValue ? new Attribute(this.pendingAttributeName, "") : new Attribute(this.pendingAttributeName, this.pendingAttributeValue.toString());
                this.attributes.put(attribute);
            }
            this.pendingAttributeName = null;
            Tag.reset(this.pendingAttributeValue);
        }

        final void finaliseTag() {
            if (this.pendingAttributeName != null) {
                this.newAttribute();
            }
        }

        final String name() {
            Validate.isFalse(this.tagName == null || this.tagName.length() == 0);
            return this.tagName;
        }

        final Tag name(String name) {
            this.tagName = name;
            return this;
        }

        final boolean isSelfClosing() {
            return this.selfClosing;
        }

        final Attributes getAttributes() {
            return this.attributes;
        }

        final void appendTagName(String append) {
            this.tagName = this.tagName == null ? append : this.tagName.concat(append);
        }

        final void appendTagName(char append) {
            this.appendTagName(String.valueOf(append));
        }

        final void appendAttributeName(String append) {
            this.pendingAttributeName = this.pendingAttributeName == null ? append : this.pendingAttributeName.concat(append);
        }

        final void appendAttributeName(char append) {
            this.appendAttributeName(String.valueOf(append));
        }

        final void appendAttributeValue(String append) {
            this.ensureAttributeValue();
            this.pendingAttributeValue.append(append);
        }

        final void appendAttributeValue(char append) {
            this.ensureAttributeValue();
            this.pendingAttributeValue.append(append);
        }

        final void appendAttributeValue(char[] append) {
            this.ensureAttributeValue();
            this.pendingAttributeValue.append(append);
        }

        private void ensureAttributeValue() {
            this.hasPendingAttributeValue = true;
        }
    }

    static final class Doctype
    extends Token {
        final StringBuilder name = new StringBuilder();
        final StringBuilder publicIdentifier = new StringBuilder();
        final StringBuilder systemIdentifier = new StringBuilder();
        boolean forceQuirks = false;

        Doctype() {
            super();
            this.type = TokenType.Doctype;
        }

        Token reset() {
            Doctype.reset(this.name);
            Doctype.reset(this.publicIdentifier);
            Doctype.reset(this.systemIdentifier);
            this.forceQuirks = false;
            return this;
        }

        String getName() {
            return this.name.toString();
        }

        String getPublicIdentifier() {
            return this.publicIdentifier.toString();
        }

        public String getSystemIdentifier() {
            return this.systemIdentifier.toString();
        }

        public boolean isForceQuirks() {
            return this.forceQuirks;
        }
    }

}

