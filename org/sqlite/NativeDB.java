/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.sql.SQLException;
import org.sqlite.DB;
import org.sqlite.Function;
import org.sqlite.SQLiteJDBCLoader;

final class NativeDB
extends DB {
    long pointer = 0;
    private static boolean isLoaded = false;
    private static boolean loadSucceeded = false;
    private final long udfdatalist = 0;

    NativeDB() {
    }

    static boolean load() {
        if (isLoaded) {
            return loadSucceeded;
        }
        loadSucceeded = SQLiteJDBCLoader.initialize();
        isLoaded = true;
        return loadSucceeded;
    }

    protected synchronized native void _open(String var1, int var2) throws SQLException;

    protected synchronized native void _close() throws SQLException;

    protected synchronized native int _exec(String var1) throws SQLException;

    synchronized native int shared_cache(boolean var1);

    synchronized native int enable_load_extension(boolean var1);

    synchronized native void interrupt();

    synchronized native void busy_timeout(int var1);

    protected synchronized native long prepare(String var1) throws SQLException;

    synchronized native String errmsg();

    synchronized native String libversion();

    synchronized native int changes();

    protected synchronized native int finalize(long var1);

    protected synchronized native int step(long var1);

    protected synchronized native int reset(long var1);

    synchronized native int clear_bindings(long var1);

    synchronized native int bind_parameter_count(long var1);

    synchronized native int column_count(long var1);

    synchronized native int column_type(long var1, int var3);

    synchronized native String column_decltype(long var1, int var3);

    synchronized native String column_table_name(long var1, int var3);

    synchronized native String column_name(long var1, int var3);

    synchronized native String column_text(long var1, int var3);

    synchronized native byte[] column_blob(long var1, int var3);

    synchronized native double column_double(long var1, int var3);

    synchronized native long column_long(long var1, int var3);

    synchronized native int column_int(long var1, int var3);

    synchronized native int bind_null(long var1, int var3);

    synchronized native int bind_int(long var1, int var3, int var4);

    synchronized native int bind_long(long var1, int var3, long var4);

    synchronized native int bind_double(long var1, int var3, double var4);

    synchronized native int bind_text(long var1, int var3, String var4);

    synchronized native int bind_blob(long var1, int var3, byte[] var4);

    synchronized native void result_null(long var1);

    synchronized native void result_text(long var1, String var3);

    synchronized native void result_blob(long var1, byte[] var3);

    synchronized native void result_double(long var1, double var3);

    synchronized native void result_long(long var1, long var3);

    synchronized native void result_int(long var1, int var3);

    synchronized native void result_error(long var1, String var3);

    synchronized native int value_bytes(Function var1, int var2);

    synchronized native String value_text(Function var1, int var2);

    synchronized native byte[] value_blob(Function var1, int var2);

    synchronized native double value_double(Function var1, int var2);

    synchronized native long value_long(Function var1, int var2);

    synchronized native int value_int(Function var1, int var2);

    synchronized native int value_type(Function var1, int var2);

    synchronized native int create_function(String var1, Function var2);

    synchronized native int destroy_function(String var1);

    synchronized native void free_functions();

    synchronized native int backup(String var1, String var2, DB.ProgressObserver var3) throws SQLException;

    synchronized native int restore(String var1, String var2, DB.ProgressObserver var3) throws SQLException;

    synchronized native boolean[][] column_metadata(long var1);

    static void throwex(String msg) throws SQLException {
        throw new SQLException(msg);
    }
}

