/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.io.PrintStream;

public class OSInfo {
    public static void main(String[] args) {
        if (args.length >= 1) {
            if ("--os".equals(args[0])) {
                System.out.print(OSInfo.getOSName());
                return;
            }
            if ("--arch".equals(args[0])) {
                System.out.print(OSInfo.getArchName());
                return;
            }
        }
        System.out.print(OSInfo.getNativeLibFolderPathForCurrentOS());
    }

    public static String getNativeLibFolderPathForCurrentOS() {
        return OSInfo.getOSName() + "/" + OSInfo.getArchName();
    }

    public static String getOSName() {
        return OSInfo.translateOSNameToFolderName(System.getProperty("os.name"));
    }

    public static String getArchName() {
        return OSInfo.translateArchNameToFolderName(System.getProperty("os.arch"));
    }

    public static String translateOSNameToFolderName(String osName) {
        if (osName.contains("Windows")) {
            return "Windows";
        }
        if (osName.contains("Mac")) {
            return "Mac";
        }
        if (osName.contains("Linux")) {
            return "Linux";
        }
        return osName.replaceAll("\\W", "");
    }

    public static String translateArchNameToFolderName(String archName) {
        return archName.replaceAll("\\W", "");
    }
}

