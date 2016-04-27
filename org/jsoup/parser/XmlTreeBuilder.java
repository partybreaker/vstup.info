/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Tag;
import org.jsoup.parser.Token;
import org.jsoup.parser.Tokeniser;
import org.jsoup.parser.TreeBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class XmlTreeBuilder
extends TreeBuilder {
    @Override
    protected void initialiseParse(String input, String baseUri, ParseErrorList errors) {
        super.initialiseParse(input, baseUri, errors);
        this.stack.add(this.doc);
        this.doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
    }

    @Override
    protected boolean process(Token token) {
        switch (token.type) {
            case StartTag: {
                this.insert(token.asStartTag());
                break;
            }
            case EndTag: {
                this.popStackToClose(token.asEndTag());
                break;
            }
            case Comment: {
                this.insert(token.asComment());
                break;
            }
            case Character: {
                this.insert(token.asCharacter());
                break;
            }
            case Doctype: {
                this.insert(token.asDoctype());
                break;
            }
            case EOF: {
                break;
            }
            default: {
                Validate.fail("Unexpected token type: " + (Object)((Object)token.type));
            }
        }
        return true;
    }

    private void insertNode(Node node) {
        this.currentElement().appendChild(node);
    }

    Element insert(Token.StartTag startTag) {
        Tag tag = Tag.valueOf(startTag.name());
        Element el = new Element(tag, this.baseUri, startTag.attributes);
        this.insertNode(el);
        if (startTag.isSelfClosing()) {
            this.tokeniser.acknowledgeSelfClosingFlag();
            if (!tag.isKnownTag()) {
                tag.setSelfClosing();
            }
        } else {
            this.stack.add(el);
        }
        return el;
    }

    void insert(Token.Comment commentToken) {
        String data;
        Comment comment;
        Node insert = comment = new Comment(commentToken.getData(), this.baseUri);
        if (commentToken.bogus && (data = comment.getData()).length() > 1 && (data.startsWith("!") || data.startsWith("?"))) {
            String declaration = data.substring(1);
            insert = new XmlDeclaration(declaration, comment.baseUri(), data.startsWith("!"));
        }
        this.insertNode(insert);
    }

    void insert(Token.Character characterToken) {
        TextNode node = new TextNode(characterToken.getData(), this.baseUri);
        this.insertNode(node);
    }

    void insert(Token.Doctype d) {
        DocumentType doctypeNode = new DocumentType(d.getName(), d.getPublicIdentifier(), d.getSystemIdentifier(), this.baseUri);
        this.insertNode(doctypeNode);
    }

    private void popStackToClose(Token.EndTag endTag) {
        Element next;
        int pos;
        String elName = endTag.name();
        Element firstFound = null;
        for (pos = this.stack.size() - 1; pos >= 0; --pos) {
            next = (Element)this.stack.get(pos);
            if (!next.nodeName().equals(elName)) continue;
            firstFound = next;
            break;
        }
        if (firstFound == null) {
            return;
        }
        for (pos = this.stack.size() - 1; pos >= 0; --pos) {
            next = (Element)this.stack.get(pos);
            this.stack.remove(pos);
            if (next == firstFound) break;
        }
    }

    List<Node> parseFragment(String inputFragment, String baseUri, ParseErrorList errors) {
        this.initialiseParse(inputFragment, baseUri, errors);
        this.runParser();
        return this.doc.childNodes();
    }

}

