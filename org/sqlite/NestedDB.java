/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.io.PrintWriter;
import java.sql.SQLException;
import org.ibex.nestedvm.Runtime;
import org.sqlite.DB;
import org.sqlite.Function;
import org.sqlite.SQLiteErrorCode;

final class NestedDB
extends DB
implements Runtime.CallJavaCB {
    int handle = 0;
    private Runtime rt = null;
    private Function[] functions = null;
    private String[] funcNames = null;
    private final int[] p0 = new int[0];
    private final int[] p1 = new int[]{0};
    private final int[] p2 = new int[]{0, 0};
    private final int[] p3 = new int[]{0, 0, 0};
    private final int[] p4 = new int[]{0, 0, 0, 0};
    private final int[] p5 = new int[]{0, 0, 0, 0, 0};

    NestedDB() {
    }

    protected synchronized void _open(String filename, int openFlags) throws SQLException {
        if (this.handle != 0) {
            throw new SQLException("DB already open");
        }
        if (filename.length() > 2) {
            char drive = Character.toLowerCase(filename.charAt(0));
            if (filename.charAt(1) == ':' && drive >= 'a' && drive <= 'z') {
                filename = filename.substring(2);
                filename = filename.replace('\\', '/');
                filename = "/" + drive + ":" + filename;
            }
        }
        try {
            this.rt = (Runtime)Class.forName("org.sqlite.SQLite").newInstance();
            this.rt.start();
        }
        catch (Exception e) {
            throw new CausedSQLException(e);
        }
        this.rt.setCallJavaCB(this);
        int passback = this.rt.xmalloc(4);
        int str = this.rt.strdup(filename);
        if (this.call("sqlite3_open_v2", str, passback, openFlags, 0) != 0) {
            this.throwex();
        }
        this.handle = this.deref(passback);
        this.rt.free(str);
        this.rt.free(passback);
    }

    public int call(int xType, int context, int args, int value) {
        this.xUDF(xType, context, args, value);
        return 0;
    }

    protected synchronized void _close() throws SQLException {
        if (this.handle == 0) {
            return;
        }
        try {
            if (this.call("sqlite3_close", this.handle) != 0) {
                this.throwex();
            }
        }
        finally {
            this.handle = 0;
            this.rt.stop();
            this.rt = null;
        }
    }

    int shared_cache(boolean enable) throws SQLException {
        return -1;
    }

    int enable_load_extension(boolean enable) throws SQLException {
        return 1;
    }

    synchronized void interrupt() throws SQLException {
        this.call("sqlite3_interrupt", this.handle);
    }

    synchronized void busy_timeout(int ms) throws SQLException {
        this.call("sqlite3_busy_timeout", this.handle, ms);
    }

    protected synchronized long prepare(String sql) throws SQLException {
        int passback = this.rt.xmalloc(4);
        int str = this.rt.strdup(sql);
        int ret = this.call("sqlite3_prepare_v2", this.handle, str, -1, passback, 0);
        this.rt.free(str);
        if (ret != 0) {
            this.rt.free(passback);
            this.throwex(ret);
        }
        int pointer = this.deref(passback);
        this.rt.free(passback);
        return pointer;
    }

    synchronized String errmsg() throws SQLException {
        return this.cstring(this.call("sqlite3_errmsg", this.handle));
    }

    synchronized String libversion() throws SQLException {
        return this.cstring(this.call("sqlite3_libversion", this.handle));
    }

    synchronized int changes() throws SQLException {
        return this.call("sqlite3_changes", this.handle);
    }

    protected synchronized int _exec(String sql) throws SQLException {
        if (this.rt == null) {
            throw DB.newSQLException(SQLiteErrorCode.SQLITE_MISUSE.code, "attempt to use the closed conection");
        }
        int passback = this.rt.xmalloc(4);
        int str = this.rt.strdup(sql);
        int status = this.call("sqlite3_exec", this.handle, str, 0, 0, passback);
        if (status != 0) {
            String errorMessage = this.cstring(passback);
            this.call("sqlite3_free", this.deref(passback));
            this.rt.free(passback);
            this.throwex(status, errorMessage);
        }
        this.rt.free(passback);
        return status;
    }

    protected synchronized int finalize(long stmt) throws SQLException {
        return this.call("sqlite3_finalize", (int)stmt);
    }

    protected synchronized int step(long stmt) throws SQLException {
        return this.call("sqlite3_step", (int)stmt);
    }

    protected synchronized int reset(long stmt) throws SQLException {
        return this.call("sqlite3_reset", (int)stmt);
    }

    synchronized int clear_bindings(long stmt) throws SQLException {
        return this.call("sqlite3_clear_bindings", (int)stmt);
    }

    synchronized int bind_parameter_count(long stmt) throws SQLException {
        return this.call("sqlite3_bind_parameter_count", (int)stmt);
    }

    synchronized int column_count(long stmt) throws SQLException {
        return this.call("sqlite3_column_count", (int)stmt);
    }

    synchronized int column_type(long stmt, int col) throws SQLException {
        return this.call("sqlite3_column_type", (int)stmt, col);
    }

    synchronized String column_name(long stmt, int col) throws SQLException {
        return this.utfstring(this.call("sqlite3_column_name", (int)stmt, col));
    }

    synchronized String column_text(long stmt, int col) throws SQLException {
        return this.utfstring(this.call("sqlite3_column_text", (int)stmt, col));
    }

    synchronized byte[] column_blob(long stmt, int col) throws SQLException {
        int addr = this.call("sqlite3_column_blob", (int)stmt, col);
        if (addr == 0) {
            return null;
        }
        byte[] blob = new byte[this.call("sqlite3_column_bytes", (int)stmt, col)];
        this.copyin(addr, blob, blob.length);
        return blob;
    }

    synchronized double column_double(long stmt, int col) throws SQLException {
        try {
            return Double.parseDouble(this.column_text(stmt, col));
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    synchronized long column_long(long stmt, int col) throws SQLException {
        try {
            return Long.parseLong(this.column_text(stmt, col));
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    synchronized int column_int(long stmt, int col) throws SQLException {
        return this.call("sqlite3_column_int", (int)stmt, col);
    }

    synchronized String column_decltype(long stmt, int col) throws SQLException {
        return this.utfstring(this.call("sqlite3_column_decltype", (int)stmt, col));
    }

    synchronized String column_table_name(long stmt, int col) throws SQLException {
        return this.utfstring(this.call("sqlite3_column_table_name", (int)stmt, col));
    }

    synchronized int bind_null(long stmt, int pos) throws SQLException {
        return this.call("sqlite3_bind_null", (int)stmt, pos);
    }

    synchronized int bind_int(long stmt, int pos, int v) throws SQLException {
        return this.call("sqlite3_bind_int", (int)stmt, pos, v);
    }

    synchronized int bind_long(long stmt, int pos, long v) throws SQLException {
        return this.bind_text(stmt, pos, Long.toString(v));
    }

    synchronized int bind_double(long stmt, int pos, double v) throws SQLException {
        return this.bind_text(stmt, pos, Double.toString(v));
    }

    synchronized int bind_text(long stmt, int pos, String v) throws SQLException {
        if (v == null) {
            return this.bind_null(stmt, pos);
        }
        return this.call("sqlite3_bind_text", (int)stmt, pos, this.rt.strdup(v), -1, this.rt.lookupSymbol("free"));
    }

    synchronized int bind_blob(long stmt, int pos, byte[] buf) throws SQLException {
        if (buf == null || buf.length < 1) {
            return this.bind_null(stmt, pos);
        }
        int len = buf.length;
        int blob = this.rt.xmalloc(len);
        this.copyout(buf, blob, len);
        return this.call("sqlite3_bind_blob", (int)stmt, pos, blob, len, this.rt.lookupSymbol("free"));
    }

    synchronized void result_null(long cxt) throws SQLException {
        this.call("sqlite3_result_null", (int)cxt);
    }

    synchronized void result_text(long cxt, String val) throws SQLException {
        this.call("sqlite3_result_text", (int)cxt, this.rt.strdup(val), -1, this.rt.lookupSymbol("free"));
    }

    synchronized void result_blob(long cxt, byte[] val) throws SQLException {
        if (val == null || val.length == 0) {
            this.result_null(cxt);
            return;
        }
        int blob = this.rt.xmalloc(val.length);
        this.copyout(val, blob, val.length);
        this.call("sqlite3_result_blob", (int)cxt, blob, val.length, this.rt.lookupSymbol("free"));
    }

    synchronized void result_double(long cxt, double val) throws SQLException {
        this.result_text(cxt, Double.toString(val));
    }

    synchronized void result_long(long cxt, long val) throws SQLException {
        this.result_text(cxt, Long.toString(val));
    }

    synchronized void result_int(long cxt, int val) throws SQLException {
        this.call("sqlite3_result_int", (int)cxt, val);
    }

    synchronized void result_error(long cxt, String err) throws SQLException {
        int str = this.rt.strdup(err);
        this.call("sqlite3_result_error", (int)cxt, str, -1);
        this.rt.free(str);
    }

    synchronized int value_bytes(Function f, int arg) throws SQLException {
        return this.call("sqlite3_value_bytes", this.value(f, arg));
    }

    synchronized String value_text(Function f, int arg) throws SQLException {
        return this.utfstring(this.call("sqlite3_value_text", this.value(f, arg)));
    }

    synchronized byte[] value_blob(Function f, int arg) throws SQLException {
        int addr = this.call("sqlite3_value_blob", this.value(f, arg));
        if (addr == 0) {
            return null;
        }
        byte[] blob = new byte[this.value_bytes(f, arg)];
        this.copyin(addr, blob, blob.length);
        return blob;
    }

    synchronized double value_double(Function f, int arg) throws SQLException {
        return Double.parseDouble(this.value_text(f, arg));
    }

    synchronized long value_long(Function f, int arg) throws SQLException {
        return Long.parseLong(this.value_text(f, arg));
    }

    synchronized int value_int(Function f, int arg) throws SQLException {
        return this.call("sqlite3_value_int", this.value(f, arg));
    }

    synchronized int value_type(Function f, int arg) throws SQLException {
        return this.call("sqlite3_value_type", this.value(f, arg));
    }

    private int value(Function f, int arg) throws SQLException {
        return this.deref((int)f.value + arg * 4);
    }

    synchronized int create_function(String name, Function func) throws SQLException {
        int pos;
        if (this.functions == null) {
            this.functions = new Function[10];
            this.funcNames = new String[10];
        }
        for (pos = 0; pos < this.functions.length && this.functions[pos] != null; ++pos) {
        }
        if (pos == this.functions.length) {
            Function[] fnew = new Function[this.functions.length * 2];
            String[] nnew = new String[this.funcNames.length * 2];
            System.arraycopy(this.functions, 0, fnew, 0, this.functions.length);
            System.arraycopy(this.funcNames, 0, nnew, 0, this.funcNames.length);
            this.functions = fnew;
            this.funcNames = nnew;
        }
        this.functions[pos] = func;
        this.funcNames[pos] = name;
        int str = this.rt.strdup(name);
        int rc = this.call("create_function_helper", this.handle, str, pos, func instanceof Function.Aggregate ? 1 : 0);
        this.rt.free(str);
        return rc;
    }

    synchronized int destroy_function(String name) throws SQLException {
        int pos;
        if (name == null) {
            return 0;
        }
        for (pos = 0; pos < this.funcNames.length && !name.equals(this.funcNames[pos]); ++pos) {
        }
        if (pos == this.funcNames.length) {
            return 0;
        }
        this.functions[pos] = null;
        this.funcNames[pos] = null;
        int str = this.rt.strdup(name);
        int rc = this.call("create_function_helper", this.handle, str, -1, 0);
        this.rt.free(str);
        return rc;
    }

    synchronized void free_functions() {
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    synchronized void xUDF(int xType, int context, int args, int value) {
        Function func = null;
        try {
            int pos = this.call("sqlite3_user_data", context);
            func = this.functions[pos];
            if (func == null) {
                throw new SQLException("function state inconsistent");
            }
            func.context = context;
            func.value = value;
            func.args = args;
            switch (xType) {
                case 1: {
                    func.xFunc();
                    return;
                }
                case 2: {
                    ((Function.Aggregate)func).xStep();
                    return;
                }
                case 3: {
                    ((Function.Aggregate)func).xFinal();
                }
            }
        }
        catch (SQLException e) {
            try {
                String err = e.toString();
                if (err == null) {
                    err = "unknown error";
                }
                int str = this.rt.strdup(err);
                this.call("sqlite3_result_error", context, str, -1);
                this.rt.free(str);
                return;
            }
            catch (SQLException exp) {
                exp.printStackTrace();
            }
        }
        finally {
            if (func != null) {
                func.context = 0;
                func.value = 0;
                func.args = 0;
            }
        }
    }

    synchronized boolean[][] column_metadata(long stmt) throws SQLException {
        int colCount = this.call("sqlite3_column_count", (int)stmt);
        boolean[][] meta = new boolean[colCount][3];
        int pass = this.rt.xmalloc(12);
        for (int i = 0; i < colCount; ++i) {
            this.call("column_metadata_helper", this.handle, (int)stmt, i, pass);
            meta[i][0] = this.deref(pass) == 1;
            meta[i][1] = this.deref(pass + 4) == 1;
            meta[i][2] = this.deref(pass + 8) == 1;
        }
        this.rt.free(pass);
        return meta;
    }

    int backup(String dbName, String destFileName, DB.ProgressObserver observer) throws SQLException {
        throw new SQLException("backup command is not supported in pure-java mode");
    }

    int restore(String dbName, String sourceFileName, DB.ProgressObserver observer) throws SQLException {
        throw new SQLException("restore command is not supported in pure-java mode");
    }

    private int call(String addr, int a0) throws SQLException {
        this.p1[0] = a0;
        return this.call(addr, this.p1);
    }

    private int call(String addr, int a0, int a1) throws SQLException {
        this.p2[0] = a0;
        this.p2[1] = a1;
        return this.call(addr, this.p2);
    }

    private int call(String addr, int a0, int a1, int a2) throws SQLException {
        this.p3[0] = a0;
        this.p3[1] = a1;
        this.p3[2] = a2;
        return this.call(addr, this.p3);
    }

    private int call(String addr, int a0, int a1, int a2, int a3) throws SQLException {
        this.p4[0] = a0;
        this.p4[1] = a1;
        this.p4[2] = a2;
        this.p4[3] = a3;
        return this.call(addr, this.p4);
    }

    private int call(String addr, int a0, int a1, int a2, int a3, int a4) throws SQLException {
        this.p5[0] = a0;
        this.p5[1] = a1;
        this.p5[2] = a2;
        this.p5[3] = a3;
        this.p5[4] = a4;
        return this.call(addr, this.p5);
    }

    private int call(String func, int[] args) throws SQLException {
        try {
            return this.rt.call(func, args);
        }
        catch (Runtime.CallException e) {
            throw new CausedSQLException(e);
        }
    }

    private int deref(int pointer) throws SQLException {
        try {
            return this.rt.memRead(pointer);
        }
        catch (Runtime.ReadFaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private String utfstring(int str) throws SQLException {
        try {
            return this.rt.utfstring(str);
        }
        catch (Runtime.ReadFaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private String cstring(int str) throws SQLException {
        try {
            return this.rt.cstring(str);
        }
        catch (Runtime.ReadFaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private void copyin(int addr, byte[] buf, int count) throws SQLException {
        try {
            this.rt.copyin(addr, buf, count);
        }
        catch (Runtime.ReadFaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private void copyout(byte[] buf, int addr, int count) throws SQLException {
        try {
            this.rt.copyout(buf, addr, count);
        }
        catch (Runtime.FaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private static final class CausedSQLException
    extends SQLException {
        private final Exception cause;

        CausedSQLException(Exception e) {
            if (e == null) {
                throw new RuntimeException("null exception cause");
            }
            this.cause = e;
        }

        public Throwable getCause() {
            return this.cause;
        }

        public void printStackTrace() {
            this.cause.printStackTrace();
        }

        public void printStackTrace(PrintWriter s) {
            this.cause.printStackTrace(s);
        }

        public Throwable fillInStackTrace() {
            return this.cause.fillInStackTrace();
        }

        public StackTraceElement[] getStackTrace() {
            return this.cause.getStackTrace();
        }

        public String getMessage() {
            return this.cause.getMessage();
        }
    }

}

