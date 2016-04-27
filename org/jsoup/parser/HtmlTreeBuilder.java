/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.HtmlTreeBuilderState;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Tag;
import org.jsoup.parser.Token;
import org.jsoup.parser.Tokeniser;
import org.jsoup.parser.TokeniserState;
import org.jsoup.parser.TreeBuilder;
import org.jsoup.select.Elements;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class HtmlTreeBuilder
extends TreeBuilder {
    private static final String[] TagsScriptStyle = new String[]{"script", "style"};
    public static final String[] TagsSearchInScope = new String[]{"applet", "caption", "html", "table", "td", "th", "marquee", "object"};
    private static final String[] TagSearchList = new String[]{"ol", "ul"};
    private static final String[] TagSearchButton = new String[]{"button"};
    private static final String[] TagSearchTableScope = new String[]{"html", "table"};
    private static final String[] TagSearchSelectScope = new String[]{"optgroup", "option"};
    private static final String[] TagSearchEndTags = new String[]{"dd", "dt", "li", "option", "optgroup", "p", "rp", "rt"};
    private static final String[] TagSearchSpecial = new String[]{"address", "applet", "area", "article", "aside", "base", "basefont", "bgsound", "blockquote", "body", "br", "button", "caption", "center", "col", "colgroup", "command", "dd", "details", "dir", "div", "dl", "dt", "embed", "fieldset", "figcaption", "figure", "footer", "form", "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hgroup", "hr", "html", "iframe", "img", "input", "isindex", "li", "link", "listing", "marquee", "menu", "meta", "nav", "noembed", "noframes", "noscript", "object", "ol", "p", "param", "plaintext", "pre", "script", "section", "select", "style", "summary", "table", "tbody", "td", "textarea", "tfoot", "th", "thead", "title", "tr", "ul", "wbr", "xmp"};
    private HtmlTreeBuilderState state;
    private HtmlTreeBuilderState originalState;
    private boolean baseUriSetFromDoc = false;
    private Element headElement;
    private FormElement formElement;
    private Element contextElement;
    private ArrayList<Element> formattingElements = new ArrayList();
    private List<String> pendingTableCharacters = new ArrayList<String>();
    private Token.EndTag emptyEnd = new Token.EndTag();
    private boolean framesetOk = true;
    private boolean fosterInserts = false;
    private boolean fragmentParsing = false;
    private String[] specificScopeTarget = new String[]{null};

    HtmlTreeBuilder() {
    }

    @Override
    Document parse(String input, String baseUri, ParseErrorList errors) {
        this.state = HtmlTreeBuilderState.Initial;
        this.baseUriSetFromDoc = false;
        return super.parse(input, baseUri, errors);
    }

    List<Node> parseFragment(String inputFragment, Element context, String baseUri, ParseErrorList errors) {
        this.state = HtmlTreeBuilderState.Initial;
        this.initialiseParse(inputFragment, baseUri, errors);
        this.contextElement = context;
        this.fragmentParsing = true;
        Node root = null;
        if (context != null) {
            String contextTag;
            if (context.ownerDocument() != null) {
                this.doc.quirksMode(context.ownerDocument().quirksMode());
            }
            if (StringUtil.in(contextTag = context.tagName(), "title", "textarea")) {
                this.tokeniser.transition(TokeniserState.Rcdata);
            } else if (StringUtil.in(contextTag, "iframe", "noembed", "noframes", "style", "xmp")) {
                this.tokeniser.transition(TokeniserState.Rawtext);
            } else if (contextTag.equals("script")) {
                this.tokeniser.transition(TokeniserState.ScriptData);
            } else if (contextTag.equals("noscript")) {
                this.tokeniser.transition(TokeniserState.Data);
            } else if (contextTag.equals("plaintext")) {
                this.tokeniser.transition(TokeniserState.Data);
            } else {
                this.tokeniser.transition(TokeniserState.Data);
            }
            root = new Element(Tag.valueOf("html"), baseUri);
            this.doc.appendChild(root);
            this.stack.add(root);
            this.resetInsertionMode();
            Elements contextChain = context.parents();
            contextChain.add(0, context);
            for (Element parent : contextChain) {
                if (!(parent instanceof FormElement)) continue;
                this.formElement = (FormElement)parent;
                break;
            }
        }
        this.runParser();
        if (context != null && root != null) {
            return root.childNodes();
        }
        return this.doc.childNodes();
    }

    @Override
    protected boolean process(Token token) {
        this.currentToken = token;
        return this.state.process(token, this);
    }

    boolean process(Token token, HtmlTreeBuilderState state) {
        this.currentToken = token;
        return state.process(token, this);
    }

    void transition(HtmlTreeBuilderState state) {
        this.state = state;
    }

    HtmlTreeBuilderState state() {
        return this.state;
    }

    void markInsertionMode() {
        this.originalState = this.state;
    }

    HtmlTreeBuilderState originalState() {
        return this.originalState;
    }

    void framesetOk(boolean framesetOk) {
        this.framesetOk = framesetOk;
    }

    boolean framesetOk() {
        return this.framesetOk;
    }

    Document getDocument() {
        return this.doc;
    }

    String getBaseUri() {
        return this.baseUri;
    }

    void maybeSetBaseUri(Element base) {
        if (this.baseUriSetFromDoc) {
            return;
        }
        String href = base.absUrl("href");
        if (href.length() != 0) {
            this.baseUri = href;
            this.baseUriSetFromDoc = true;
            this.doc.setBaseUri(href);
        }
    }

    boolean isFragmentParsing() {
        return this.fragmentParsing;
    }

    void error(HtmlTreeBuilderState state) {
        if (this.errors.canAddError()) {
            this.errors.add(new ParseError(this.reader.pos(), "Unexpected token [%s] when in state [%s]", new Object[]{this.currentToken.tokenType(), state}));
        }
    }

    Element insert(Token.StartTag startTag) {
        if (startTag.isSelfClosing()) {
            Element el = this.insertEmpty(startTag);
            this.stack.add(el);
            this.tokeniser.transition(TokeniserState.Data);
            this.tokeniser.emit(this.emptyEnd.reset().name(el.tagName()));
            return el;
        }
        Element el = new Element(Tag.valueOf(startTag.name()), this.baseUri, startTag.attributes);
        this.insert(el);
        return el;
    }

    Element insertStartTag(String startTagName) {
        Element el = new Element(Tag.valueOf(startTagName), this.baseUri);
        this.insert(el);
        return el;
    }

    void insert(Element el) {
        this.insertNode(el);
        this.stack.add(el);
    }

    Element insertEmpty(Token.StartTag startTag) {
        Tag tag = Tag.valueOf(startTag.name());
        Element el = new Element(tag, this.baseUri, startTag.attributes);
        this.insertNode(el);
        if (startTag.isSelfClosing()) {
            if (tag.isKnownTag()) {
                if (tag.isSelfClosing()) {
                    this.tokeniser.acknowledgeSelfClosingFlag();
                }
            } else {
                tag.setSelfClosing();
                this.tokeniser.acknowledgeSelfClosingFlag();
            }
        }
        return el;
    }

    FormElement insertForm(Token.StartTag startTag, boolean onStack) {
        Tag tag = Tag.valueOf(startTag.name());
        FormElement el = new FormElement(tag, this.baseUri, startTag.attributes);
        this.setFormElement(el);
        this.insertNode(el);
        if (onStack) {
            this.stack.add(el);
        }
        return el;
    }

    void insert(Token.Comment commentToken) {
        Comment comment = new Comment(commentToken.getData(), this.baseUri);
        this.insertNode(comment);
    }

    void insert(Token.Character characterToken) {
        String tagName = this.currentElement().tagName();
        Node node = tagName.equals("script") || tagName.equals("style") ? new DataNode(characterToken.getData(), this.baseUri) : new TextNode(characterToken.getData(), this.baseUri);
        this.currentElement().appendChild(node);
    }

    private void insertNode(Node node) {
        if (this.stack.size() == 0) {
            this.doc.appendChild(node);
        } else if (this.isFosterInserts()) {
            this.insertInFosterParent(node);
        } else {
            this.currentElement().appendChild(node);
        }
        if (node instanceof Element && ((Element)node).tag().isFormListed() && this.formElement != null) {
            this.formElement.addElement((Element)node);
        }
    }

    Element pop() {
        int size = this.stack.size();
        return (Element)this.stack.remove(size - 1);
    }

    void push(Element element) {
        this.stack.add(element);
    }

    ArrayList<Element> getStack() {
        return this.stack;
    }

    boolean onStack(Element el) {
        return this.isElementInQueue(this.stack, el);
    }

    private boolean isElementInQueue(ArrayList<Element> queue, Element element) {
        for (int pos = queue.size() - 1; pos >= 0; --pos) {
            Element next = queue.get(pos);
            if (next != element) continue;
            return true;
        }
        return false;
    }

    Element getFromStack(String elName) {
        for (int pos = this.stack.size() - 1; pos >= 0; --pos) {
            Element next = (Element)this.stack.get(pos);
            if (!next.nodeName().equals(elName)) continue;
            return next;
        }
        return null;
    }

    boolean removeFromStack(Element el) {
        for (int pos = this.stack.size() - 1; pos >= 0; --pos) {
            Element next = (Element)this.stack.get(pos);
            if (next != el) continue;
            this.stack.remove(pos);
            return true;
        }
        return false;
    }

    void popStackToClose(String elName) {
        for (int pos = this.stack.size() - 1; pos >= 0; --pos) {
            Element next = (Element)this.stack.get(pos);
            this.stack.remove(pos);
            if (next.nodeName().equals(elName)) break;
        }
    }

    /* varargs */ void popStackToClose(String ... elNames) {
        for (int pos = this.stack.size() - 1; pos >= 0; --pos) {
            Element next = (Element)this.stack.get(pos);
            this.stack.remove(pos);
            if (StringUtil.in(next.nodeName(), elNames)) break;
        }
    }

    void popStackToBefore(String elName) {
        Element next;
        for (int pos = this.stack.size() - 1; pos >= 0 && !(next = (Element)this.stack.get(pos)).nodeName().equals(elName); --pos) {
            this.stack.remove(pos);
        }
    }

    void clearStackToTableContext() {
        this.clearStackToContext("table");
    }

    void clearStackToTableBodyContext() {
        this.clearStackToContext("tbody", "tfoot", "thead");
    }

    void clearStackToTableRowContext() {
        this.clearStackToContext("tr");
    }

    private /* varargs */ void clearStackToContext(String ... nodeNames) {
        Element next;
        for (int pos = this.stack.size() - 1; pos >= 0 && !StringUtil.in((next = (Element)this.stack.get(pos)).nodeName(), nodeNames) && !next.nodeName().equals("html"); --pos) {
            this.stack.remove(pos);
        }
    }

    Element aboveOnStack(Element el) {
        assert (this.onStack(el));
        for (int pos = this.stack.size() - 1; pos >= 0; --pos) {
            Element next = (Element)this.stack.get(pos);
            if (next != el) continue;
            return (Element)this.stack.get(pos - 1);
        }
        return null;
    }

    void insertOnStackAfter(Element after, Element in) {
        int i = this.stack.lastIndexOf(after);
        Validate.isTrue(i != -1);
        this.stack.add(i + 1, in);
    }

    void replaceOnStack(Element out, Element in) {
        this.replaceInQueue(this.stack, out, in);
    }

    private void replaceInQueue(ArrayList<Element> queue, Element out, Element in) {
        int i = queue.lastIndexOf(out);
        Validate.isTrue(i != -1);
        queue.set(i, in);
    }

    void resetInsertionMode() {
        boolean last = false;
        for (int pos = this.stack.size() - 1; pos >= 0; --pos) {
            String name;
            Element node = (Element)this.stack.get(pos);
            if (pos == 0) {
                last = true;
                node = this.contextElement;
            }
            if ("select".equals(name = node.nodeName())) {
                this.transition(HtmlTreeBuilderState.InSelect);
                break;
            }
            if ("td".equals(name) || "td".equals(name) && !last) {
                this.transition(HtmlTreeBuilderState.InCell);
                break;
            }
            if ("tr".equals(name)) {
                this.transition(HtmlTreeBuilderState.InRow);
                break;
            }
            if ("tbody".equals(name) || "thead".equals(name) || "tfoot".equals(name)) {
                this.transition(HtmlTreeBuilderState.InTableBody);
                break;
            }
            if ("caption".equals(name)) {
                this.transition(HtmlTreeBuilderState.InCaption);
                break;
            }
            if ("colgroup".equals(name)) {
                this.transition(HtmlTreeBuilderState.InColumnGroup);
                break;
            }
            if ("table".equals(name)) {
                this.transition(HtmlTreeBuilderState.InTable);
                break;
            }
            if ("head".equals(name)) {
                this.transition(HtmlTreeBuilderState.InBody);
                break;
            }
            if ("body".equals(name)) {
                this.transition(HtmlTreeBuilderState.InBody);
                break;
            }
            if ("frameset".equals(name)) {
                this.transition(HtmlTreeBuilderState.InFrameset);
                break;
            }
            if ("html".equals(name)) {
                this.transition(HtmlTreeBuilderState.BeforeHead);
                break;
            }
            if (!last) continue;
            this.transition(HtmlTreeBuilderState.InBody);
            break;
        }
    }

    private boolean inSpecificScope(String targetName, String[] baseTypes, String[] extraTypes) {
        this.specificScopeTarget[0] = targetName;
        return this.inSpecificScope(this.specificScopeTarget, baseTypes, extraTypes);
    }

    private boolean inSpecificScope(String[] targetNames, String[] baseTypes, String[] extraTypes) {
        for (int pos = this.stack.size() - 1; pos >= 0; --pos) {
            Element el = (Element)this.stack.get(pos);
            String elName = el.nodeName();
            if (StringUtil.in(elName, targetNames)) {
                return true;
            }
            if (StringUtil.in(elName, baseTypes)) {
                return false;
            }
            if (extraTypes == null || !StringUtil.in(elName, extraTypes)) continue;
            return false;
        }
        Validate.fail("Should not be reachable");
        return false;
    }

    boolean inScope(String[] targetNames) {
        return this.inSpecificScope(targetNames, TagsSearchInScope, null);
    }

    boolean inScope(String targetName) {
        return this.inScope(targetName, null);
    }

    boolean inScope(String targetName, String[] extras) {
        return this.inSpecificScope(targetName, TagsSearchInScope, extras);
    }

    boolean inListItemScope(String targetName) {
        return this.inScope(targetName, TagSearchList);
    }

    boolean inButtonScope(String targetName) {
        return this.inScope(targetName, TagSearchButton);
    }

    boolean inTableScope(String targetName) {
        return this.inSpecificScope(targetName, TagSearchTableScope, null);
    }

    boolean inSelectScope(String targetName) {
        for (int pos = this.stack.size() - 1; pos >= 0; --pos) {
            Element el = (Element)this.stack.get(pos);
            String elName = el.nodeName();
            if (elName.equals(targetName)) {
                return true;
            }
            if (StringUtil.in(elName, TagSearchSelectScope)) continue;
            return false;
        }
        Validate.fail("Should not be reachable");
        return false;
    }

    void setHeadElement(Element headElement) {
        this.headElement = headElement;
    }

    Element getHeadElement() {
        return this.headElement;
    }

    boolean isFosterInserts() {
        return this.fosterInserts;
    }

    void setFosterInserts(boolean fosterInserts) {
        this.fosterInserts = fosterInserts;
    }

    FormElement getFormElement() {
        return this.formElement;
    }

    void setFormElement(FormElement formElement) {
        this.formElement = formElement;
    }

    void newPendingTableCharacters() {
        this.pendingTableCharacters = new ArrayList<String>();
    }

    List<String> getPendingTableCharacters() {
        return this.pendingTableCharacters;
    }

    void setPendingTableCharacters(List<String> pendingTableCharacters) {
        this.pendingTableCharacters = pendingTableCharacters;
    }

    void generateImpliedEndTags(String excludeTag) {
        while (excludeTag != null && !this.currentElement().nodeName().equals(excludeTag) && StringUtil.in(this.currentElement().nodeName(), TagSearchEndTags)) {
            this.pop();
        }
    }

    void generateImpliedEndTags() {
        this.generateImpliedEndTags(null);
    }

    boolean isSpecial(Element el) {
        String name = el.nodeName();
        return StringUtil.in(name, TagSearchSpecial);
    }

    Element lastFormattingElement() {
        return this.formattingElements.size() > 0 ? this.formattingElements.get(this.formattingElements.size() - 1) : null;
    }

    Element removeLastFormattingElement() {
        int size = this.formattingElements.size();
        if (size > 0) {
            return this.formattingElements.remove(size - 1);
        }
        return null;
    }

    void pushActiveFormattingElements(Element in) {
        Element el;
        int numSeen = 0;
        for (int pos = this.formattingElements.size() - 1; pos >= 0 && (el = this.formattingElements.get(pos)) != null; --pos) {
            if (this.isSameFormattingElement(in, el)) {
                ++numSeen;
            }
            if (numSeen != 3) continue;
            this.formattingElements.remove(pos);
            break;
        }
        this.formattingElements.add(in);
    }

    private boolean isSameFormattingElement(Element a, Element b) {
        return a.nodeName().equals(b.nodeName()) && a.attributes().equals(b.attributes());
    }

    void reconstructFormattingElements() {
        Element last = this.lastFormattingElement();
        if (last == null || this.onStack(last)) {
            return;
        }
        Element entry = last;
        int size = this.formattingElements.size();
        int pos = size - 1;
        boolean skip = false;
        do {
            if (pos != 0) continue;
            skip = true;
            break;
        } while ((entry = this.formattingElements.get(--pos)) != null && !this.onStack(entry));
        do {
            if (!skip) {
                entry = this.formattingElements.get(++pos);
            }
            Validate.notNull(entry);
            skip = false;
            Element newEl = this.insertStartTag(entry.nodeName());
            newEl.attributes().addAll(entry.attributes());
            this.formattingElements.set(pos, newEl);
        } while (pos != size - 1);
    }

    void clearFormattingElementsToLastMarker() {
        Element el;
        while (!this.formattingElements.isEmpty() && (el = this.removeLastFormattingElement()) != null) {
        }
    }

    void removeFromActiveFormattingElements(Element el) {
        for (int pos = this.formattingElements.size() - 1; pos >= 0; --pos) {
            Element next = this.formattingElements.get(pos);
            if (next != el) continue;
            this.formattingElements.remove(pos);
            break;
        }
    }

    boolean isInActiveFormattingElements(Element el) {
        return this.isElementInQueue(this.formattingElements, el);
    }

    Element getActiveFormattingElement(String nodeName) {
        Element next;
        for (int pos = this.formattingElements.size() - 1; pos >= 0 && (next = this.formattingElements.get(pos)) != null; --pos) {
            if (!next.nodeName().equals(nodeName)) continue;
            return next;
        }
        return null;
    }

    void replaceActiveFormattingElement(Element out, Element in) {
        this.replaceInQueue(this.formattingElements, out, in);
    }

    void insertMarkerToFormattingElements() {
        this.formattingElements.add(null);
    }

    void insertInFosterParent(Node in) {
        Element fosterParent;
        Element lastTable = this.getFromStack("table");
        boolean isLastTableParent = false;
        if (lastTable != null) {
            if (lastTable.parent() != null) {
                fosterParent = lastTable.parent();
                isLastTableParent = true;
            } else {
                fosterParent = this.aboveOnStack(lastTable);
            }
        } else {
            fosterParent = (Element)this.stack.get(0);
        }
        if (isLastTableParent) {
            Validate.notNull(lastTable);
            lastTable.before(in);
        } else {
            fosterParent.appendChild(in);
        }
    }

    public String toString() {
        return "TreeBuilder{currentToken=" + this.currentToken + ", state=" + (Object)((Object)this.state) + ", currentElement=" + this.currentElement() + '}';
    }
}

