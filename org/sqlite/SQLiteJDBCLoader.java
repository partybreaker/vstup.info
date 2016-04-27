/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.sqlite.OSInfo;

public class SQLiteJDBCLoader {
    private static boolean extracted = false;

    public static boolean initialize() {
        SQLiteJDBCLoader.loadSQLiteNativeLibrary();
        return extracted;
    }

    static boolean getPureJavaFlag() {
        return Boolean.parseBoolean(System.getProperty("sqlite.purejava", "false"));
    }

    public static boolean isPureJavaMode() {
        return !SQLiteJDBCLoader.isNativeMode();
    }

    public static boolean isNativeMode() {
        if (SQLiteJDBCLoader.getPureJavaFlag()) {
            return false;
        }
        SQLiteJDBCLoader.initialize();
        return extracted;
    }

    static String md5sum(InputStream input) throws IOException {
        BufferedInputStream in = new BufferedInputStream(input);
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            DigestInputStream digestInputStream = new DigestInputStream(in, digest);
            while (digestInputStream.read() >= 0) {
            }
            ByteArrayOutputStream md5out = new ByteArrayOutputStream();
            md5out.write(digest.digest());
            String string = md5out.toString();
            return string;
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available: " + e);
        }
        finally {
            in.close();
        }
    }

    private static boolean extractAndLoadLibraryFile(String libFolderForCurrentOS, String libraryFileName, String targetFolder) {
        String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
        String prefix = "sqlite-" + SQLiteJDBCLoader.getVersion() + "-";
        String extractedLibFileName = prefix + libraryFileName;
        File extractedLibFile = new File(targetFolder, extractedLibFileName);
        try {
            if (extractedLibFile.exists()) {
                String md5sum2;
                String md5sum1 = SQLiteJDBCLoader.md5sum(SQLiteJDBCLoader.class.getResourceAsStream(nativeLibraryFilePath));
                if (md5sum1.equals(md5sum2 = SQLiteJDBCLoader.md5sum(new FileInputStream(extractedLibFile)))) {
                    return SQLiteJDBCLoader.loadNativeLibrary(targetFolder, extractedLibFileName);
                }
                boolean deletionSucceeded = extractedLibFile.delete();
                if (!deletionSucceeded) {
                    throw new IOException("failed to remove existing native library file: " + extractedLibFile.getAbsolutePath());
                }
            }
            InputStream reader = SQLiteJDBCLoader.class.getResourceAsStream(nativeLibraryFilePath);
            FileOutputStream writer = new FileOutputStream(extractedLibFile);
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytesRead);
            }
            writer.close();
            reader.close();
            if (!System.getProperty("os.name").contains("Windows")) {
                try {
                    Runtime.getRuntime().exec(new String[]{"chmod", "755", extractedLibFile.getAbsolutePath()}).waitFor();
                }
                catch (Throwable e) {
                    // empty catch block
                }
            }
            return SQLiteJDBCLoader.loadNativeLibrary(targetFolder, extractedLibFileName);
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    private static synchronized boolean loadNativeLibrary(String path, String name) {
        File libPath = new File(path, name);
        if (libPath.exists()) {
            try {
                System.load(new File(path, name).getAbsolutePath());
                return true;
            }
            catch (UnsatisfiedLinkError e) {
                System.err.println(e);
                return false;
            }
        }
        return false;
    }

    private static void loadSQLiteNativeLibrary() {
        if (extracted) {
            return;
        }
        boolean runInPureJavaMode = SQLiteJDBCLoader.getPureJavaFlag();
        if (runInPureJavaMode) {
            extracted = false;
            return;
        }
        String sqliteNativeLibraryPath = System.getProperty("org.sqlite.lib.path");
        String sqliteNativeLibraryName = System.getProperty("org.sqlite.lib.name");
        if (sqliteNativeLibraryName == null) {
            sqliteNativeLibraryName = System.mapLibraryName("sqlitejdbc");
        }
        if (sqliteNativeLibraryPath != null && SQLiteJDBCLoader.loadNativeLibrary(sqliteNativeLibraryPath, sqliteNativeLibraryName)) {
            extracted = true;
            return;
        }
        sqliteNativeLibraryPath = "/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
        if (SQLiteJDBCLoader.class.getResource(sqliteNativeLibraryPath + "/" + sqliteNativeLibraryName) == null) {
            return;
        }
        String tempFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        if (SQLiteJDBCLoader.extractAndLoadLibraryFile(sqliteNativeLibraryPath, sqliteNativeLibraryName, tempFolder)) {
            extracted = true;
            return;
        }
        extracted = false;
    }

    private static void getNativeLibraryFolderForTheCurrentOS() {
        String osName = OSInfo.getOSName();
        String archName = OSInfo.getArchName();
    }

    public static int getMajorVersion() {
        String[] c = SQLiteJDBCLoader.getVersion().split("\\.");
        return c.length > 0 ? Integer.parseInt(c[0]) : 1;
    }

    public static int getMinorVersion() {
        String[] c = SQLiteJDBCLoader.getVersion().split("\\.");
        return c.length > 1 ? Integer.parseInt(c[1]) : 0;
    }

    public static String getVersion() {
        URL versionFile = SQLiteJDBCLoader.class.getResource("/META-INF/maven/org.xerial/sqlite-jdbc/pom.properties");
        if (versionFile == null) {
            versionFile = SQLiteJDBCLoader.class.getResource("/META-INF/maven/org.xerial/sqlite-jdbc/VERSION");
        }
        String version = "unknown";
        try {
            if (versionFile != null) {
                Properties versionData = new Properties();
                versionData.load(versionFile.openStream());
                version = versionData.getProperty("version", version);
                version = version.trim().replaceAll("[^0-9\\.]", "");
            }
        }
        catch (IOException e) {
            System.err.println(e);
        }
        return version;
    }
}

