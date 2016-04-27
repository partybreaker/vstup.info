/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.Tag;
import org.jsoup.parser.Token;
import org.jsoup.parser.Tokeniser;
import org.jsoup.parser.TokeniserState;
import org.jsoup.parser.TreeBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
enum HtmlTreeBuilderState {
    Initial{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (HtmlTreeBuilderState.isWhitespace(t)) {
                return true;
            }
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else if (t.isDoctype()) {
                Token.Doctype d = t.asDoctype();
                DocumentType doctype = new DocumentType(d.getName(), d.getPublicIdentifier(), d.getSystemIdentifier(), tb.getBaseUri());
                tb.getDocument().appendChild(doctype);
                if (d.isForceQuirks()) {
                    tb.getDocument().quirksMode(Document.QuirksMode.quirks);
                }
                tb.transition(BeforeHtml);
            } else {
                tb.transition(BeforeHtml);
                return tb.process(t);
            }
            return true;
        }
    }
    ,
    BeforeHtml{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isDoctype()) {
                tb.error(this);
                return false;
            }
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else {
                if (HtmlTreeBuilderState.isWhitespace(t)) {
                    return true;
                }
                if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                    tb.insert(t.asStartTag());
                    tb.transition(BeforeHead);
                } else {
                    if (t.isEndTag() && StringUtil.in(t.asEndTag().name(), "head", "body", "html", "br")) {
                        return this.anythingElse(t, tb);
                    }
                    if (t.isEndTag()) {
                        tb.error(this);
                        return false;
                    }
                    return this.anythingElse(t, tb);
                }
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.insertStartTag("html");
            tb.transition(BeforeHead);
            return tb.process(t);
        }
    }
    ,
    BeforeHead{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (HtmlTreeBuilderState.isWhitespace(t)) {
                return true;
            }
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else {
                if (t.isDoctype()) {
                    tb.error(this);
                    return false;
                }
                if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                    return InBody.process(t, tb);
                }
                if (t.isStartTag() && t.asStartTag().name().equals("head")) {
                    Element head = tb.insert(t.asStartTag());
                    tb.setHeadElement(head);
                    tb.transition(InHead);
                } else {
                    if (t.isEndTag() && StringUtil.in(t.asEndTag().name(), "head", "body", "html", "br")) {
                        tb.processStartTag("head");
                        return tb.process(t);
                    }
                    if (t.isEndTag()) {
                        tb.error(this);
                        return false;
                    }
                    tb.processStartTag("head");
                    return tb.process(t);
                }
            }
            return true;
        }
    }
    ,
    InHead{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (HtmlTreeBuilderState.isWhitespace(t)) {
                tb.insert(t.asCharacter());
                return true;
            }
            switch (t.type) {
                case Comment: {
                    tb.insert(t.asComment());
                    break;
                }
                case Doctype: {
                    tb.error(this);
                    return false;
                }
                case StartTag: {
                    Token.StartTag start = t.asStartTag();
                    String name = start.name();
                    if (name.equals("html")) {
                        return InBody.process(t, tb);
                    }
                    if (StringUtil.in(name, "base", "basefont", "bgsound", "command", "link")) {
                        Element el = tb.insertEmpty(start);
                        if (!name.equals("base") || !el.hasAttr("href")) break;
                        tb.maybeSetBaseUri(el);
                        break;
                    }
                    if (name.equals("meta")) {
                        Element meta = tb.insertEmpty(start);
                        break;
                    }
                    if (name.equals("title")) {
                        HtmlTreeBuilderState.handleRcData(start, tb);
                        break;
                    }
                    if (StringUtil.in(name, "noframes", "style")) {
                        HtmlTreeBuilderState.handleRawtext(start, tb);
                        break;
                    }
                    if (name.equals("noscript")) {
                        tb.insert(start);
                        tb.transition(InHeadNoscript);
                        break;
                    }
                    if (name.equals("script")) {
                        tb.tokeniser.transition(TokeniserState.ScriptData);
                        tb.markInsertionMode();
                        tb.transition(Text);
                        tb.insert(start);
                        break;
                    }
                    if (name.equals("head")) {
                        tb.error(this);
                        return false;
                    }
                    return this.anythingElse(t, tb);
                }
                case EndTag: {
                    Token.EndTag end = t.asEndTag();
                    String name = end.name();
                    if (name.equals("head")) {
                        tb.pop();
                        tb.transition(AfterHead);
                        break;
                    }
                    if (StringUtil.in(name, "body", "html", "br")) {
                        return this.anythingElse(t, tb);
                    }
                    tb.error(this);
                    return false;
                }
                default: {
                    return this.anythingElse(t, tb);
                }
            }
            return true;
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            tb.processEndTag("head");
            return tb.process(t);
        }
    }
    ,
    InHeadNoscript{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isDoctype()) {
                tb.error(this);
            } else {
                if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                    return tb.process(t, InBody);
                }
                if (t.isEndTag() && t.asEndTag().name().equals("noscript")) {
                    tb.pop();
                    tb.transition(InHead);
                } else {
                    if (HtmlTreeBuilderState.isWhitespace(t) || t.isComment() || t.isStartTag() && StringUtil.in(t.asStartTag().name(), "basefont", "bgsound", "link", "meta", "noframes", "style")) {
                        return tb.process(t, InHead);
                    }
                    if (t.isEndTag() && t.asEndTag().name().equals("br")) {
                        return this.anythingElse(t, tb);
                    }
                    if (t.isStartTag() && StringUtil.in(t.asStartTag().name(), "head", "noscript") || t.isEndTag()) {
                        tb.error(this);
                        return false;
                    }
                    return this.anythingElse(t, tb);
                }
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.error(this);
            tb.insert(new Token.Character().data(t.toString()));
            return true;
        }
    }
    ,
    AfterHead{

        /*
         * Enabled aggressive block sorting
         */
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (HtmlTreeBuilderState.isWhitespace(t)) {
                tb.insert(t.asCharacter());
                return true;
            }
            if (t.isComment()) {
                tb.insert(t.asComment());
                return true;
            }
            if (t.isDoctype()) {
                tb.error(this);
                return true;
            }
            if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.name();
                if (name.equals("html")) {
                    return tb.process(t, InBody);
                }
                if (name.equals("body")) {
                    tb.insert(startTag);
                    tb.framesetOk(false);
                    tb.transition(InBody);
                    return true;
                }
                if (name.equals("frameset")) {
                    tb.insert(startTag);
                    tb.transition(InFrameset);
                    return true;
                }
                if (StringUtil.in(name, "base", "basefont", "bgsound", "link", "meta", "noframes", "script", "style", "title")) {
                    tb.error(this);
                    Element head = tb.getHeadElement();
                    tb.push(head);
                    tb.process(t, InHead);
                    tb.removeFromStack(head);
                    return true;
                }
                if (name.equals("head")) {
                    tb.error(this);
                    return false;
                }
                this.anythingElse(t, tb);
                return true;
            }
            if (!t.isEndTag()) {
                this.anythingElse(t, tb);
                return true;
            }
            if (StringUtil.in(t.asEndTag().name(), "body", "html")) {
                this.anythingElse(t, tb);
                return true;
            }
            tb.error(this);
            return false;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.processStartTag("body");
            tb.framesetOk(true);
            return tb.process(t);
        }
    }
    ,
    InBody{

        boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case Character: {
                    Token.Character c = t.asCharacter();
                    if (c.getData().equals(nullString)) {
                        tb.error(this);
                        return false;
                    }
                    if (tb.framesetOk() && HtmlTreeBuilderState.isWhitespace(c)) {
                        tb.reconstructFormattingElements();
                        tb.insert(c);
                        break;
                    }
                    tb.reconstructFormattingElements();
                    tb.insert(c);
                    tb.framesetOk(false);
                    break;
                }
                case Comment: {
                    tb.insert(t.asComment());
                    break;
                }
                case Doctype: {
                    tb.error(this);
                    return false;
                }
                case StartTag: {
                    Token.StartTag startTag = t.asStartTag();
                    String name = startTag.name();
                    if (name.equals("html")) {
                        tb.error(this);
                        Element html = tb.getStack().get(0);
                        for (Attribute attribute : startTag.getAttributes()) {
                            if (html.hasAttr(attribute.getKey())) continue;
                            html.attributes().put(attribute);
                        }
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartToHead)) {
                        return tb.process(t, InHead);
                    }
                    if (name.equals("body")) {
                        tb.error(this);
                        ArrayList<Element> stack = tb.getStack();
                        if (stack.size() == 1 || stack.size() > 2 && !stack.get(1).nodeName().equals("body")) {
                            return false;
                        }
                        tb.framesetOk(false);
                        Element body = stack.get(1);
                        for (Attribute attribute : startTag.getAttributes()) {
                            if (body.hasAttr(attribute.getKey())) continue;
                            body.attributes().put(attribute);
                        }
                        break;
                    }
                    if (name.equals("frameset")) {
                        tb.error(this);
                        ArrayList<Element> stack = tb.getStack();
                        if (stack.size() == 1 || stack.size() > 2 && !stack.get(1).nodeName().equals("body")) {
                            return false;
                        }
                        if (!tb.framesetOk()) {
                            return false;
                        }
                        Element second = stack.get(1);
                        if (second.parent() != null) {
                            second.remove();
                        }
                        while (stack.size() > 1) {
                            stack.remove(stack.size() - 1);
                        }
                        tb.insert(startTag);
                        tb.transition(InFrameset);
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartPClosers)) {
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insert(startTag);
                        break;
                    }
                    if (StringUtil.in(name, Headings)) {
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        if (StringUtil.in(tb.currentElement().nodeName(), Headings)) {
                            tb.error(this);
                            tb.pop();
                        }
                        tb.insert(startTag);
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartPreListing)) {
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insert(startTag);
                        tb.framesetOk(false);
                        break;
                    }
                    if (name.equals("form")) {
                        if (tb.getFormElement() != null) {
                            tb.error(this);
                            return false;
                        }
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insertForm(startTag, true);
                        break;
                    }
                    if (name.equals("li")) {
                        tb.framesetOk(false);
                        ArrayList<Element> stack = tb.getStack();
                        for (int i = stack.size() - 1; i > 0; --i) {
                            Element el = stack.get(i);
                            if (el.nodeName().equals("li")) {
                                tb.processEndTag("li");
                                break;
                            }
                            if (tb.isSpecial(el) && !StringUtil.in(el.nodeName(), InBodyStartLiBreakers)) break;
                        }
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insert(startTag);
                        break;
                    }
                    if (StringUtil.in(name, DdDt)) {
                        tb.framesetOk(false);
                        ArrayList<Element> stack = tb.getStack();
                        for (int i = stack.size() - 1; i > 0; --i) {
                            Element el = stack.get(i);
                            if (StringUtil.in(el.nodeName(), DdDt)) {
                                tb.processEndTag(el.nodeName());
                                break;
                            }
                            if (tb.isSpecial(el) && !StringUtil.in(el.nodeName(), InBodyStartLiBreakers)) break;
                        }
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insert(startTag);
                        break;
                    }
                    if (name.equals("plaintext")) {
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insert(startTag);
                        tb.tokeniser.transition(TokeniserState.PLAINTEXT);
                        break;
                    }
                    if (name.equals("button")) {
                        if (tb.inButtonScope("button")) {
                            tb.error(this);
                            tb.processEndTag("button");
                            tb.process(startTag);
                            break;
                        }
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        tb.framesetOk(false);
                        break;
                    }
                    if (name.equals("a")) {
                        if (tb.getActiveFormattingElement("a") != null) {
                            tb.error(this);
                            tb.processEndTag("a");
                            Element remainingA = tb.getFromStack("a");
                            if (remainingA != null) {
                                tb.removeFromActiveFormattingElements(remainingA);
                                tb.removeFromStack(remainingA);
                            }
                        }
                        tb.reconstructFormattingElements();
                        Element a = tb.insert(startTag);
                        tb.pushActiveFormattingElements(a);
                        break;
                    }
                    if (StringUtil.in(name, Formatters)) {
                        tb.reconstructFormattingElements();
                        Element el = tb.insert(startTag);
                        tb.pushActiveFormattingElements(el);
                        break;
                    }
                    if (name.equals("nobr")) {
                        tb.reconstructFormattingElements();
                        if (tb.inScope("nobr")) {
                            tb.error(this);
                            tb.processEndTag("nobr");
                            tb.reconstructFormattingElements();
                        }
                        Element el = tb.insert(startTag);
                        tb.pushActiveFormattingElements(el);
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartApplets)) {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        tb.insertMarkerToFormattingElements();
                        tb.framesetOk(false);
                        break;
                    }
                    if (name.equals("table")) {
                        if (tb.getDocument().quirksMode() != Document.QuirksMode.quirks && tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insert(startTag);
                        tb.framesetOk(false);
                        tb.transition(InTable);
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartEmptyFormatters)) {
                        tb.reconstructFormattingElements();
                        tb.insertEmpty(startTag);
                        tb.framesetOk(false);
                        break;
                    }
                    if (name.equals("input")) {
                        tb.reconstructFormattingElements();
                        Element el = tb.insertEmpty(startTag);
                        if (el.attr("type").equalsIgnoreCase("hidden")) break;
                        tb.framesetOk(false);
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartMedia)) {
                        tb.insertEmpty(startTag);
                        break;
                    }
                    if (name.equals("hr")) {
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.insertEmpty(startTag);
                        tb.framesetOk(false);
                        break;
                    }
                    if (name.equals("image")) {
                        if (tb.getFromStack("svg") == null) {
                            return tb.process(startTag.name("img"));
                        }
                        tb.insert(startTag);
                        break;
                    }
                    if (name.equals("isindex")) {
                        tb.error(this);
                        if (tb.getFormElement() != null) {
                            return false;
                        }
                        tb.tokeniser.acknowledgeSelfClosingFlag();
                        tb.processStartTag("form");
                        if (startTag.attributes.hasKey("action")) {
                            FormElement form = tb.getFormElement();
                            form.attr("action", startTag.attributes.get("action"));
                        }
                        tb.processStartTag("hr");
                        tb.processStartTag("label");
                        String prompt = startTag.attributes.hasKey("prompt") ? startTag.attributes.get("prompt") : "This is a searchable index. Enter search keywords: ";
                        tb.process(new Token.Character().data(prompt));
                        Attributes inputAttribs = new Attributes();
                        for (Attribute attr : startTag.attributes) {
                            if (StringUtil.in(attr.getKey(), InBodyStartInputAttribs)) continue;
                            inputAttribs.put(attr);
                        }
                        inputAttribs.put("name", "isindex");
                        tb.processStartTag("input", inputAttribs);
                        tb.processEndTag("label");
                        tb.processStartTag("hr");
                        tb.processEndTag("form");
                        break;
                    }
                    if (name.equals("textarea")) {
                        tb.insert(startTag);
                        tb.tokeniser.transition(TokeniserState.Rcdata);
                        tb.markInsertionMode();
                        tb.framesetOk(false);
                        tb.transition(Text);
                        break;
                    }
                    if (name.equals("xmp")) {
                        if (tb.inButtonScope("p")) {
                            tb.processEndTag("p");
                        }
                        tb.reconstructFormattingElements();
                        tb.framesetOk(false);
                        HtmlTreeBuilderState.handleRawtext(startTag, tb);
                        break;
                    }
                    if (name.equals("iframe")) {
                        tb.framesetOk(false);
                        HtmlTreeBuilderState.handleRawtext(startTag, tb);
                        break;
                    }
                    if (name.equals("noembed")) {
                        HtmlTreeBuilderState.handleRawtext(startTag, tb);
                        break;
                    }
                    if (name.equals("select")) {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        tb.framesetOk(false);
                        HtmlTreeBuilderState state = tb.state();
                        if (state.equals((Object)InTable) || state.equals((Object)InCaption) || state.equals((Object)InTableBody) || state.equals((Object)InRow) || state.equals((Object)InCell)) {
                            tb.transition(InSelectInTable);
                            break;
                        }
                        tb.transition(InSelect);
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartOptions)) {
                        if (tb.currentElement().nodeName().equals("option")) {
                            tb.processEndTag("option");
                        }
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartRuby)) {
                        if (!tb.inScope("ruby")) break;
                        tb.generateImpliedEndTags();
                        if (!tb.currentElement().nodeName().equals("ruby")) {
                            tb.error(this);
                            tb.popStackToBefore("ruby");
                        }
                        tb.insert(startTag);
                        break;
                    }
                    if (name.equals("math")) {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        tb.tokeniser.acknowledgeSelfClosingFlag();
                        break;
                    }
                    if (name.equals("svg")) {
                        tb.reconstructFormattingElements();
                        tb.insert(startTag);
                        tb.tokeniser.acknowledgeSelfClosingFlag();
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartDrop)) {
                        tb.error(this);
                        return false;
                    }
                    tb.reconstructFormattingElements();
                    tb.insert(startTag);
                    break;
                }
                case EndTag: {
                    Token.EndTag endTag = t.asEndTag();
                    String name = endTag.name();
                    if (name.equals("body")) {
                        if (!tb.inScope("body")) {
                            tb.error(this);
                            return false;
                        }
                        tb.transition(AfterBody);
                        break;
                    }
                    if (name.equals("html")) {
                        boolean notIgnored = tb.processEndTag("body");
                        if (!notIgnored) break;
                        return tb.process(endTag);
                    }
                    if (StringUtil.in(name, InBodyEndClosers)) {
                        if (!tb.inScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.generateImpliedEndTags();
                        if (!tb.currentElement().nodeName().equals(name)) {
                            tb.error(this);
                        }
                        tb.popStackToClose(name);
                        break;
                    }
                    if (name.equals("form")) {
                        FormElement currentForm = tb.getFormElement();
                        tb.setFormElement(null);
                        if (currentForm == null || !tb.inScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.generateImpliedEndTags();
                        if (!tb.currentElement().nodeName().equals(name)) {
                            tb.error(this);
                        }
                        tb.removeFromStack(currentForm);
                        break;
                    }
                    if (name.equals("p")) {
                        if (!tb.inButtonScope(name)) {
                            tb.error(this);
                            tb.processStartTag(name);
                            return tb.process(endTag);
                        }
                        tb.generateImpliedEndTags(name);
                        if (!tb.currentElement().nodeName().equals(name)) {
                            tb.error(this);
                        }
                        tb.popStackToClose(name);
                        break;
                    }
                    if (name.equals("li")) {
                        if (!tb.inListItemScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.generateImpliedEndTags(name);
                        if (!tb.currentElement().nodeName().equals(name)) {
                            tb.error(this);
                        }
                        tb.popStackToClose(name);
                        break;
                    }
                    if (StringUtil.in(name, DdDt)) {
                        if (!tb.inScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.generateImpliedEndTags(name);
                        if (!tb.currentElement().nodeName().equals(name)) {
                            tb.error(this);
                        }
                        tb.popStackToClose(name);
                        break;
                    }
                    if (StringUtil.in(name, Headings)) {
                        if (!tb.inScope(Headings)) {
                            tb.error(this);
                            return false;
                        }
                        tb.generateImpliedEndTags(name);
                        if (!tb.currentElement().nodeName().equals(name)) {
                            tb.error(this);
                        }
                        tb.popStackToClose(Headings);
                        break;
                    }
                    if (name.equals("sarcasm")) {
                        return this.anyOtherEndTag(t, tb);
                    }
                    if (StringUtil.in(name, InBodyEndAdoptionFormatters)) {
                        for (int i = 0; i < 8; ++i) {
                            Node[] childNodes;
                            Element formatEl = tb.getActiveFormattingElement(name);
                            if (formatEl == null) {
                                return this.anyOtherEndTag(t, tb);
                            }
                            if (!tb.onStack(formatEl)) {
                                tb.error(this);
                                tb.removeFromActiveFormattingElements(formatEl);
                                return true;
                            }
                            if (!tb.inScope(formatEl.nodeName())) {
                                tb.error(this);
                                return false;
                            }
                            if (tb.currentElement() != formatEl) {
                                tb.error(this);
                            }
                            Element furthestBlock = null;
                            Element commonAncestor = null;
                            boolean seenFormattingElement = false;
                            ArrayList<Element> stack = tb.getStack();
                            int stackSize = stack.size();
                            for (int si = 0; si < stackSize && si < 64; ++si) {
                                Element el = stack.get(si);
                                if (el == formatEl) {
                                    commonAncestor = stack.get(si - 1);
                                    seenFormattingElement = true;
                                    continue;
                                }
                                if (!seenFormattingElement || !tb.isSpecial(el)) continue;
                                furthestBlock = el;
                                break;
                            }
                            if (furthestBlock == null) {
                                tb.popStackToClose(formatEl.nodeName());
                                tb.removeFromActiveFormattingElements(formatEl);
                                return true;
                            }
                            Element node = furthestBlock;
                            Element lastNode = furthestBlock;
                            for (int j = 0; j < 3; ++j) {
                                if (tb.onStack(node)) {
                                    node = tb.aboveOnStack(node);
                                }
                                if (!tb.isInActiveFormattingElements(node)) {
                                    tb.removeFromStack(node);
                                    continue;
                                }
                                if (node == formatEl) break;
                                Element replacement = new Element(Tag.valueOf(node.nodeName()), tb.getBaseUri());
                                tb.replaceActiveFormattingElement(node, replacement);
                                tb.replaceOnStack(node, replacement);
                                node = replacement;
                                if (lastNode == furthestBlock) {
                                    // empty if block
                                }
                                if (lastNode.parent() != null) {
                                    lastNode.remove();
                                }
                                node.appendChild(lastNode);
                                lastNode = node;
                            }
                            if (StringUtil.in(commonAncestor.nodeName(), InBodyEndTableFosters)) {
                                if (lastNode.parent() != null) {
                                    lastNode.remove();
                                }
                                tb.insertInFosterParent(lastNode);
                            } else {
                                if (lastNode.parent() != null) {
                                    lastNode.remove();
                                }
                                commonAncestor.appendChild(lastNode);
                            }
                            Element adopter = new Element(formatEl.tag(), tb.getBaseUri());
                            adopter.attributes().addAll(formatEl.attributes());
                            for (Node childNode : childNodes = furthestBlock.childNodes().toArray(new Node[furthestBlock.childNodeSize()])) {
                                adopter.appendChild(childNode);
                            }
                            furthestBlock.appendChild(adopter);
                            tb.removeFromActiveFormattingElements(formatEl);
                            tb.removeFromStack(formatEl);
                            tb.insertOnStackAfter(furthestBlock, adopter);
                        }
                        break;
                    }
                    if (StringUtil.in(name, InBodyStartApplets)) {
                        if (tb.inScope("name")) break;
                        if (!tb.inScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.generateImpliedEndTags();
                        if (!tb.currentElement().nodeName().equals(name)) {
                            tb.error(this);
                        }
                        tb.popStackToClose(name);
                        tb.clearFormattingElementsToLastMarker();
                        break;
                    }
                    if (name.equals("br")) {
                        tb.error(this);
                        tb.processStartTag("br");
                        return false;
                    }
                    return this.anyOtherEndTag(t, tb);
                }
            }
            return true;
        }

        boolean anyOtherEndTag(Token t, HtmlTreeBuilder tb) {
            String name = t.asEndTag().name();
            ArrayList<Element> stack = tb.getStack();
            for (int pos = stack.size() - 1; pos >= 0; --pos) {
                Element node = stack.get(pos);
                if (node.nodeName().equals(name)) {
                    tb.generateImpliedEndTags(name);
                    if (!name.equals(tb.currentElement().nodeName())) {
                        tb.error(this);
                    }
                    tb.popStackToClose(name);
                    break;
                }
                if (!tb.isSpecial(node)) continue;
                tb.error(this);
                return false;
            }
            return true;
        }
    }
    ,
    Text{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isCharacter()) {
                tb.insert(t.asCharacter());
            } else {
                if (t.isEOF()) {
                    tb.error(this);
                    tb.pop();
                    tb.transition(tb.originalState());
                    return tb.process(t);
                }
                if (t.isEndTag()) {
                    tb.pop();
                    tb.transition(tb.originalState());
                }
            }
            return true;
        }
    }
    ,
    InTable{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isCharacter()) {
                tb.newPendingTableCharacters();
                tb.markInsertionMode();
                tb.transition(InTableText);
                return tb.process(t);
            }
            if (t.isComment()) {
                tb.insert(t.asComment());
                return true;
            }
            if (t.isDoctype()) {
                tb.error(this);
                return false;
            }
            if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.name();
                if (name.equals("caption")) {
                    tb.clearStackToTableContext();
                    tb.insertMarkerToFormattingElements();
                    tb.insert(startTag);
                    tb.transition(InCaption);
                } else if (name.equals("colgroup")) {
                    tb.clearStackToTableContext();
                    tb.insert(startTag);
                    tb.transition(InColumnGroup);
                } else {
                    if (name.equals("col")) {
                        tb.processStartTag("colgroup");
                        return tb.process(t);
                    }
                    if (StringUtil.in(name, "tbody", "tfoot", "thead")) {
                        tb.clearStackToTableContext();
                        tb.insert(startTag);
                        tb.transition(InTableBody);
                    } else {
                        if (StringUtil.in(name, "td", "th", "tr")) {
                            tb.processStartTag("tbody");
                            return tb.process(t);
                        }
                        if (name.equals("table")) {
                            tb.error(this);
                            boolean processed = tb.processEndTag("table");
                            if (processed) {
                                return tb.process(t);
                            }
                        } else {
                            if (StringUtil.in(name, "style", "script")) {
                                return tb.process(t, InHead);
                            }
                            if (name.equals("input")) {
                                if (!startTag.attributes.get("type").equalsIgnoreCase("hidden")) {
                                    return this.anythingElse(t, tb);
                                }
                                tb.insertEmpty(startTag);
                            } else if (name.equals("form")) {
                                tb.error(this);
                                if (tb.getFormElement() != null) {
                                    return false;
                                }
                                tb.insertForm(startTag, false);
                            } else {
                                return this.anythingElse(t, tb);
                            }
                        }
                    }
                }
                return true;
            }
            if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.name();
                if (name.equals("table")) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        return false;
                    }
                } else {
                    if (StringUtil.in(name, "body", "caption", "col", "colgroup", "html", "tbody", "td", "tfoot", "th", "thead", "tr")) {
                        tb.error(this);
                        return false;
                    }
                    return this.anythingElse(t, tb);
                }
                tb.popStackToClose("table");
                tb.resetInsertionMode();
                return true;
            }
            if (t.isEOF()) {
                if (tb.currentElement().nodeName().equals("html")) {
                    tb.error(this);
                }
                return true;
            }
            return this.anythingElse(t, tb);
        }

        boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            boolean processed;
            tb.error(this);
            if (StringUtil.in(tb.currentElement().nodeName(), "table", "tbody", "tfoot", "thead", "tr")) {
                tb.setFosterInserts(true);
                processed = tb.process(t, InBody);
                tb.setFosterInserts(false);
            } else {
                processed = tb.process(t, InBody);
            }
            return processed;
        }
    }
    ,
    InTableText{

        boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case Character: {
                    Token.Character c = t.asCharacter();
                    if (c.getData().equals(nullString)) {
                        tb.error(this);
                        return false;
                    }
                    tb.getPendingTableCharacters().add(c.getData());
                    break;
                }
                default: {
                    if (tb.getPendingTableCharacters().size() > 0) {
                        for (String character : tb.getPendingTableCharacters()) {
                            if (!HtmlTreeBuilderState.isWhitespace(character)) {
                                tb.error(this);
                                if (StringUtil.in(tb.currentElement().nodeName(), "table", "tbody", "tfoot", "thead", "tr")) {
                                    tb.setFosterInserts(true);
                                    tb.process(new Token.Character().data(character), InBody);
                                    tb.setFosterInserts(false);
                                    continue;
                                }
                                tb.process(new Token.Character().data(character), InBody);
                                continue;
                            }
                            tb.insert(new Token.Character().data(character));
                        }
                        tb.newPendingTableCharacters();
                    }
                    tb.transition(tb.originalState());
                    return tb.process(t);
                }
            }
            return true;
        }
    }
    ,
    InCaption{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isEndTag() && t.asEndTag().name().equals("caption")) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.name();
                if (!tb.inTableScope(name)) {
                    tb.error(this);
                    return false;
                }
                tb.generateImpliedEndTags();
                if (!tb.currentElement().nodeName().equals("caption")) {
                    tb.error(this);
                }
                tb.popStackToClose("caption");
                tb.clearFormattingElementsToLastMarker();
                tb.transition(InTable);
            } else if (t.isStartTag() && StringUtil.in(t.asStartTag().name(), "caption", "col", "colgroup", "tbody", "td", "tfoot", "th", "thead", "tr") || t.isEndTag() && t.asEndTag().name().equals("table")) {
                tb.error(this);
                boolean processed = tb.processEndTag("caption");
                if (processed) {
                    return tb.process(t);
                }
            } else {
                if (t.isEndTag() && StringUtil.in(t.asEndTag().name(), "body", "col", "colgroup", "html", "tbody", "td", "tfoot", "th", "thead", "tr")) {
                    tb.error(this);
                    return false;
                }
                return tb.process(t, InBody);
            }
            return true;
        }
    }
    ,
    InColumnGroup{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (HtmlTreeBuilderState.isWhitespace(t)) {
                tb.insert(t.asCharacter());
                return true;
            }
            switch (t.type) {
                case Comment: {
                    tb.insert(t.asComment());
                    break;
                }
                case Doctype: {
                    tb.error(this);
                    break;
                }
                case StartTag: {
                    Token.StartTag startTag = t.asStartTag();
                    String name = startTag.name();
                    if (name.equals("html")) {
                        return tb.process(t, InBody);
                    }
                    if (name.equals("col")) {
                        tb.insertEmpty(startTag);
                        break;
                    }
                    return this.anythingElse(t, tb);
                }
                case EndTag: {
                    Token.EndTag endTag = t.asEndTag();
                    String name = endTag.name();
                    if (name.equals("colgroup")) {
                        if (tb.currentElement().nodeName().equals("html")) {
                            tb.error(this);
                            return false;
                        }
                        tb.pop();
                        tb.transition(InTable);
                        break;
                    }
                    return this.anythingElse(t, tb);
                }
                case EOF: {
                    if (tb.currentElement().nodeName().equals("html")) {
                        return true;
                    }
                    return this.anythingElse(t, tb);
                }
                default: {
                    return this.anythingElse(t, tb);
                }
            }
            return true;
        }

        private boolean anythingElse(Token t, TreeBuilder tb) {
            boolean processed = tb.processEndTag("colgroup");
            if (processed) {
                return tb.process(t);
            }
            return true;
        }
    }
    ,
    InTableBody{

        boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case StartTag: {
                    Token.StartTag startTag = t.asStartTag();
                    String name = startTag.name();
                    if (name.equals("tr")) {
                        tb.clearStackToTableBodyContext();
                        tb.insert(startTag);
                        tb.transition(InRow);
                        break;
                    }
                    if (StringUtil.in(name, "th", "td")) {
                        tb.error(this);
                        tb.processStartTag("tr");
                        return tb.process(startTag);
                    }
                    if (StringUtil.in(name, "caption", "col", "colgroup", "tbody", "tfoot", "thead")) {
                        return this.exitTableBody(t, tb);
                    }
                    return this.anythingElse(t, tb);
                }
                case EndTag: {
                    Token.EndTag endTag = t.asEndTag();
                    String name = endTag.name();
                    if (StringUtil.in(name, "tbody", "tfoot", "thead")) {
                        if (!tb.inTableScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.clearStackToTableBodyContext();
                        tb.pop();
                        tb.transition(InTable);
                        break;
                    }
                    if (name.equals("table")) {
                        return this.exitTableBody(t, tb);
                    }
                    if (StringUtil.in(name, "body", "caption", "col", "colgroup", "html", "td", "th", "tr")) {
                        tb.error(this);
                        return false;
                    }
                    return this.anythingElse(t, tb);
                }
                default: {
                    return this.anythingElse(t, tb);
                }
            }
            return true;
        }

        private boolean exitTableBody(Token t, HtmlTreeBuilder tb) {
            if (!(tb.inTableScope("tbody") || tb.inTableScope("thead") || tb.inScope("tfoot"))) {
                tb.error(this);
                return false;
            }
            tb.clearStackToTableBodyContext();
            tb.processEndTag(tb.currentElement().nodeName());
            return tb.process(t);
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            return tb.process(t, InTable);
        }
    }
    ,
    InRow{

        /*
         * Enabled aggressive block sorting
         */
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isStartTag()) {
                Token.StartTag startTag = t.asStartTag();
                String name = startTag.name();
                if (StringUtil.in(name, "th", "td")) {
                    tb.clearStackToTableRowContext();
                    tb.insert(startTag);
                    tb.transition(InCell);
                    tb.insertMarkerToFormattingElements();
                    return true;
                }
                if (!StringUtil.in(name, "caption", "col", "colgroup", "tbody", "tfoot", "thead", "tr")) return this.anythingElse(t, tb);
                return this.handleMissingTr(t, tb);
            }
            if (!t.isEndTag()) return this.anythingElse(t, tb);
            Token.EndTag endTag = t.asEndTag();
            String name = endTag.name();
            if (name.equals("tr")) {
                if (!tb.inTableScope(name)) {
                    tb.error(this);
                    return false;
                }
                tb.clearStackToTableRowContext();
                tb.pop();
                tb.transition(InTableBody);
                return true;
            }
            if (name.equals("table")) {
                return this.handleMissingTr(t, tb);
            }
            if (!StringUtil.in(name, "tbody", "tfoot", "thead")) {
                if (!StringUtil.in(name, "body", "caption", "col", "colgroup", "html", "td", "th")) return this.anythingElse(t, tb);
                tb.error(this);
                return false;
            }
            if (!tb.inTableScope(name)) {
                tb.error(this);
                return false;
            }
            tb.processEndTag("tr");
            return tb.process(t);
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            return tb.process(t, InTable);
        }

        private boolean handleMissingTr(Token t, TreeBuilder tb) {
            boolean processed = tb.processEndTag("tr");
            if (processed) {
                return tb.process(t);
            }
            return false;
        }
    }
    ,
    InCell{

        /*
         * Enabled aggressive block sorting
         */
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isEndTag()) {
                Token.EndTag endTag = t.asEndTag();
                String name = endTag.name();
                if (StringUtil.in(name, "td", "th")) {
                    if (!tb.inTableScope(name)) {
                        tb.error(this);
                        tb.transition(InRow);
                        return false;
                    }
                    tb.generateImpliedEndTags();
                    if (!tb.currentElement().nodeName().equals(name)) {
                        tb.error(this);
                    }
                    tb.popStackToClose(name);
                    tb.clearFormattingElementsToLastMarker();
                    tb.transition(InRow);
                    return true;
                }
                if (StringUtil.in(name, "body", "caption", "col", "colgroup", "html")) {
                    tb.error(this);
                    return false;
                }
                if (!StringUtil.in(name, "table", "tbody", "tfoot", "thead", "tr")) return this.anythingElse(t, tb);
                if (!tb.inTableScope(name)) {
                    tb.error(this);
                    return false;
                }
                this.closeCell(tb);
                return tb.process(t);
            }
            if (!t.isStartTag()) return this.anythingElse(t, tb);
            if (!StringUtil.in(t.asStartTag().name(), "caption", "col", "colgroup", "tbody", "td", "tfoot", "th", "thead", "tr")) return this.anythingElse(t, tb);
            if (!tb.inTableScope("td") && !tb.inTableScope("th")) {
                tb.error(this);
                return false;
            }
            this.closeCell(tb);
            return tb.process(t);
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            return tb.process(t, InBody);
        }

        private void closeCell(HtmlTreeBuilder tb) {
            if (tb.inTableScope("td")) {
                tb.processEndTag("td");
            } else {
                tb.processEndTag("th");
            }
        }
    }
    ,
    InSelect{

        boolean process(Token t, HtmlTreeBuilder tb) {
            switch (t.type) {
                case Character: {
                    Token.Character c = t.asCharacter();
                    if (c.getData().equals(nullString)) {
                        tb.error(this);
                        return false;
                    }
                    tb.insert(c);
                    break;
                }
                case Comment: {
                    tb.insert(t.asComment());
                    break;
                }
                case Doctype: {
                    tb.error(this);
                    return false;
                }
                case StartTag: {
                    Token.StartTag start = t.asStartTag();
                    String name = start.name();
                    if (name.equals("html")) {
                        return tb.process(start, InBody);
                    }
                    if (name.equals("option")) {
                        tb.processEndTag("option");
                        tb.insert(start);
                        break;
                    }
                    if (name.equals("optgroup")) {
                        if (tb.currentElement().nodeName().equals("option")) {
                            tb.processEndTag("option");
                        } else if (tb.currentElement().nodeName().equals("optgroup")) {
                            tb.processEndTag("optgroup");
                        }
                        tb.insert(start);
                        break;
                    }
                    if (name.equals("select")) {
                        tb.error(this);
                        return tb.processEndTag("select");
                    }
                    if (StringUtil.in(name, "input", "keygen", "textarea")) {
                        tb.error(this);
                        if (!tb.inSelectScope("select")) {
                            return false;
                        }
                        tb.processEndTag("select");
                        return tb.process(start);
                    }
                    if (name.equals("script")) {
                        return tb.process(t, InHead);
                    }
                    return this.anythingElse(t, tb);
                }
                case EndTag: {
                    Token.EndTag end = t.asEndTag();
                    String name = end.name();
                    if (name.equals("optgroup")) {
                        if (tb.currentElement().nodeName().equals("option") && tb.aboveOnStack(tb.currentElement()) != null && tb.aboveOnStack(tb.currentElement()).nodeName().equals("optgroup")) {
                            tb.processEndTag("option");
                        }
                        if (tb.currentElement().nodeName().equals("optgroup")) {
                            tb.pop();
                            break;
                        }
                        tb.error(this);
                        break;
                    }
                    if (name.equals("option")) {
                        if (tb.currentElement().nodeName().equals("option")) {
                            tb.pop();
                            break;
                        }
                        tb.error(this);
                        break;
                    }
                    if (name.equals("select")) {
                        if (!tb.inSelectScope(name)) {
                            tb.error(this);
                            return false;
                        }
                        tb.popStackToClose(name);
                        tb.resetInsertionMode();
                        break;
                    }
                    return this.anythingElse(t, tb);
                }
                case EOF: {
                    if (tb.currentElement().nodeName().equals("html")) break;
                    tb.error(this);
                    break;
                }
                default: {
                    return this.anythingElse(t, tb);
                }
            }
            return true;
        }

        private boolean anythingElse(Token t, HtmlTreeBuilder tb) {
            tb.error(this);
            return false;
        }
    }
    ,
    InSelectInTable{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isStartTag() && StringUtil.in(t.asStartTag().name(), "caption", "table", "tbody", "tfoot", "thead", "tr", "td", "th")) {
                tb.error(this);
                tb.processEndTag("select");
                return tb.process(t);
            }
            if (t.isEndTag() && StringUtil.in(t.asEndTag().name(), "caption", "table", "tbody", "tfoot", "thead", "tr", "td", "th")) {
                tb.error(this);
                if (tb.inTableScope(t.asEndTag().name())) {
                    tb.processEndTag("select");
                    return tb.process(t);
                }
                return false;
            }
            return tb.process(t, InSelect);
        }
    }
    ,
    AfterBody{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (HtmlTreeBuilderState.isWhitespace(t)) {
                return tb.process(t, InBody);
            }
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else {
                if (t.isDoctype()) {
                    tb.error(this);
                    return false;
                }
                if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                    return tb.process(t, InBody);
                }
                if (t.isEndTag() && t.asEndTag().name().equals("html")) {
                    if (tb.isFragmentParsing()) {
                        tb.error(this);
                        return false;
                    }
                    tb.transition(AfterAfterBody);
                } else if (!t.isEOF()) {
                    tb.error(this);
                    tb.transition(InBody);
                    return tb.process(t);
                }
            }
            return true;
        }
    }
    ,
    InFrameset{

        /*
         * Enabled aggressive block sorting
         */
        boolean process(Token t, HtmlTreeBuilder tb) {
            if (HtmlTreeBuilderState.isWhitespace(t)) {
                tb.insert(t.asCharacter());
                return true;
            }
            if (t.isComment()) {
                tb.insert(t.asComment());
                return true;
            }
            if (t.isDoctype()) {
                tb.error(this);
                return false;
            }
            if (t.isStartTag()) {
                Token.StartTag start = t.asStartTag();
                String name = start.name();
                if (name.equals("html")) {
                    return tb.process(start, InBody);
                }
                if (name.equals("frameset")) {
                    tb.insert(start);
                    return true;
                }
                if (name.equals("frame")) {
                    tb.insertEmpty(start);
                    return true;
                }
                if (name.equals("noframes")) {
                    return tb.process(start, InHead);
                }
                tb.error(this);
                return false;
            }
            if (t.isEndTag() && t.asEndTag().name().equals("frameset")) {
                if (tb.currentElement().nodeName().equals("html")) {
                    tb.error(this);
                    return false;
                }
                tb.pop();
                if (tb.isFragmentParsing()) return true;
                if (tb.currentElement().nodeName().equals("frameset")) return true;
                tb.transition(AfterFrameset);
                return true;
            }
            if (t.isEOF()) {
                if (tb.currentElement().nodeName().equals("html")) return true;
                tb.error(this);
                return true;
            }
            tb.error(this);
            return false;
        }
    }
    ,
    AfterFrameset{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (HtmlTreeBuilderState.isWhitespace(t)) {
                tb.insert(t.asCharacter());
            } else if (t.isComment()) {
                tb.insert(t.asComment());
            } else {
                if (t.isDoctype()) {
                    tb.error(this);
                    return false;
                }
                if (t.isStartTag() && t.asStartTag().name().equals("html")) {
                    return tb.process(t, InBody);
                }
                if (t.isEndTag() && t.asEndTag().name().equals("html")) {
                    tb.transition(AfterAfterFrameset);
                } else {
                    if (t.isStartTag() && t.asStartTag().name().equals("noframes")) {
                        return tb.process(t, InHead);
                    }
                    if (!t.isEOF()) {
                        tb.error(this);
                        return false;
                    }
                }
            }
            return true;
        }
    }
    ,
    AfterAfterBody{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else {
                if (t.isDoctype() || HtmlTreeBuilderState.isWhitespace(t) || t.isStartTag() && t.asStartTag().name().equals("html")) {
                    return tb.process(t, InBody);
                }
                if (!t.isEOF()) {
                    tb.error(this);
                    tb.transition(InBody);
                    return tb.process(t);
                }
            }
            return true;
        }
    }
    ,
    AfterAfterFrameset{

        boolean process(Token t, HtmlTreeBuilder tb) {
            if (t.isComment()) {
                tb.insert(t.asComment());
            } else {
                if (t.isDoctype() || HtmlTreeBuilderState.isWhitespace(t) || t.isStartTag() && t.asStartTag().name().equals("html")) {
                    return tb.process(t, InBody);
                }
                if (!t.isEOF()) {
                    if (t.isStartTag() && t.asStartTag().name().equals("noframes")) {
                        return tb.process(t, InHead);
                    }
                    tb.error(this);
                    return false;
                }
            }
            return true;
        }
    }
    ,
    ForeignContent{

        boolean process(Token t, HtmlTreeBuilder tb) {
            return true;
        }
    };
    
    private static String nullString;

    private HtmlTreeBuilderState() {
    }

    abstract boolean process(Token var1, HtmlTreeBuilder var2);

    private static boolean isWhitespace(Token t) {
        if (t.isCharacter()) {
            String data = t.asCharacter().getData();
            return HtmlTreeBuilderState.isWhitespace(data);
        }
        return false;
    }

    private static boolean isWhitespace(String data) {
        for (int i = 0; i < data.length(); ++i) {
            char c = data.charAt(i);
            if (StringUtil.isWhitespace(c)) continue;
            return false;
        }
        return true;
    }

    private static void handleRcData(Token.StartTag startTag, HtmlTreeBuilder tb) {
        tb.insert(startTag);
        tb.tokeniser.transition(TokeniserState.Rcdata);
        tb.markInsertionMode();
        tb.transition(Text);
    }

    private static void handleRawtext(Token.StartTag startTag, HtmlTreeBuilder tb) {
        tb.insert(startTag);
        tb.tokeniser.transition(TokeniserState.Rawtext);
        tb.markInsertionMode();
        tb.transition(Text);
    }

    static {
        nullString = String.valueOf('\u0000');
    }

    private static final class Constants {
        private static final String[] InBodyStartToHead = new String[]{"base", "basefont", "bgsound", "command", "link", "meta", "noframes", "script", "style", "title"};
        private static final String[] InBodyStartPClosers = new String[]{"address", "article", "aside", "blockquote", "center", "details", "dir", "div", "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "menu", "nav", "ol", "p", "section", "summary", "ul"};
        private static final String[] Headings = new String[]{"h1", "h2", "h3", "h4", "h5", "h6"};
        private static final String[] InBodyStartPreListing = new String[]{"pre", "listing"};
        private static final String[] InBodyStartLiBreakers = new String[]{"address", "div", "p"};
        private static final String[] DdDt = new String[]{"dd", "dt"};
        private static final String[] Formatters = new String[]{"b", "big", "code", "em", "font", "i", "s", "small", "strike", "strong", "tt", "u"};
        private static final String[] InBodyStartApplets = new String[]{"applet", "marquee", "object"};
        private static final String[] InBodyStartEmptyFormatters = new String[]{"area", "br", "embed", "img", "keygen", "wbr"};
        private static final String[] InBodyStartMedia = new String[]{"param", "source", "track"};
        private static final String[] InBodyStartInputAttribs = new String[]{"name", "action", "prompt"};
        private static final String[] InBodyStartOptions = new String[]{"optgroup", "option"};
        private static final String[] InBodyStartRuby = new String[]{"rp", "rt"};
        private static final String[] InBodyStartDrop = new String[]{"caption", "col", "colgroup", "frame", "head", "tbody", "td", "tfoot", "th", "thead", "tr"};
        private static final String[] InBodyEndClosers = new String[]{"address", "article", "aside", "blockquote", "button", "center", "details", "dir", "div", "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "listing", "menu", "nav", "ol", "pre", "section", "summary", "ul"};
        private static final String[] InBodyEndAdoptionFormatters = new String[]{"a", "b", "big", "code", "em", "font", "i", "nobr", "s", "small", "strike", "strong", "tt", "u"};
        private static final String[] InBodyEndTableFosters = new String[]{"table", "tbody", "tfoot", "thead", "tr"};

        private Constants() {
        }
    }

}

