/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import org.sqlite.Conn;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteJDBCLoader;

public class JDBC
implements Driver {
    public static final String PREFIX = "jdbc:sqlite:";

    public int getMajorVersion() {
        return SQLiteJDBCLoader.getMajorVersion();
    }

    public int getMinorVersion() {
        return SQLiteJDBCLoader.getMinorVersion();
    }

    public boolean jdbcCompliant() {
        return false;
    }

    public boolean acceptsURL(String url) {
        return JDBC.isValidURL(url);
    }

    public static boolean isValidURL(String url) {
        return url != null && url.toLowerCase().startsWith("jdbc:sqlite:");
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return SQLiteConfig.getDriverPropertyInfo();
    }

    public Connection connect(String url, Properties info) throws SQLException {
        return JDBC.createConnection(url, info);
    }

    static String extractAddress(String url) {
        return "jdbc:sqlite:".equalsIgnoreCase(url) ? ":memory:" : url.substring("jdbc:sqlite:".length());
    }

    public static Connection createConnection(String url, Properties prop) throws SQLException {
        if (!JDBC.isValidURL(url)) {
            throw new SQLException("invalid database address: " + url);
        }
        url = url.trim();
        return new Conn(url, JDBC.extractAddress(url), prop);
    }

    static {
        try {
            DriverManager.registerDriver(new JDBC());
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

