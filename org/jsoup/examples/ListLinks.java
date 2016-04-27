/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.examples;

import java.io.IOException;
import java.io.PrintStream;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ListLinks {
    public static void main(String[] args) throws IOException {
        Validate.isTrue(args.length == 1, "usage: supply url to fetch");
        String url = args[0];
        ListLinks.print("Fetching %s...", url);
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");
        Elements media = doc.select("[src]");
        Elements imports = doc.select("link[href]");
        ListLinks.print("\nMedia: (%d)", media.size());
        for (Element src : media) {
            if (src.tagName().equals("img")) {
                ListLinks.print(" * %s: <%s> %sx%s (%s)", src.tagName(), src.attr("abs:src"), src.attr("width"), src.attr("height"), ListLinks.trim(src.attr("alt"), 20));
                continue;
            }
            ListLinks.print(" * %s: <%s>", src.tagName(), src.attr("abs:src"));
        }
        ListLinks.print("\nImports: (%d)", imports.size());
        for (Element link2 : imports) {
            ListLinks.print(" * %s <%s> (%s)", link2.tagName(), link2.attr("abs:href"), link2.attr("rel"));
        }
        ListLinks.print("\nLinks: (%d)", links.size());
        for (Element link2 : links) {
            ListLinks.print(" * a: <%s>  (%s)", link2.attr("abs:href"), ListLinks.trim(link2.text(), 35));
        }
    }

    private static /* varargs */ void print(String msg, Object ... args) {
        System.out.println(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width) {
            return s.substring(0, width - 1) + ".";
        }
        return s;
    }
}

