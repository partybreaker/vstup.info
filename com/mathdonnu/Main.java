/*
 * Decompiled with CFR 0_114.
 */
package com.mathdonnu;

import com.mathdonnu.Entry;
import com.mathdonnu.Parser;
import com.mathdonnu.SQLiteJDBC;
import java.io.PrintStream;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Parser parser = new Parser();
        SQLiteJDBC sqlite = new SQLiteJDBC();
        int UniRangeStart = Integer.parseInt(args[1]);
        int UniRangeStop = Integer.parseInt(args[2]);
        ArrayList<Integer> badUniversities = new ArrayList<Integer>();
        sqlite.OpenDatabase("VSTUP_2015_" + args[0]);
        if (UniRangeStop > 1657) {
            UniRangeStop = 1657;
        }
        for (int UID = UniRangeStart; UID < UniRangeStop; ++UID) {
            parser.UniID = UID;
            String URL = "http://vstup.info/2015/i2015i" + UID + ".html";
            System.out.println("Parsing url: " + URL);
            parser.parse(URL);
            ArrayList<String> specList = null;
            try {
                specList = parser.getUniversitySpecialityList();
            }
            catch (Exception exception) {
                System.out.println("Bad University #" + UID);
                badUniversities.add(UID);
            }
            for (int i = 0; i < specList.size(); ++i) {
                String spectablename = "SPEC_TABLE_" + UID + "p" + specList.get(i).substring(specList.get(i).length() - 11, specList.get(i).length() - 5);
                sqlite.CreateSpecialityTableInCurrentDB(spectablename);
                System.out.println("" + i + ": Table " + spectablename + " has been created.");
                System.out.println("Now parsing URL:");
                URL = "http://vstup.info/2015" + specList.get(i).substring(1);
                System.out.println(URL);
                parser.parse("http://vstup.info/2015" + specList.get(i).substring(1));
                ArrayList<Entry> entryList = null;
                try {
                    entryList = parser.getEntryList();
                }
                catch (Exception exception) {
                    System.out.println("Error when getEntryList");
                    badUniversities.add(UID);
                }
                if (entryList != null) {
                    System.out.println("Some debug info:\n ########## \nObtained Entry list (" + entryList.size() + " Entries)\n##########");
                    System.out.println("Now inserting these Entries to table " + spectablename);
                    for (int j = 0; j < entryList.size(); ++j) {
                        sqlite.InsertEntryToTable(spectablename, entryList.get(j));
                    }
                    continue;
                }
                System.out.println("NULL entryList");
            }
        }
        sqlite.CloseDatabase();
        System.out.println("Some DEBUG about bad universities\n##########\n");
        for (Integer i : badUniversities) {
            System.out.print(i + " ");
        }
    }
}

