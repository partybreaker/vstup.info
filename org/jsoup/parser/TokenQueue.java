/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;

public class TokenQueue {
    private String queue;
    private int pos = 0;
    private static final char ESC = '\\';

    public TokenQueue(String data) {
        Validate.notNull(data);
        this.queue = data;
    }

    public boolean isEmpty() {
        return this.remainingLength() == 0;
    }

    private int remainingLength() {
        return this.queue.length() - this.pos;
    }

    public char peek() {
        return this.isEmpty() ? '\u0000' : this.queue.charAt(this.pos);
    }

    public void addFirst(Character c) {
        this.addFirst(c.toString());
    }

    public void addFirst(String seq) {
        this.queue = seq + this.queue.substring(this.pos);
        this.pos = 0;
    }

    public boolean matches(String seq) {
        return this.queue.regionMatches(true, this.pos, seq, 0, seq.length());
    }

    public boolean matchesCS(String seq) {
        return this.queue.startsWith(seq, this.pos);
    }

    public /* varargs */ boolean matchesAny(String ... seq) {
        for (String s : seq) {
            if (!this.matches(s)) continue;
            return true;
        }
        return false;
    }

    public /* varargs */ boolean matchesAny(char ... seq) {
        if (this.isEmpty()) {
            return false;
        }
        for (char c : seq) {
            if (this.queue.charAt(this.pos) != c) continue;
            return true;
        }
        return false;
    }

    public boolean matchesStartTag() {
        return this.remainingLength() >= 2 && this.queue.charAt(this.pos) == '<' && Character.isLetter(this.queue.charAt(this.pos + 1));
    }

    public boolean matchChomp(String seq) {
        if (this.matches(seq)) {
            this.pos += seq.length();
            return true;
        }
        return false;
    }

    public boolean matchesWhitespace() {
        return !this.isEmpty() && StringUtil.isWhitespace(this.queue.charAt(this.pos));
    }

    public boolean matchesWord() {
        return !this.isEmpty() && Character.isLetterOrDigit(this.queue.charAt(this.pos));
    }

    public void advance() {
        if (!this.isEmpty()) {
            ++this.pos;
        }
    }

    public char consume() {
        return this.queue.charAt(this.pos++);
    }

    public void consume(String seq) {
        if (!this.matches(seq)) {
            throw new IllegalStateException("Queue did not match expected sequence");
        }
        int len = seq.length();
        if (len > this.remainingLength()) {
            throw new IllegalStateException("Queue not long enough to consume sequence");
        }
        this.pos += len;
    }

    public String consumeTo(String seq) {
        int offset = this.queue.indexOf(seq, this.pos);
        if (offset != -1) {
            String consumed = this.queue.substring(this.pos, offset);
            this.pos += consumed.length();
            return consumed;
        }
        return this.remainder();
    }

    public String consumeToIgnoreCase(String seq) {
        int start = this.pos;
        String first = seq.substring(0, 1);
        boolean canScan = first.toLowerCase().equals(first.toUpperCase());
        while (!this.isEmpty() && !this.matches(seq)) {
            if (canScan) {
                int skip = this.queue.indexOf(first, this.pos) - this.pos;
                if (skip == 0) {
                    ++this.pos;
                    continue;
                }
                if (skip < 0) {
                    this.pos = this.queue.length();
                    continue;
                }
                this.pos += skip;
                continue;
            }
            ++this.pos;
        }
        String data = this.queue.substring(start, this.pos);
        return data;
    }

    public /* varargs */ String consumeToAny(String ... seq) {
        int start = this.pos;
        while (!this.isEmpty() && !this.matchesAny(seq)) {
            ++this.pos;
        }
        String data = this.queue.substring(start, this.pos);
        return data;
    }

    public String chompTo(String seq) {
        String data = this.consumeTo(seq);
        this.matchChomp(seq);
        return data;
    }

    public String chompToIgnoreCase(String seq) {
        String data = this.consumeToIgnoreCase(seq);
        this.matchChomp(seq);
        return data;
    }

    public String chompBalanced(char open, char close) {
        int start = -1;
        int end = -1;
        int depth = 0;
        int last = 0;
        while (!this.isEmpty()) {
            Character c = Character.valueOf(this.consume());
            if (last == 0 || last != 92) {
                if (c.equals(Character.valueOf(open))) {
                    ++depth;
                    if (start == -1) {
                        start = this.pos;
                    }
                } else if (c.equals(Character.valueOf(close))) {
                    --depth;
                }
            }
            if (depth > 0 && last != 0) {
                end = this.pos;
            }
            last = c.charValue();
            if (depth > 0) continue;
        }
        return end >= 0 ? this.queue.substring(start, end) : "";
    }

    public static String unescape(String in) {
        StringBuilder out = new StringBuilder();
        char last = '\u0000';
        for (char c : in.toCharArray()) {
            if (c == '\\') {
                if (last != '\u0000' && last == '\\') {
                    out.append(c);
                }
            } else {
                out.append(c);
            }
            last = c;
        }
        return out.toString();
    }

    public boolean consumeWhitespace() {
        boolean seen = false;
        while (this.matchesWhitespace()) {
            ++this.pos;
            seen = true;
        }
        return seen;
    }

    public String consumeWord() {
        int start = this.pos;
        while (this.matchesWord()) {
            ++this.pos;
        }
        return this.queue.substring(start, this.pos);
    }

    public String consumeTagName() {
        int start = this.pos;
        while (!this.isEmpty() && (this.matchesWord() || this.matchesAny(':', '_', '-'))) {
            ++this.pos;
        }
        return this.queue.substring(start, this.pos);
    }

    public String consumeElementSelector() {
        int start = this.pos;
        while (!this.isEmpty() && (this.matchesWord() || this.matchesAny('|', '_', '-'))) {
            ++this.pos;
        }
        return this.queue.substring(start, this.pos);
    }

    public String consumeCssIdentifier() {
        int start = this.pos;
        while (!this.isEmpty() && (this.matchesWord() || this.matchesAny('-', '_'))) {
            ++this.pos;
        }
        return this.queue.substring(start, this.pos);
    }

    public String consumeAttributeKey() {
        int start = this.pos;
        while (!this.isEmpty() && (this.matchesWord() || this.matchesAny('-', '_', ':'))) {
            ++this.pos;
        }
        return this.queue.substring(start, this.pos);
    }

    public String remainder() {
        String remainder = this.queue.substring(this.pos, this.queue.length());
        this.pos = this.queue.length();
        return remainder;
    }

    public String toString() {
        return this.queue.substring(this.pos);
    }
}

