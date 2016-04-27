/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

import java.util.Arrays;
import java.util.Locale;
import org.jsoup.helper.Validate;

final class CharacterReader {
    static final char EOF = '\uffff';
    private static final int maxCacheLen = 12;
    private final char[] input;
    private final int length;
    private int pos = 0;
    private int mark = 0;
    private final String[] stringCache = new String[512];

    CharacterReader(String input) {
        Validate.notNull(input);
        this.input = input.toCharArray();
        this.length = this.input.length;
    }

    int pos() {
        return this.pos;
    }

    boolean isEmpty() {
        return this.pos >= this.length;
    }

    char current() {
        return this.pos >= this.length ? '\uffff' : this.input[this.pos];
    }

    char consume() {
        char val = this.pos >= this.length ? '\uffff' : this.input[this.pos];
        ++this.pos;
        return val;
    }

    void unconsume() {
        --this.pos;
    }

    void advance() {
        ++this.pos;
    }

    void mark() {
        this.mark = this.pos;
    }

    void rewindToMark() {
        this.pos = this.mark;
    }

    String consumeAsString() {
        return new String(this.input, this.pos++, 1);
    }

    int nextIndexOf(char c) {
        for (int i = this.pos; i < this.length; ++i) {
            if (c != this.input[i]) continue;
            return i - this.pos;
        }
        return -1;
    }

    int nextIndexOf(CharSequence seq) {
        char startChar = seq.charAt(0);
        for (int offset = this.pos; offset < this.length; ++offset) {
            if (startChar != this.input[offset]) {
                while (++offset < this.length && startChar != this.input[offset]) {
                }
            }
            int i = offset + 1;
            int last = i + seq.length() - 1;
            if (offset >= this.length || last > this.length) continue;
            int j = 1;
            while (i < last && seq.charAt(j) == this.input[i]) {
                ++i;
                ++j;
            }
            if (i != last) continue;
            return offset - this.pos;
        }
        return -1;
    }

    String consumeTo(char c) {
        int offset = this.nextIndexOf(c);
        if (offset != -1) {
            String consumed = this.cacheString(this.pos, offset);
            this.pos += offset;
            return consumed;
        }
        return this.consumeToEnd();
    }

    String consumeTo(String seq) {
        int offset = this.nextIndexOf(seq);
        if (offset != -1) {
            String consumed = this.cacheString(this.pos, offset);
            this.pos += offset;
            return consumed;
        }
        return this.consumeToEnd();
    }

    /* varargs */ String consumeToAny(char ... chars) {
        int start = this.pos;
        int remaining = this.length;
        block0 : while (this.pos < remaining) {
            for (char c : chars) {
                if (this.input[this.pos] == c) break block0;
            }
            ++this.pos;
        }
        return this.pos > start ? this.cacheString(start, this.pos - start) : "";
    }

    /* varargs */ String consumeToAnySorted(char ... chars) {
        int start = this.pos;
        int remaining = this.length;
        char[] val = this.input;
        while (this.pos < remaining && Arrays.binarySearch(chars, val[this.pos]) < 0) {
            ++this.pos;
        }
        return this.pos > start ? this.cacheString(start, this.pos - start) : "";
    }

    String consumeData() {
        char c;
        int start = this.pos;
        int remaining = this.length;
        char[] val = this.input;
        while (this.pos < remaining && (c = val[this.pos]) != '&' && c != '<' && c != '\u0000') {
            ++this.pos;
        }
        return this.pos > start ? this.cacheString(start, this.pos - start) : "";
    }

    String consumeTagName() {
        char c;
        int start = this.pos;
        int remaining = this.length;
        char[] val = this.input;
        while (this.pos < remaining && (c = val[this.pos]) != '\t' && c != '\n' && c != '\r' && c != '\f' && c != ' ' && c != '/' && c != '>' && c != '\u0000') {
            ++this.pos;
        }
        return this.pos > start ? this.cacheString(start, this.pos - start) : "";
    }

    String consumeToEnd() {
        String data = this.cacheString(this.pos, this.length - this.pos);
        this.pos = this.length;
        return data;
    }

    String consumeLetterSequence() {
        char c;
        int start = this.pos;
        while (this.pos < this.length && ((c = this.input[this.pos]) >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')) {
            ++this.pos;
        }
        return this.cacheString(start, this.pos - start);
    }

    String consumeLetterThenDigitSequence() {
        char c;
        int start = this.pos;
        while (this.pos < this.length && ((c = this.input[this.pos]) >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')) {
            ++this.pos;
        }
        while (!this.isEmpty() && (c = this.input[this.pos]) >= '0' && c <= '9') {
            ++this.pos;
        }
        return this.cacheString(start, this.pos - start);
    }

    String consumeHexSequence() {
        char c;
        int start = this.pos;
        while (this.pos < this.length && ((c = this.input[this.pos]) >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
            ++this.pos;
        }
        return this.cacheString(start, this.pos - start);
    }

    String consumeDigitSequence() {
        char c;
        int start = this.pos;
        while (this.pos < this.length && (c = this.input[this.pos]) >= '0' && c <= '9') {
            ++this.pos;
        }
        return this.cacheString(start, this.pos - start);
    }

    boolean matches(char c) {
        return !this.isEmpty() && this.input[this.pos] == c;
    }

    boolean matches(String seq) {
        int scanLength = seq.length();
        if (scanLength > this.length - this.pos) {
            return false;
        }
        for (int offset = 0; offset < scanLength; ++offset) {
            if (seq.charAt(offset) == this.input[this.pos + offset]) continue;
            return false;
        }
        return true;
    }

    boolean matchesIgnoreCase(String seq) {
        int scanLength = seq.length();
        if (scanLength > this.length - this.pos) {
            return false;
        }
        for (int offset = 0; offset < scanLength; ++offset) {
            char upTarget;
            char upScan = Character.toUpperCase(seq.charAt(offset));
            if (upScan == (upTarget = Character.toUpperCase(this.input[this.pos + offset]))) continue;
            return false;
        }
        return true;
    }

    /* varargs */ boolean matchesAny(char ... seq) {
        if (this.isEmpty()) {
            return false;
        }
        char c = this.input[this.pos];
        for (char seek : seq) {
            if (seek != c) continue;
            return true;
        }
        return false;
    }

    boolean matchesAnySorted(char[] seq) {
        return !this.isEmpty() && Arrays.binarySearch(seq, this.input[this.pos]) >= 0;
    }

    boolean matchesLetter() {
        if (this.isEmpty()) {
            return false;
        }
        char c = this.input[this.pos];
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z';
    }

    boolean matchesDigit() {
        if (this.isEmpty()) {
            return false;
        }
        char c = this.input[this.pos];
        return c >= '0' && c <= '9';
    }

    boolean matchConsume(String seq) {
        if (this.matches(seq)) {
            this.pos += seq.length();
            return true;
        }
        return false;
    }

    boolean matchConsumeIgnoreCase(String seq) {
        if (this.matchesIgnoreCase(seq)) {
            this.pos += seq.length();
            return true;
        }
        return false;
    }

    boolean containsIgnoreCase(String seq) {
        String loScan = seq.toLowerCase(Locale.ENGLISH);
        String hiScan = seq.toUpperCase(Locale.ENGLISH);
        return this.nextIndexOf(loScan) > -1 || this.nextIndexOf(hiScan) > -1;
    }

    public String toString() {
        return new String(this.input, this.pos, this.length - this.pos);
    }

    private String cacheString(int start, int count) {
        char[] val = this.input;
        String[] cache = this.stringCache;
        if (count > 12) {
            return new String(val, start, count);
        }
        int hash = 0;
        int offset = start;
        for (int i = 0; i < count; ++i) {
            hash = 31 * hash + val[offset++];
        }
        int index = hash & cache.length - 1;
        String cached = cache[index];
        if (cached == null) {
            cache[index] = cached = new String(val, start, count);
        } else {
            if (this.rangeEquals(start, count, cached)) {
                return cached;
            }
            cached = new String(val, start, count);
        }
        return cached;
    }

    boolean rangeEquals(int start, int count, String cached) {
        if (count == cached.length()) {
            char[] one = this.input;
            int i = start;
            int j = 0;
            while (count-- != 0) {
                if (one[i++] == cached.charAt(j++)) continue;
                return false;
            }
            return true;
        }
        return false;
    }
}

