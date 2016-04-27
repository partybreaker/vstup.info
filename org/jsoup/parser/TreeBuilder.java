/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import java.util.ArrayList;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Token;
import org.jsoup.parser.Tokeniser;

abstract class TreeBuilder {
    CharacterReader reader;
    Tokeniser tokeniser;
    protected Document doc;
    protected ArrayList<Element> stack;
    protected String baseUri;
    protected Token currentToken;
    protected ParseErrorList errors;
    private Token.StartTag start = new Token.StartTag();
    private Token.EndTag end = new Token.EndTag();

    TreeBuilder() {
    }

    protected void initialiseParse(String input, String baseUri, ParseErrorList errors) {
        Validate.notNull(input, "String input must not be null");
        Validate.notNull(baseUri, "BaseURI must not be null");
        this.doc = new Document(baseUri);
        this.reader = new CharacterReader(input);
        this.errors = errors;
        this.tokeniser = new Tokeniser(this.reader, errors);
        this.stack = new ArrayList(32);
        this.baseUri = baseUri;
    }

    Document parse(String input, String baseUri) {
        return this.parse(input, baseUri, ParseErrorList.noTracking());
    }

    Document parse(String input, String baseUri, ParseErrorList errors) {
        this.initialiseParse(input, baseUri, errors);
        this.runParser();
        return this.doc;
    }

    protected void runParser() {
        do {
            Token token = this.tokeniser.read();
            this.process(token);
            token.reset();
        } while (token.type != Token.TokenType.EOF);
    }

    protected abstract boolean process(Token var1);

    protected boolean processStartTag(String name) {
        return this.process(this.start.reset().name(name));
    }

    public boolean processStartTag(String name, Attributes attrs) {
        this.start.reset();
        this.start.nameAttr(name, attrs);
        return this.process(this.start);
    }

    protected boolean processEndTag(String name) {
        return this.process(this.end.reset().name(name));
    }

    protected Element currentElement() {
        int size = this.stack.size();
        return size > 0 ? this.stack.get(size - 1) : null;
    }
}

