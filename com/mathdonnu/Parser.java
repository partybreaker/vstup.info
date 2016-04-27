/*
 * Decompiled with CFR 0_114.
 */
package com.mathdonnu;

import com.mathdonnu.Entry;
import com.mathdonnu.University;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Parser {
    Document HTMLpage;
    Integer UniID = 0;

    public void parse(String url) {
        try {
            this.HTMLpage = Jsoup.connect(url).get();
        }
        catch (IOException var2_2) {
            // empty catch block
        }
    }

    public static void main(String[] args) throws IOException {
    }

    public ArrayList<Entry> getEntryList() throws Exception {
        ArrayList<Entry> entryList = new ArrayList<Entry>();
        if (this.HTMLpage != null) {
            Element table;
            try {
                table = (Element)this.HTMLpage.select("table").get(3);
            }
            catch (Exception exception) {
                System.out.println("Small University. No spec links on the page.");
                throw exception;
            }
            for (int i = 2; i < table.select("tr").size(); ++i) {
                int ID = 0;
                try {
                    ID = Integer.parseInt(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(0)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("Error get integer ID tag, ID = 0");
                }
                String FULLNAME = null;
                try {
                    FULLNAME = Parser.SQLStringFilter(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(1)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("Name Error!");
                }
                int PRIORITY = 0;
                try {
                    PRIORITY = Integer.parseInt(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(2)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("PRIORITY Error");
                }
                float RATE_SUM = 0.0f;
                try {
                    RATE_SUM = Float.parseFloat(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(3)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("RATE_SUM error");
                }
                float SCHOOL_RATE = 0.0f;
                try {
                    SCHOOL_RATE = Float.parseFloat(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(4)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("SCHOOL_RATE Error");
                }
                String ZNO = "";
                try {
                    ZNO = ZNO + Parser.SQLStringFilter(((Element)((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(5)).getElementsByTag("span").get(0)).html());
                    ZNO = ZNO + ";";
                    ZNO = ZNO + Parser.SQLStringFilter(((Element)((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(5)).getElementsByTag("span").get(1)).html());
                    ZNO = ZNO + ";";
                    ZNO = ZNO + Parser.SQLStringFilter(((Element)((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(5)).getElementsByTag("span").get(2)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("ZNO Error");
                }
                String UNIVERSITY_EXAM = null;
                try {
                    UNIVERSITY_EXAM = Parser.SQLStringFilter(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(6)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("UNIVERSITY_EXAM Error");
                }
                String ADDITIONAL_SUMM = null;
                try {
                    ADDITIONAL_SUMM = Parser.SQLStringFilter(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(7)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("ADDITIONAL_SUM Error");
                }
                String RIGHT_TO_EXTRAORDINARY_ADMISSION = null;
                try {
                    RIGHT_TO_EXTRAORDINARY_ADMISSION = Parser.SQLStringFilter(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(8)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("RIGHT_TO_EXTRAORDINARY_ADMISSION Error");
                }
                String RIGHT_TO_NON_COMPETITIVE_ADMISSION = null;
                try {
                    RIGHT_TO_NON_COMPETITIVE_ADMISSION = Parser.SQLStringFilter(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(9)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("RIGHT_TO_NON_COMPETITIVE_ADMISSION Error");
                }
                String TARGET_DIRECTION = null;
                try {
                    TARGET_DIRECTION = Parser.SQLStringFilter(((Element)((Element)table.select("tr").get(i)).getElementsByTag("td").get(10)).html());
                }
                catch (Exception exception) {
                    System.out.println(exception.toString());
                    System.out.println("TARGET_DIRECTION Error");
                }
                entryList.add(new Entry(ID, FULLNAME, PRIORITY, RATE_SUM, SCHOOL_RATE, ZNO, UNIVERSITY_EXAM, ADDITIONAL_SUMM, RIGHT_TO_EXTRAORDINARY_ADMISSION, RIGHT_TO_NON_COMPETITIVE_ADMISSION, TARGET_DIRECTION));
            }
        }
        this.HTMLpage = null;
        return entryList;
    }

    public Integer getIntType(String Type) {
        if (Type.equals("\u0423\u043d\u0456\u0432\u0435\u0440\u0441\u0438\u0442\u0435\u0442")) {
            return 1;
        }
        if (Type.equals("\u0406\u043d\u0441\u0442\u0438\u0442\u0443\u0442")) {
            return 2;
        }
        if (Type.equals("\u0410\u043a\u0430\u0434\u0435\u043c\u0456\u044f")) {
            return 3;
        }
        if (Type.equals("\u041a\u043e\u043d\u0441\u0435\u0440\u0432\u0430\u0442\u043e\u0440\u0456\u044f (\u043c\u0443\u0437\u0438\u0447\u043d\u0430 \u0430\u043a\u0430\u0434\u0435\u043c\u0456\u044f)")) {
            return 4;
        }
        if (Type.equals("\u041a\u043e\u043b\u0435\u0434\u0436")) {
            return 5;
        }
        if (Type.equals("\u0422\u0435\u0445\u043d\u0456\u043a\u0443\u043c (\u0443\u0447\u0438\u043b\u0438\u0449\u0435)")) {
            return 6;
        }
        return 0;
    }

    public University getUniversityInfo() {
        if (this.HTMLpage != null) {
            Integer PostCode;
            Element table = (Element)this.HTMLpage.select("table.tablesaw").get(0);
            Elements rows = table.select("tr");
            String Uniname = Parser.SQLStringFilter(((Element)((Element)rows.get(0)).getElementsByTag("td").get(1)).text());
            String Post = Parser.SQLStringFilter(((Element)((Element)rows.get(2)).getElementsByTag("td").get(1)).text());
            String Address = Parser.SQLStringFilter(((Element)((Element)rows.get(3)).getElementsByTag("td").get(1)).text());
            String Phones = Parser.SQLStringFilter(((Element)((Element)rows.get(4)).getElementsByTag("td").get(1)).text());
            String Website = Parser.SQLStringFilter(((Element)((Element)rows.get(5)).getElementsByTag("td").get(1)).text());
            String Email = Parser.SQLStringFilter(((Element)((Element)rows.get(6)).getElementsByTag("td").get(1)).text());
            String Type = Parser.SQLStringFilter(((Element)((Element)rows.get(1)).getElementsByTag("td").get(1)).text());
            try {
                PostCode = Integer.parseInt(Post);
            }
            catch (Exception exception) {
                PostCode = 0;
            }
            this.HTMLpage = null;
            return new University(this.UniID, Uniname, this.getIntType(Type), PostCode, Address, Phones, Website, Email, "http://vstup.info/2015/i2015i" + this.UniID + ".html#vnz");
        }
        return new University(this.UniID, "\u041d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442", 0, 0, "\u041d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442", "\u041d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442", "\u041d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442", "\u041d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442", "http://vstup.info/2015/i2015i" + this.UniID + ".html#vnz");
    }

    public ArrayList<String> getUniversitySpecialityList() {
        ArrayList<String> SpecLinks = new ArrayList<String>();
        if (this.HTMLpage != null) {
            Elements links = this.HTMLpage.select("a");
            for (Element link : links) {
                String linkaddr = link.attr("href");
                if (!this.isLinkAccordSpecLink(linkaddr)) continue;
                SpecLinks.add(linkaddr.substring(0, linkaddr.length() - 5));
            }
        }
        this.HTMLpage = null;
        return SpecLinks;
    }

    public boolean isLinkAccordSpecLink(String Url) {
        String simple = "./" + this.UniID + "/i2015i" + this.UniID + "p";
        if (Url.length() < simple.length()) {
            return false;
        }
        if (Url.substring(0, simple.length()).equals(simple)) {
            return true;
        }
        return false;
    }

    public static String SQLStringFilter(String s) {
        char c1 = '\'';
        char c2 = '\\';
        char c3 = '\u2014';
        char c4 = '/';
        if (s == null) {
            return "null";
        }
        if (s.equals("\u2014/\u2014")) {
            return "NoNo";
        }
        if (s.equals("\u2014")) {
            return "No";
        }
        String result = "";
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == c1 || s.charAt(i) == c2 || s.charAt(i) == c3 || s.charAt(i) == c4) continue;
            result = result + s.charAt(i);
        }
        return result;
    }
}

