/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.sqlite.JDBC;
import org.sqlite.SQLiteOpenMode;

public class SQLiteConfig {
    private final Properties pragmaTable;
    private int openModeFlag = 0;
    private static final String[] OnOff = new String[]{"true", "false"};

    public SQLiteConfig() {
        this(new Properties());
    }

    public SQLiteConfig(Properties prop) {
        this.pragmaTable = prop;
        String openMode = this.pragmaTable.getProperty(Pragma.OPEN_MODE.pragmaName);
        if (openMode != null) {
            this.openModeFlag = Integer.parseInt(openMode);
        } else {
            this.setOpenMode(SQLiteOpenMode.READWRITE);
            this.setOpenMode(SQLiteOpenMode.CREATE);
        }
    }

    public Connection createConnection(String url) throws SQLException {
        return JDBC.createConnection(url, this.toProperties());
    }

    public void apply(Connection conn) throws SQLException {
        HashSet<String> pragmaParams = new HashSet<String>();
        for (Pragma each2 : Pragma.values()) {
            pragmaParams.add(each2.pragmaName);
        }
        pragmaParams.remove(Pragma.OPEN_MODE.pragmaName);
        pragmaParams.remove(Pragma.SHARED_CACHE.pragmaName);
        pragmaParams.remove(Pragma.LOAD_EXTENSION.pragmaName);
        Statement stat = conn.createStatement();
        try {
            int count = 0;
            for (Object each : this.pragmaTable.keySet()) {
                String value;
                String key = each.toString();
                if (!pragmaParams.contains(key) || (value = this.pragmaTable.getProperty(key)) == null) continue;
                String sql = String.format("pragma %s=%s", key, value);
                stat.addBatch(sql);
                ++count;
            }
            if (count > 0) {
                stat.executeBatch();
            }
        }
        finally {
            if (stat != null) {
                stat.close();
            }
        }
    }

    private void set(Pragma pragma, boolean flag) {
        this.setPragma(pragma, Boolean.toString(flag));
    }

    private void set(Pragma pragma, int num) {
        this.setPragma(pragma, Integer.toString(num));
    }

    private boolean getBoolean(Pragma pragma, String defaultValue) {
        return Boolean.parseBoolean(this.pragmaTable.getProperty(pragma.pragmaName, defaultValue));
    }

    public boolean isEnabledSharedCache() {
        return this.getBoolean(Pragma.SHARED_CACHE, "false");
    }

    public boolean isEnabledLoadExtension() {
        return this.getBoolean(Pragma.LOAD_EXTENSION, "false");
    }

    public int getOpenModeFlags() {
        return this.openModeFlag;
    }

    public void setPragma(Pragma pragma, String value) {
        this.pragmaTable.put(pragma.pragmaName, value);
    }

    public Properties toProperties() {
        this.pragmaTable.setProperty(Pragma.OPEN_MODE.pragmaName, Integer.toString(this.openModeFlag));
        return this.pragmaTable;
    }

    static DriverPropertyInfo[] getDriverPropertyInfo() {
        Pragma[] pragma = Pragma.values();
        DriverPropertyInfo[] result = new DriverPropertyInfo[pragma.length];
        int index = 0;
        for (Pragma p : Pragma.values()) {
            DriverPropertyInfo di = new DriverPropertyInfo(p.pragmaName, null);
            di.choices = p.choices;
            di.description = p.description;
            di.required = false;
            result[index++] = di;
        }
        return result;
    }

    public void setOpenMode(SQLiteOpenMode mode) {
        this.openModeFlag |= mode.flag;
    }

    public void resetOpenMode(SQLiteOpenMode mode) {
        this.openModeFlag &= ~ mode.flag;
    }

    public void setSharedCache(boolean enable) {
        this.set(Pragma.SHARED_CACHE, enable);
    }

    public void enableLoadExtension(boolean enable) {
        this.set(Pragma.LOAD_EXTENSION, enable);
    }

    public void setReadOnly(boolean readOnly) {
        if (readOnly) {
            this.setOpenMode(SQLiteOpenMode.READONLY);
            this.resetOpenMode(SQLiteOpenMode.READWRITE);
        } else {
            this.setOpenMode(SQLiteOpenMode.READWRITE);
            this.resetOpenMode(SQLiteOpenMode.READONLY);
        }
    }

    public void setCacheSize(int numberOfPages) {
        this.set(Pragma.CACHE_SIZE, numberOfPages);
    }

    public void enableCaseSensitiveLike(boolean enable) {
        this.set(Pragma.CASE_SENSITIVE_LIKE, enable);
    }

    public void enableCountChanges(boolean enable) {
        this.set(Pragma.COUNT_CHANGES, enable);
    }

    public void setDefaultCacheSize(int numberOfPages) {
        this.set(Pragma.DEFAULT_CACHE_SIZE, numberOfPages);
    }

    public void enableEmptyResultCallBacks(boolean enable) {
        this.set(Pragma.EMPTY_RESULT_CALLBACKS, enable);
    }

    private static String[] toStringArray(PragmaValue[] list) {
        String[] result = new String[list.length];
        for (int i = 0; i < list.length; ++i) {
            result[i] = list[i].getValue();
        }
        return result;
    }

    public void setEncoding(Encoding encoding) {
        this.setPragma(Pragma.ENCODING, encoding.typeName);
    }

    public void enforceForeignKeys(boolean enforce) {
        this.set(Pragma.FOREIGN_KEYS, enforce);
    }

    public void enableFullColumnNames(boolean enable) {
        this.set(Pragma.FULL_COLUMN_NAMES, enable);
    }

    public void enableFullSync(boolean enable) {
        this.set(Pragma.FULL_SYNC, enable);
    }

    public void incrementalVacuum(int numberOfPagesToBeRemoved) {
        this.set(Pragma.INCREMENTAL_VACUUM, numberOfPagesToBeRemoved);
    }

    public void setJournalMode(JournalMode mode) {
        this.setPragma(Pragma.JOURNAL_MODE, mode.name());
    }

    public void setJounalSizeLimit(int limit) {
        this.set(Pragma.JOURNAL_SIZE_LIMIT, limit);
    }

    public void useLegacyFileFormat(boolean use) {
        this.set(Pragma.LEGACY_FILE_FORMAT, use);
    }

    public void setLockingMode(LockingMode mode) {
        this.setPragma(Pragma.LOCKING_MODE, mode.name());
    }

    public void setPageSize(int numBytes) {
        this.set(Pragma.PAGE_SIZE, numBytes);
    }

    public void setMaxPageCount(int numPages) {
        this.set(Pragma.MAX_PAGE_COUNT, numPages);
    }

    public void setReadUncommited(boolean useReadUncommitedIsolationMode) {
        this.set(Pragma.READ_UNCOMMITED, useReadUncommitedIsolationMode);
    }

    public void enableRecursiveTriggers(boolean enable) {
        this.set(Pragma.RECURSIVE_TRIGGERS, enable);
    }

    public void enableReverseUnorderedSelects(boolean enable) {
        this.set(Pragma.REVERSE_UNORDERED_SELECTS, enable);
    }

    public void enableShortColumnNames(boolean enable) {
        this.set(Pragma.SHORT_COLUMN_NAMES, enable);
    }

    public void setSynchronous(SynchronousMode mode) {
        this.setPragma(Pragma.SYNCHRONOUS, mode.name());
    }

    public void setTempStore(TempStore storeType) {
        this.setPragma(Pragma.TEMP_STORE, storeType.name());
    }

    public void setTempStoreDirectory(String directoryName) {
        this.setPragma(Pragma.TEMP_STORE_DIRECTORY, String.format("'%s'", directoryName));
    }

    public void setUserVersion(int version) {
        this.set(Pragma.USER_VERSION, version);
    }

    static /* synthetic */ String[] access$000() {
        return OnOff;
    }

    static /* synthetic */ String[] access$100(PragmaValue[] x0) {
        return SQLiteConfig.toStringArray(x0);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum TempStore implements PragmaValue
    {
        DEFAULT,
        FILE,
        MEMORY;
        

        private TempStore() {
        }

        @Override
        public String getValue() {
            return this.name();
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum SynchronousMode implements PragmaValue
    {
        OFF,
        NORMAL,
        FULL;
        

        private SynchronousMode() {
        }

        @Override
        public String getValue() {
            return this.name();
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum LockingMode implements PragmaValue
    {
        NORMAL,
        EXCLUSIVE;
        

        private LockingMode() {
        }

        @Override
        public String getValue() {
            return this.name();
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum JournalMode implements PragmaValue
    {
        DELETE,
        TRUNCATE,
        PERSIST,
        MEMORY,
        OFF;
        

        private JournalMode() {
        }

        @Override
        public String getValue() {
            return this.name();
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum Encoding implements PragmaValue
    {
        UTF8("UTF-8"),
        UTF16("UTF-16"),
        UTF16_LITTLE_ENDIAN("UTF-16le"),
        UTF16_BIG_ENDIAN("UTF-16be");
        
        public final String typeName;

        private Encoding(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String getValue() {
            return this.typeName;
        }
    }

    private static interface PragmaValue {
        public String getValue();
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static enum Pragma {
        OPEN_MODE("open_mode", "Database open-mode flag", null),
        SHARED_CACHE("shared_cache", "Enablse SQLite Shared-Cache mode, native driver only", SQLiteConfig.access$000()),
        LOAD_EXTENSION("enable_load_extension", "Enable SQLite load_extention() function, native driver only", SQLiteConfig.access$000()),
        CACHE_SIZE("cache_size"),
        CASE_SENSITIVE_LIKE("case_sensitive_like", SQLiteConfig.access$000()),
        COUNT_CHANGES("count_changes", SQLiteConfig.access$000()),
        DEFAULT_CACHE_SIZE("default_cache_size"),
        EMPTY_RESULT_CALLBACKS("empty_result_callback", SQLiteConfig.access$000()),
        ENCODING("encoding", SQLiteConfig.access$100(Encoding.values())),
        FOREIGN_KEYS("foreign_keys", SQLiteConfig.access$000()),
        FULL_COLUMN_NAMES("full_column_names", SQLiteConfig.access$000()),
        FULL_SYNC("fullsync", SQLiteConfig.access$000()),
        INCREMENTAL_VACUUM("incremental_vacuum"),
        JOURNAL_MODE("journal_mode", SQLiteConfig.access$100(JournalMode.values())),
        JOURNAL_SIZE_LIMIT("journal_size_limit"),
        LEGACY_FILE_FORMAT("legacy_file_format", SQLiteConfig.access$000()),
        LOCKING_MODE("locking_mode", SQLiteConfig.access$100(LockingMode.values())),
        PAGE_SIZE("page_size"),
        MAX_PAGE_COUNT("max_page_count"),
        READ_UNCOMMITED("read_uncommited", SQLiteConfig.access$000()),
        RECURSIVE_TRIGGERS("recursive_triggers", SQLiteConfig.access$000()),
        REVERSE_UNORDERED_SELECTS("reverse_unordered_selects", SQLiteConfig.access$000()),
        SHORT_COLUMN_NAMES("short_column_names", SQLiteConfig.access$000()),
        SYNCHRONOUS("synchronous", SQLiteConfig.access$100(SynchronousMode.values())),
        TEMP_STORE("temp_store", SQLiteConfig.access$100(TempStore.values())),
        TEMP_STORE_DIRECTORY("temp_store_directory"),
        USER_VERSION("user_version");
        
        public final String pragmaName;
        public final String[] choices;
        public final String description;

        private Pragma(String pragmaName) {
            this(pragmaName, null);
        }

        private Pragma(String pragmaName, String[] choices) {
            this(pragmaName, null, null);
        }

        private Pragma(String pragmaName, String description, String[] choices) {
            this.pragmaName = pragmaName;
            this.description = description;
            this.choices = choices;
        }
    }

}

