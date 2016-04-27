/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.Set;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Entities {
    private static final Map<String, Character> full;
    private static final Map<Character, String> xhtmlByVal;
    private static final Map<String, Character> base;
    private static final Map<Character, String> baseByVal;
    private static final Map<Character, String> fullByVal;
    private static final Object[][] xhtmlArray;

    private Entities() {
    }

    public static boolean isNamedEntity(String name) {
        return full.containsKey(name);
    }

    public static boolean isBaseNamedEntity(String name) {
        return base.containsKey(name);
    }

    public static Character getCharacterByName(String name) {
        return full.get(name);
    }

    static String escape(String string, Document.OutputSettings out) {
        StringBuilder accum = new StringBuilder(string.length() * 2);
        Entities.escape(accum, string, out, false, false, false);
        return accum.toString();
    }

    static void escape(StringBuilder accum, String string, Document.OutputSettings out, boolean inAttribute, boolean normaliseWhite, boolean stripLeadingWhite) {
        int codePoint;
        boolean lastWasWhite = false;
        boolean reachedNonWhite = false;
        EscapeMode escapeMode = out.escapeMode();
        CharsetEncoder encoder = out.encoder();
        CoreCharset coreCharset = CoreCharset.byName(encoder.charset().name());
        Map<Character, String> map = escapeMode.getMap();
        int length = string.length();
        block7 : for (int offset = 0; offset < length; offset += Character.charCount((int)codePoint)) {
            codePoint = string.codePointAt(offset);
            if (normaliseWhite) {
                if (StringUtil.isWhitespace(codePoint)) {
                    if (stripLeadingWhite && !reachedNonWhite || lastWasWhite) continue;
                    accum.append(' ');
                    lastWasWhite = true;
                    continue;
                }
                lastWasWhite = false;
                reachedNonWhite = true;
            }
            if (codePoint < 65536) {
                char c = (char)codePoint;
                switch (c) {
                    case '&': {
                        accum.append("&amp;");
                        continue block7;
                    }
                    case '\u00a0': {
                        if (escapeMode != EscapeMode.xhtml) {
                            accum.append("&nbsp;");
                            continue block7;
                        }
                        accum.append(c);
                        continue block7;
                    }
                    case '<': {
                        if (!inAttribute) {
                            accum.append("&lt;");
                            continue block7;
                        }
                        accum.append(c);
                        continue block7;
                    }
                    case '>': {
                        if (!inAttribute) {
                            accum.append("&gt;");
                            continue block7;
                        }
                        accum.append(c);
                        continue block7;
                    }
                    case '\"': {
                        if (inAttribute) {
                            accum.append("&quot;");
                            continue block7;
                        }
                        accum.append(c);
                        continue block7;
                    }
                }
                if (Entities.canEncode(coreCharset, c, encoder)) {
                    accum.append(c);
                    continue;
                }
                if (map.containsKey(Character.valueOf(c))) {
                    accum.append('&').append(map.get(Character.valueOf(c))).append(';');
                    continue;
                }
                accum.append("&#x").append(Integer.toHexString(codePoint)).append(';');
                continue;
            }
            String c = new String(Character.toChars(codePoint));
            if (encoder.canEncode(c)) {
                accum.append(c);
                continue;
            }
            accum.append("&#x").append(Integer.toHexString(codePoint)).append(';');
        }
    }

    static String unescape(String string) {
        return Entities.unescape(string, false);
    }

    static String unescape(String string, boolean strict) {
        return Parser.unescapeEntities(string, strict);
    }

    private static boolean canEncode(CoreCharset charset, char c, CharsetEncoder fallback) {
        switch (charset) {
            case ascii: {
                return c < '';
            }
            case utf: {
                return true;
            }
        }
        return fallback.canEncode(c);
    }

    private static Map<String, Character> loadEntities(String filename) {
        Properties properties = new Properties();
        HashMap<String, Character> entities = new HashMap<String, Character>();
        try {
            InputStream in = Entities.class.getResourceAsStream(filename);
            properties.load(in);
            in.close();
        }
        catch (IOException e) {
            throw new MissingResourceException("Error loading entities resource: " + e.getMessage(), "Entities", filename);
        }
        for (Map.Entry entry : properties.entrySet()) {
            Character val = Character.valueOf((char)Integer.parseInt((String)entry.getValue(), 16));
            String name = (String)entry.getKey();
            entities.put(name, val);
        }
        return entities;
    }

    private static Map<Character, String> toCharacterKey(Map<String, Character> inMap) {
        HashMap<Character, String> outMap = new HashMap<Character, String>();
        for (Map.Entry<String, Character> entry : inMap.entrySet()) {
            Character character = entry.getValue();
            String name = entry.getKey();
            if (outMap.containsKey(character)) {
                if (!name.toLowerCase().equals(name)) continue;
                outMap.put(character, name);
                continue;
            }
            outMap.put(character, name);
        }
        return outMap;
    }

    static /* synthetic */ Map access$000() {
        return xhtmlByVal;
    }

    static /* synthetic */ Map access$100() {
        return baseByVal;
    }

    static /* synthetic */ Map access$200() {
        return fullByVal;
    }

    static {
        xhtmlArray = new Object[][]{{"quot", 34}, {"amp", 38}, {"lt", 60}, {"gt", 62}};
        xhtmlByVal = new HashMap<Character, String>();
        base = Entities.loadEntities("entities-base.properties");
        baseByVal = Entities.toCharacterKey(base);
        full = Entities.loadEntities("entities-full.properties");
        fullByVal = Entities.toCharacterKey(full);
        for (Object[] entity : xhtmlArray) {
            Character c = Character.valueOf((char)((Integer)entity[1]).intValue());
            xhtmlByVal.put(c, (String)entity[0]);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static enum CoreCharset {
        ascii,
        utf,
        fallback;
        

        private CoreCharset() {
        }

        private static CoreCharset byName(String name) {
            if (name.equals("US-ASCII")) {
                return ascii;
            }
            if (name.startsWith("UTF-")) {
                return utf;
            }
            return fallback;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum EscapeMode {
        xhtml(Entities.access$000()),
        base(Entities.access$100()),
        extended(Entities.access$200());
        
        private Map<Character, String> map;

        private EscapeMode(Map<Character, String> map) {
            this.map = map;
        }

        public Map<Character, String> getMap() {
            return this.map;
        }
    }

}

