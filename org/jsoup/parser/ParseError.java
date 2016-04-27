/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.parser;

public class ParseError {
    private int pos;
    private String errorMsg;

    ParseError(int pos, String errorMsg) {
        this.pos = pos;
        this.errorMsg = errorMsg;
    }

    /* varargs */ ParseError(int pos, String errorFormat, Object ... args) {
        this.errorMsg = String.format(errorFormat, args);
        this.pos = pos;
    }

    public String getErrorMessage() {
        return this.errorMsg;
    }

    public int getPosition() {
        return this.pos;
    }

    public String toString() {
        return "" + this.pos + ": " + this.errorMsg;
    }
}

