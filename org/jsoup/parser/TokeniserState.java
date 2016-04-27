/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import java.util.Arrays;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.Token;
import org.jsoup.parser.Tokeniser;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
enum TokeniserState {
    Data{

        void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '&': {
                    t.advanceTransition(CharacterReferenceInData);
                    break;
                }
                case '<': {
                    t.advanceTransition(TagOpen);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.emit(r.consume());
                    break;
                }
                case '\uffff': {
                    t.emit(new Token.EOF());
                    break;
                }
                default: {
                    String data = r.consumeData();
                    t.emit(data);
                }
            }
        }
    }
    ,
    CharacterReferenceInData{

        void read(Tokeniser t, CharacterReader r) {
            char[] c = t.consumeCharacterReference(null, false);
            if (c == null) {
                t.emit('&');
            } else {
                t.emit(c);
            }
            t.transition(Data);
        }
    }
    ,
    Rcdata{

        void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '&': {
                    t.advanceTransition(CharacterReferenceInRcdata);
                    break;
                }
                case '<': {
                    t.advanceTransition(RcdataLessthanSign);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    r.advance();
                    t.emit('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.emit(new Token.EOF());
                    break;
                }
                default: {
                    String data = r.consumeToAny('&', '<', '\u0000');
                    t.emit(data);
                }
            }
        }
    }
    ,
    CharacterReferenceInRcdata{

        void read(Tokeniser t, CharacterReader r) {
            char[] c = t.consumeCharacterReference(null, false);
            if (c == null) {
                t.emit('&');
            } else {
                t.emit(c);
            }
            t.transition(Rcdata);
        }
    }
    ,
    Rawtext{

        void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '<': {
                    t.advanceTransition(RawtextLessthanSign);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    r.advance();
                    t.emit('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.emit(new Token.EOF());
                    break;
                }
                default: {
                    String data = r.consumeToAny('<', '\u0000');
                    t.emit(data);
                }
            }
        }
    }
    ,
    ScriptData{

        void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '<': {
                    t.advanceTransition(ScriptDataLessthanSign);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    r.advance();
                    t.emit('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.emit(new Token.EOF());
                    break;
                }
                default: {
                    String data = r.consumeToAny('<', '\u0000');
                    t.emit(data);
                }
            }
        }
    }
    ,
    PLAINTEXT{

        void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '\u0000': {
                    t.error(this);
                    r.advance();
                    t.emit('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.emit(new Token.EOF());
                    break;
                }
                default: {
                    String data = r.consumeTo('\u0000');
                    t.emit(data);
                }
            }
        }
    }
    ,
    TagOpen{

        void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '!': {
                    t.advanceTransition(MarkupDeclarationOpen);
                    break;
                }
                case '/': {
                    t.advanceTransition(EndTagOpen);
                    break;
                }
                case '?': {
                    t.advanceTransition(BogusComment);
                    break;
                }
                default: {
                    if (r.matchesLetter()) {
                        t.createTagPending(true);
                        t.transition(TagName);
                        break;
                    }
                    t.error(this);
                    t.emit('<');
                    t.transition(Data);
                }
            }
        }
    }
    ,
    EndTagOpen{

        void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.emit("</");
                t.transition(Data);
            } else if (r.matchesLetter()) {
                t.createTagPending(false);
                t.transition(TagName);
            } else if (r.matches('>')) {
                t.error(this);
                t.advanceTransition(Data);
            } else {
                t.error(this);
                t.advanceTransition(BogusComment);
            }
        }
    }
    ,
    TagName{

        void read(Tokeniser t, CharacterReader r) {
            String tagName = r.consumeTagName().toLowerCase();
            t.tagPending.appendTagName(tagName);
            switch (r.consume()) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(BeforeAttributeName);
                    break;
                }
                case '/': {
                    t.transition(SelfClosingStartTag);
                    break;
                }
                case '>': {
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                case '\u0000': {
                    t.tagPending.appendTagName(replacementStr);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                }
            }
        }
    }
    ,
    RcdataLessthanSign{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matches('/')) {
                t.createTempBuffer();
                t.advanceTransition(RCDATAEndTagOpen);
            } else if (r.matchesLetter() && t.appropriateEndTagName() != null && !r.containsIgnoreCase("</" + t.appropriateEndTagName())) {
                t.tagPending = t.createTagPending(false).name(t.appropriateEndTagName());
                t.emitTagPending();
                r.unconsume();
                t.transition(Data);
            } else {
                t.emit("<");
                t.transition(Rcdata);
            }
        }
    }
    ,
    RCDATAEndTagOpen{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTagPending(false);
                t.tagPending.appendTagName(Character.toLowerCase(r.current()));
                t.dataBuffer.append(Character.toLowerCase(r.current()));
                t.advanceTransition(RCDATAEndTagName);
            } else {
                t.emit("</");
                t.transition(Rcdata);
            }
        }
    }
    ,
    RCDATAEndTagName{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                String name = r.consumeLetterSequence();
                t.tagPending.appendTagName(name.toLowerCase());
                t.dataBuffer.append(name);
                return;
            }
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    if (t.isAppropriateEndTagToken()) {
                        t.transition(BeforeAttributeName);
                        break;
                    }
                    this.anythingElse(t, r);
                    break;
                }
                case '/': {
                    if (t.isAppropriateEndTagToken()) {
                        t.transition(SelfClosingStartTag);
                        break;
                    }
                    this.anythingElse(t, r);
                    break;
                }
                case '>': {
                    if (t.isAppropriateEndTagToken()) {
                        t.emitTagPending();
                        t.transition(Data);
                        break;
                    }
                    this.anythingElse(t, r);
                    break;
                }
                default: {
                    this.anythingElse(t, r);
                }
            }
        }

        private void anythingElse(Tokeniser t, CharacterReader r) {
            t.emit("</" + t.dataBuffer.toString());
            r.unconsume();
            t.transition(Rcdata);
        }
    }
    ,
    RawtextLessthanSign{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matches('/')) {
                t.createTempBuffer();
                t.advanceTransition(RawtextEndTagOpen);
            } else {
                t.emit('<');
                t.transition(Rawtext);
            }
        }
    }
    ,
    RawtextEndTagOpen{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTagPending(false);
                t.transition(RawtextEndTagName);
            } else {
                t.emit("</");
                t.transition(Rawtext);
            }
        }
    }
    ,
    RawtextEndTagName{

        void read(Tokeniser t, CharacterReader r) {
            TokeniserState.handleDataEndTag(t, r, Rawtext);
        }
    }
    ,
    ScriptDataLessthanSign{

        void read(Tokeniser t, CharacterReader r) {
            switch (r.consume()) {
                case '/': {
                    t.createTempBuffer();
                    t.transition(ScriptDataEndTagOpen);
                    break;
                }
                case '!': {
                    t.emit("<!");
                    t.transition(ScriptDataEscapeStart);
                    break;
                }
                default: {
                    t.emit("<");
                    r.unconsume();
                    t.transition(ScriptData);
                }
            }
        }
    }
    ,
    ScriptDataEndTagOpen{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTagPending(false);
                t.transition(ScriptDataEndTagName);
            } else {
                t.emit("</");
                t.transition(ScriptData);
            }
        }
    }
    ,
    ScriptDataEndTagName{

        void read(Tokeniser t, CharacterReader r) {
            TokeniserState.handleDataEndTag(t, r, ScriptData);
        }
    }
    ,
    ScriptDataEscapeStart{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matches('-')) {
                t.emit('-');
                t.advanceTransition(ScriptDataEscapeStartDash);
            } else {
                t.transition(ScriptData);
            }
        }
    }
    ,
    ScriptDataEscapeStartDash{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matches('-')) {
                t.emit('-');
                t.advanceTransition(ScriptDataEscapedDashDash);
            } else {
                t.transition(ScriptData);
            }
        }
    }
    ,
    ScriptDataEscaped{

        void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.transition(Data);
                return;
            }
            switch (r.current()) {
                case '-': {
                    t.emit('-');
                    t.advanceTransition(ScriptDataEscapedDash);
                    break;
                }
                case '<': {
                    t.advanceTransition(ScriptDataEscapedLessthanSign);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    r.advance();
                    t.emit('\ufffd');
                    break;
                }
                default: {
                    String data = r.consumeToAny('-', '<', '\u0000');
                    t.emit(data);
                }
            }
        }
    }
    ,
    ScriptDataEscapedDash{

        void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.transition(Data);
                return;
            }
            char c = r.consume();
            switch (c) {
                case '-': {
                    t.emit(c);
                    t.transition(ScriptDataEscapedDashDash);
                    break;
                }
                case '<': {
                    t.transition(ScriptDataEscapedLessthanSign);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.emit('\ufffd');
                    t.transition(ScriptDataEscaped);
                    break;
                }
                default: {
                    t.emit(c);
                    t.transition(ScriptDataEscaped);
                }
            }
        }
    }
    ,
    ScriptDataEscapedDashDash{

        void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.transition(Data);
                return;
            }
            char c = r.consume();
            switch (c) {
                case '-': {
                    t.emit(c);
                    break;
                }
                case '<': {
                    t.transition(ScriptDataEscapedLessthanSign);
                    break;
                }
                case '>': {
                    t.emit(c);
                    t.transition(ScriptData);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.emit('\ufffd');
                    t.transition(ScriptDataEscaped);
                    break;
                }
                default: {
                    t.emit(c);
                    t.transition(ScriptDataEscaped);
                }
            }
        }
    }
    ,
    ScriptDataEscapedLessthanSign{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTempBuffer();
                t.dataBuffer.append(Character.toLowerCase(r.current()));
                t.emit("<" + r.current());
                t.advanceTransition(ScriptDataDoubleEscapeStart);
            } else if (r.matches('/')) {
                t.createTempBuffer();
                t.advanceTransition(ScriptDataEscapedEndTagOpen);
            } else {
                t.emit('<');
                t.transition(ScriptDataEscaped);
            }
        }
    }
    ,
    ScriptDataEscapedEndTagOpen{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTagPending(false);
                t.tagPending.appendTagName(Character.toLowerCase(r.current()));
                t.dataBuffer.append(r.current());
                t.advanceTransition(ScriptDataEscapedEndTagName);
            } else {
                t.emit("</");
                t.transition(ScriptDataEscaped);
            }
        }
    }
    ,
    ScriptDataEscapedEndTagName{

        void read(Tokeniser t, CharacterReader r) {
            TokeniserState.handleDataEndTag(t, r, ScriptDataEscaped);
        }
    }
    ,
    ScriptDataDoubleEscapeStart{

        void read(Tokeniser t, CharacterReader r) {
            TokeniserState.handleDataDoubleEscapeTag(t, r, ScriptDataDoubleEscaped, ScriptDataEscaped);
        }
    }
    ,
    ScriptDataDoubleEscaped{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.current();
            switch (c) {
                case '-': {
                    t.emit(c);
                    t.advanceTransition(ScriptDataDoubleEscapedDash);
                    break;
                }
                case '<': {
                    t.emit(c);
                    t.advanceTransition(ScriptDataDoubleEscapedLessthanSign);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    r.advance();
                    t.emit('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                default: {
                    String data = r.consumeToAny('-', '<', '\u0000');
                    t.emit(data);
                }
            }
        }
    }
    ,
    ScriptDataDoubleEscapedDash{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-': {
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscapedDashDash);
                    break;
                }
                case '<': {
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscapedLessthanSign);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.emit('\ufffd');
                    t.transition(ScriptDataDoubleEscaped);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                default: {
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscaped);
                }
            }
        }
    }
    ,
    ScriptDataDoubleEscapedDashDash{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-': {
                    t.emit(c);
                    break;
                }
                case '<': {
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscapedLessthanSign);
                    break;
                }
                case '>': {
                    t.emit(c);
                    t.transition(ScriptData);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.emit('\ufffd');
                    t.transition(ScriptDataDoubleEscaped);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                default: {
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscaped);
                }
            }
        }
    }
    ,
    ScriptDataDoubleEscapedLessthanSign{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matches('/')) {
                t.emit('/');
                t.createTempBuffer();
                t.advanceTransition(ScriptDataDoubleEscapeEnd);
            } else {
                t.transition(ScriptDataDoubleEscaped);
            }
        }
    }
    ,
    ScriptDataDoubleEscapeEnd{

        void read(Tokeniser t, CharacterReader r) {
            TokeniserState.handleDataDoubleEscapeTag(t, r, ScriptDataEscaped, ScriptDataDoubleEscaped);
        }
    }
    ,
    BeforeAttributeName{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    break;
                }
                case '/': {
                    t.transition(SelfClosingStartTag);
                    break;
                }
                case '>': {
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.tagPending.newAttribute();
                    r.unconsume();
                    t.transition(AttributeName);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                case '\"': 
                case '\'': 
                case '<': 
                case '=': {
                    t.error(this);
                    t.tagPending.newAttribute();
                    t.tagPending.appendAttributeName(c);
                    t.transition(AttributeName);
                    break;
                }
                default: {
                    t.tagPending.newAttribute();
                    r.unconsume();
                    t.transition(AttributeName);
                }
            }
        }
    }
    ,
    AttributeName{

        void read(Tokeniser t, CharacterReader r) {
            String name = r.consumeToAnySorted(attributeNameCharsSorted);
            t.tagPending.appendAttributeName(name.toLowerCase());
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(AfterAttributeName);
                    break;
                }
                case '/': {
                    t.transition(SelfClosingStartTag);
                    break;
                }
                case '=': {
                    t.transition(BeforeAttributeValue);
                    break;
                }
                case '>': {
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.tagPending.appendAttributeName('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                case '\"': 
                case '\'': 
                case '<': {
                    t.error(this);
                    t.tagPending.appendAttributeName(c);
                }
            }
        }
    }
    ,
    AfterAttributeName{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    break;
                }
                case '/': {
                    t.transition(SelfClosingStartTag);
                    break;
                }
                case '=': {
                    t.transition(BeforeAttributeValue);
                    break;
                }
                case '>': {
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.tagPending.appendAttributeName('\ufffd');
                    t.transition(AttributeName);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                case '\"': 
                case '\'': 
                case '<': {
                    t.error(this);
                    t.tagPending.newAttribute();
                    t.tagPending.appendAttributeName(c);
                    t.transition(AttributeName);
                    break;
                }
                default: {
                    t.tagPending.newAttribute();
                    r.unconsume();
                    t.transition(AttributeName);
                }
            }
        }
    }
    ,
    BeforeAttributeValue{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    break;
                }
                case '\"': {
                    t.transition(AttributeValue_doubleQuoted);
                    break;
                }
                case '&': {
                    r.unconsume();
                    t.transition(AttributeValue_unquoted);
                    break;
                }
                case '\'': {
                    t.transition(AttributeValue_singleQuoted);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.tagPending.appendAttributeValue('\ufffd');
                    t.transition(AttributeValue_unquoted);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                case '>': {
                    t.error(this);
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                case '<': 
                case '=': 
                case '`': {
                    t.error(this);
                    t.tagPending.appendAttributeValue(c);
                    t.transition(AttributeValue_unquoted);
                    break;
                }
                default: {
                    r.unconsume();
                    t.transition(AttributeValue_unquoted);
                }
            }
        }
    }
    ,
    AttributeValue_doubleQuoted{

        void read(Tokeniser t, CharacterReader r) {
            String value = r.consumeToAnySorted(attributeDoubleValueCharsSorted);
            if (value.length() > 0) {
                t.tagPending.appendAttributeValue(value);
            }
            char c = r.consume();
            switch (c) {
                case '\"': {
                    t.transition(AfterAttributeValue_quoted);
                    break;
                }
                case '&': {
                    char[] ref = t.consumeCharacterReference(Character.valueOf('\"'), true);
                    if (ref != null) {
                        t.tagPending.appendAttributeValue(ref);
                        break;
                    }
                    t.tagPending.appendAttributeValue('&');
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.tagPending.appendAttributeValue('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                }
            }
        }
    }
    ,
    AttributeValue_singleQuoted{

        void read(Tokeniser t, CharacterReader r) {
            String value = r.consumeToAnySorted(attributeSingleValueCharsSorted);
            if (value.length() > 0) {
                t.tagPending.appendAttributeValue(value);
            }
            char c = r.consume();
            switch (c) {
                case '\'': {
                    t.transition(AfterAttributeValue_quoted);
                    break;
                }
                case '&': {
                    char[] ref = t.consumeCharacterReference(Character.valueOf('\''), true);
                    if (ref != null) {
                        t.tagPending.appendAttributeValue(ref);
                        break;
                    }
                    t.tagPending.appendAttributeValue('&');
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.tagPending.appendAttributeValue('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                }
            }
        }
    }
    ,
    AttributeValue_unquoted{

        void read(Tokeniser t, CharacterReader r) {
            String value = r.consumeToAny('\t', '\n', '\r', '\f', ' ', '&', '>', '\u0000', '\"', '\'', '<', '=', '`');
            if (value.length() > 0) {
                t.tagPending.appendAttributeValue(value);
            }
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(BeforeAttributeName);
                    break;
                }
                case '&': {
                    char[] ref = t.consumeCharacterReference(Character.valueOf('>'), true);
                    if (ref != null) {
                        t.tagPending.appendAttributeValue(ref);
                        break;
                    }
                    t.tagPending.appendAttributeValue('&');
                    break;
                }
                case '>': {
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.tagPending.appendAttributeValue('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                case '\"': 
                case '\'': 
                case '<': 
                case '=': 
                case '`': {
                    t.error(this);
                    t.tagPending.appendAttributeValue(c);
                }
            }
        }
    }
    ,
    AfterAttributeValue_quoted{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(BeforeAttributeName);
                    break;
                }
                case '/': {
                    t.transition(SelfClosingStartTag);
                    break;
                }
                case '>': {
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    r.unconsume();
                    t.transition(BeforeAttributeName);
                }
            }
        }
    }
    ,
    SelfClosingStartTag{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '>': {
                    t.tagPending.selfClosing = true;
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.transition(BeforeAttributeName);
                }
            }
        }
    }
    ,
    BogusComment{

        void read(Tokeniser t, CharacterReader r) {
            r.unconsume();
            Token.Comment comment = new Token.Comment();
            comment.bogus = true;
            comment.data.append(r.consumeTo('>'));
            t.emit(comment);
            t.advanceTransition(Data);
        }
    }
    ,
    MarkupDeclarationOpen{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchConsume("--")) {
                t.createCommentPending();
                t.transition(CommentStart);
            } else if (r.matchConsumeIgnoreCase("DOCTYPE")) {
                t.transition(Doctype);
            } else if (r.matchConsume("[CDATA[")) {
                t.transition(CdataSection);
            } else {
                t.error(this);
                t.advanceTransition(BogusComment);
            }
        }
    }
    ,
    CommentStart{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-': {
                    t.transition(CommentStartDash);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.commentPending.data.append('\ufffd');
                    t.transition(Comment);
                    break;
                }
                case '>': {
                    t.error(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.commentPending.data.append(c);
                    t.transition(Comment);
                }
            }
        }
    }
    ,
    CommentStartDash{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-': {
                    t.transition(CommentStartDash);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.commentPending.data.append('\ufffd');
                    t.transition(Comment);
                    break;
                }
                case '>': {
                    t.error(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.commentPending.data.append(c);
                    t.transition(Comment);
                }
            }
        }
    }
    ,
    Comment{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.current();
            switch (c) {
                case '-': {
                    t.advanceTransition(CommentEndDash);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    r.advance();
                    t.commentPending.data.append('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.commentPending.data.append(r.consumeToAny('-', '\u0000'));
                }
            }
        }
    }
    ,
    CommentEndDash{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-': {
                    t.transition(CommentEnd);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.commentPending.data.append('-').append('\ufffd');
                    t.transition(Comment);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.commentPending.data.append('-').append(c);
                    t.transition(Comment);
                }
            }
        }
    }
    ,
    CommentEnd{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '>': {
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.commentPending.data.append("--").append('\ufffd');
                    t.transition(Comment);
                    break;
                }
                case '!': {
                    t.error(this);
                    t.transition(CommentEndBang);
                    break;
                }
                case '-': {
                    t.error(this);
                    t.commentPending.data.append('-');
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.commentPending.data.append("--").append(c);
                    t.transition(Comment);
                }
            }
        }
    }
    ,
    CommentEndBang{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-': {
                    t.commentPending.data.append("--!");
                    t.transition(CommentEndDash);
                    break;
                }
                case '>': {
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.commentPending.data.append("--!").append('\ufffd');
                    t.transition(Comment);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.commentPending.data.append("--!").append(c);
                    t.transition(Comment);
                }
            }
        }
    }
    ,
    Doctype{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(BeforeDoctypeName);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                }
                case '>': {
                    t.error(this);
                    t.createDoctypePending();
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.transition(BeforeDoctypeName);
                }
            }
        }
    }
    ,
    BeforeDoctypeName{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createDoctypePending();
                t.transition(DoctypeName);
                return;
            }
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.createDoctypePending();
                    t.doctypePending.name.append('\ufffd');
                    t.transition(DoctypeName);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.createDoctypePending();
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.createDoctypePending();
                    t.doctypePending.name.append(c);
                    t.transition(DoctypeName);
                }
            }
        }
    }
    ,
    DoctypeName{

        void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                String name = r.consumeLetterSequence();
                t.doctypePending.name.append(name.toLowerCase());
                return;
            }
            char c = r.consume();
            switch (c) {
                case '>': {
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(AfterDoctypeName);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.doctypePending.name.append('\ufffd');
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.doctypePending.name.append(c);
                }
            }
        }
    }
    ,
    AfterDoctypeName{

        void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.doctypePending.forceQuirks = true;
                t.emitDoctypePending();
                t.transition(Data);
                return;
            }
            if (r.matchesAny('\t', '\n', '\r', '\f', ' ')) {
                r.advance();
            } else if (r.matches('>')) {
                t.emitDoctypePending();
                t.advanceTransition(Data);
            } else if (r.matchConsumeIgnoreCase("PUBLIC")) {
                t.transition(AfterDoctypePublicKeyword);
            } else if (r.matchConsumeIgnoreCase("SYSTEM")) {
                t.transition(AfterDoctypeSystemKeyword);
            } else {
                t.error(this);
                t.doctypePending.forceQuirks = true;
                t.advanceTransition(BogusDoctype);
            }
        }
    }
    ,
    AfterDoctypePublicKeyword{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(BeforeDoctypePublicIdentifier);
                    break;
                }
                case '\"': {
                    t.error(this);
                    t.transition(DoctypePublicIdentifier_doubleQuoted);
                    break;
                }
                case '\'': {
                    t.error(this);
                    t.transition(DoctypePublicIdentifier_singleQuoted);
                    break;
                }
                case '>': {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
                }
            }
        }
    }
    ,
    BeforeDoctypePublicIdentifier{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    break;
                }
                case '\"': {
                    t.transition(DoctypePublicIdentifier_doubleQuoted);
                    break;
                }
                case '\'': {
                    t.transition(DoctypePublicIdentifier_singleQuoted);
                    break;
                }
                case '>': {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
                }
            }
        }
    }
    ,
    DoctypePublicIdentifier_doubleQuoted{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\"': {
                    t.transition(AfterDoctypePublicIdentifier);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.doctypePending.publicIdentifier.append('\ufffd');
                    break;
                }
                case '>': {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.doctypePending.publicIdentifier.append(c);
                }
            }
        }
    }
    ,
    DoctypePublicIdentifier_singleQuoted{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\'': {
                    t.transition(AfterDoctypePublicIdentifier);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.doctypePending.publicIdentifier.append('\ufffd');
                    break;
                }
                case '>': {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.doctypePending.publicIdentifier.append(c);
                }
            }
        }
    }
    ,
    AfterDoctypePublicIdentifier{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(BetweenDoctypePublicAndSystemIdentifiers);
                    break;
                }
                case '>': {
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\"': {
                    t.error(this);
                    t.transition(DoctypeSystemIdentifier_doubleQuoted);
                    break;
                }
                case '\'': {
                    t.error(this);
                    t.transition(DoctypeSystemIdentifier_singleQuoted);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
                }
            }
        }
    }
    ,
    BetweenDoctypePublicAndSystemIdentifiers{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    break;
                }
                case '>': {
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\"': {
                    t.error(this);
                    t.transition(DoctypeSystemIdentifier_doubleQuoted);
                    break;
                }
                case '\'': {
                    t.error(this);
                    t.transition(DoctypeSystemIdentifier_singleQuoted);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
                }
            }
        }
    }
    ,
    AfterDoctypeSystemKeyword{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(BeforeDoctypeSystemIdentifier);
                    break;
                }
                case '>': {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\"': {
                    t.error(this);
                    t.transition(DoctypeSystemIdentifier_doubleQuoted);
                    break;
                }
                case '\'': {
                    t.error(this);
                    t.transition(DoctypeSystemIdentifier_singleQuoted);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                }
            }
        }
    }
    ,
    BeforeDoctypeSystemIdentifier{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    break;
                }
                case '\"': {
                    t.transition(DoctypeSystemIdentifier_doubleQuoted);
                    break;
                }
                case '\'': {
                    t.transition(DoctypeSystemIdentifier_singleQuoted);
                    break;
                }
                case '>': {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
                }
            }
        }
    }
    ,
    DoctypeSystemIdentifier_doubleQuoted{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\"': {
                    t.transition(AfterDoctypeSystemIdentifier);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.doctypePending.systemIdentifier.append('\ufffd');
                    break;
                }
                case '>': {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.doctypePending.systemIdentifier.append(c);
                }
            }
        }
    }
    ,
    DoctypeSystemIdentifier_singleQuoted{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\'': {
                    t.transition(AfterDoctypeSystemIdentifier);
                    break;
                }
                case '\u0000': {
                    t.error(this);
                    t.doctypePending.systemIdentifier.append('\ufffd');
                    break;
                }
                case '>': {
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.doctypePending.systemIdentifier.append(c);
                }
            }
        }
    }
    ,
    AfterDoctypeSystemIdentifier{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    break;
                }
                case '>': {
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.error(this);
                    t.transition(BogusDoctype);
                }
            }
        }
    }
    ,
    BogusDoctype{

        void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '>': {
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
                case '\uffff': {
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                }
            }
        }
    }
    ,
    CdataSection{

        void read(Tokeniser t, CharacterReader r) {
            String data = r.consumeTo("]]>");
            t.emit(data);
            r.matchConsume("]]>");
            t.transition(Data);
        }
    };
    
    static final char nullChar = '\u0000';
    private static final char[] attributeSingleValueCharsSorted;
    private static final char[] attributeDoubleValueCharsSorted;
    private static final char[] attributeNameCharsSorted;
    private static final char replacementChar = '\ufffd';
    private static final String replacementStr;
    private static final char eof = '\uffff';

    private TokeniserState() {
    }

    abstract void read(Tokeniser var1, CharacterReader var2);

    private static void handleDataEndTag(Tokeniser t, CharacterReader r, TokeniserState elseTransition) {
        if (r.matchesLetter()) {
            String name = r.consumeLetterSequence();
            t.tagPending.appendTagName(name.toLowerCase());
            t.dataBuffer.append(name);
            return;
        }
        boolean needsExitTransition = false;
        if (t.isAppropriateEndTagToken() && !r.isEmpty()) {
            char c = r.consume();
            switch (c) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    t.transition(BeforeAttributeName);
                    break;
                }
                case '/': {
                    t.transition(SelfClosingStartTag);
                    break;
                }
                case '>': {
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                }
                default: {
                    t.dataBuffer.append(c);
                    needsExitTransition = true;
                    break;
                }
            }
        } else {
            needsExitTransition = true;
        }
        if (needsExitTransition) {
            t.emit("</" + t.dataBuffer.toString());
            t.transition(elseTransition);
        }
    }

    private static void handleDataDoubleEscapeTag(Tokeniser t, CharacterReader r, TokeniserState primary, TokeniserState fallback) {
        if (r.matchesLetter()) {
            String name = r.consumeLetterSequence();
            t.dataBuffer.append(name.toLowerCase());
            t.emit(name);
            return;
        }
        char c = r.consume();
        switch (c) {
            case '\t': 
            case '\n': 
            case '\f': 
            case '\r': 
            case ' ': 
            case '/': 
            case '>': {
                if (t.dataBuffer.toString().equals("script")) {
                    t.transition(primary);
                } else {
                    t.transition(fallback);
                }
                t.emit(c);
                break;
            }
            default: {
                r.unconsume();
                t.transition(fallback);
            }
        }
    }

    static {
        attributeSingleValueCharsSorted = new char[]{'\'', '&', '\u0000'};
        attributeDoubleValueCharsSorted = new char[]{'\"', '&', '\u0000'};
        attributeNameCharsSorted = new char[]{'\t', '\n', '\r', '\f', ' ', '/', '=', '>', '\u0000', '\"', '\'', '<'};
        replacementStr = String.valueOf('\ufffd');
        Arrays.sort(attributeSingleValueCharsSorted);
        Arrays.sort(attributeDoubleValueCharsSorted);
        Arrays.sort(attributeNameCharsSorted);
    }

}

