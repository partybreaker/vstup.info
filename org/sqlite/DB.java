/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.sqlite.Codes;
import org.sqlite.Conn;
import org.sqlite.Function;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.Stmt;

abstract class DB
implements Codes {
    Conn conn = null;
    long begin = 0;
    long commit = 0;
    private final Map<Long, Stmt> stmts = new HashMap<Long, Stmt>();

    DB() {
    }

    abstract void interrupt() throws SQLException;

    abstract void busy_timeout(int var1) throws SQLException;

    abstract String errmsg() throws SQLException;

    abstract String libversion() throws SQLException;

    abstract int changes() throws SQLException;

    abstract int shared_cache(boolean var1) throws SQLException;

    abstract int enable_load_extension(boolean var1) throws SQLException;

    final synchronized void exec(String sql) throws SQLException {
        long pointer = 0;
        try {
            pointer = this.prepare(sql);
            switch (this.step(pointer)) {
                case 101: {
                    this.ensureAutoCommit();
                    return;
                }
                case 100: {
                    return;
                }
            }
            this.throwex();
        }
        finally {
            this.finalize(pointer);
        }
    }

    final synchronized void open(Conn conn, String file, int openFlags) throws SQLException {
        this.conn = conn;
        this._open(file, openFlags);
    }

    final synchronized void close() throws SQLException {
        Map<Long, Stmt> map = this.stmts;
        synchronized (map) {
            Iterator<Map.Entry<Long, Stmt>> i = this.stmts.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Long, Stmt> entry = i.next();
                Stmt stmt = entry.getValue();
                this.finalize(entry.getKey());
                if (stmt != null) {
                    stmt.pointer = 0;
                }
                i.remove();
            }
        }
        this.free_functions();
        if (this.begin != 0) {
            this.finalize(this.begin);
            this.begin = 0;
        }
        if (this.commit != 0) {
            this.finalize(this.commit);
            this.commit = 0;
        }
        this._close();
    }

    final synchronized void prepare(Stmt stmt) throws SQLException {
        if (stmt.pointer != 0) {
            this.finalize(stmt);
        }
        stmt.pointer = this.prepare(stmt.sql);
        this.stmts.put(new Long(stmt.pointer), stmt);
    }

    final synchronized int finalize(Stmt stmt) throws SQLException {
        if (stmt.pointer == 0) {
            return 0;
        }
        int rc = 1;
        try {
            rc = this.finalize(stmt.pointer);
        }
        finally {
            this.stmts.remove(new Long(stmt.pointer));
            stmt.pointer = 0;
        }
        return rc;
    }

    protected abstract void _open(String var1, int var2) throws SQLException;

    protected abstract void _close() throws SQLException;

    protected abstract int _exec(String var1) throws SQLException;

    protected abstract long prepare(String var1) throws SQLException;

    protected abstract int finalize(long var1) throws SQLException;

    protected abstract int step(long var1) throws SQLException;

    protected abstract int reset(long var1) throws SQLException;

    abstract int clear_bindings(long var1) throws SQLException;

    abstract int bind_parameter_count(long var1) throws SQLException;

    abstract int column_count(long var1) throws SQLException;

    abstract int column_type(long var1, int var3) throws SQLException;

    abstract String column_decltype(long var1, int var3) throws SQLException;

    abstract String column_table_name(long var1, int var3) throws SQLException;

    abstract String column_name(long var1, int var3) throws SQLException;

    abstract String column_text(long var1, int var3) throws SQLException;

    abstract byte[] column_blob(long var1, int var3) throws SQLException;

    abstract double column_double(long var1, int var3) throws SQLException;

    abstract long column_long(long var1, int var3) throws SQLException;

    abstract int column_int(long var1, int var3) throws SQLException;

    abstract int bind_null(long var1, int var3) throws SQLException;

    abstract int bind_int(long var1, int var3, int var4) throws SQLException;

    abstract int bind_long(long var1, int var3, long var4) throws SQLException;

    abstract int bind_double(long var1, int var3, double var4) throws SQLException;

    abstract int bind_text(long var1, int var3, String var4) throws SQLException;

    abstract int bind_blob(long var1, int var3, byte[] var4) throws SQLException;

    abstract void result_null(long var1) throws SQLException;

    abstract void result_text(long var1, String var3) throws SQLException;

    abstract void result_blob(long var1, byte[] var3) throws SQLException;

    abstract void result_double(long var1, double var3) throws SQLException;

    abstract void result_long(long var1, long var3) throws SQLException;

    abstract void result_int(long var1, int var3) throws SQLException;

    abstract void result_error(long var1, String var3) throws SQLException;

    abstract int value_bytes(Function var1, int var2) throws SQLException;

    abstract String value_text(Function var1, int var2) throws SQLException;

    abstract byte[] value_blob(Function var1, int var2) throws SQLException;

    abstract double value_double(Function var1, int var2) throws SQLException;

    abstract long value_long(Function var1, int var2) throws SQLException;

    abstract int value_int(Function var1, int var2) throws SQLException;

    abstract int value_type(Function var1, int var2) throws SQLException;

    abstract int create_function(String var1, Function var2) throws SQLException;

    abstract int destroy_function(String var1) throws SQLException;

    abstract void free_functions() throws SQLException;

    abstract int backup(String var1, String var2, ProgressObserver var3) throws SQLException;

    abstract int restore(String var1, String var2, ProgressObserver var3) throws SQLException;

    abstract boolean[][] column_metadata(long var1) throws SQLException;

    final synchronized String[] column_names(long stmt) throws SQLException {
        String[] names = new String[this.column_count(stmt)];
        for (int i = 0; i < names.length; ++i) {
            names[i] = this.column_name(stmt, i);
        }
        return names;
    }

    final synchronized int sqlbind(long stmt, int pos, Object v) throws SQLException {
        ++pos;
        if (v == null) {
            return this.bind_null(stmt, pos);
        }
        if (v instanceof Integer) {
            return this.bind_int(stmt, pos, (Integer)v);
        }
        if (v instanceof Short) {
            return this.bind_int(stmt, pos, ((Short)v).intValue());
        }
        if (v instanceof Long) {
            return this.bind_long(stmt, pos, (Long)v);
        }
        if (v instanceof Float) {
            return this.bind_double(stmt, pos, ((Float)v).doubleValue());
        }
        if (v instanceof Double) {
            return this.bind_double(stmt, pos, (Double)v);
        }
        if (v instanceof String) {
            return this.bind_text(stmt, pos, (String)v);
        }
        if (v instanceof byte[]) {
            return this.bind_blob(stmt, pos, (byte[])v);
        }
        throw new SQLException("unexpected param type: " + v.getClass());
    }

    final synchronized int[] executeBatch(long stmt, int count, Object[] vals) throws SQLException {
        int[] changes;
        if (count < 1) {
            throw new SQLException("count (" + count + ") < 1");
        }
        int params = this.bind_parameter_count(stmt);
        changes = new int[count];
        try {
            for (int i = 0; i < count; ++i) {
                this.reset(stmt);
                for (int j = 0; j < params; ++j) {
                    if (this.sqlbind(stmt, j, vals[i * params + j]) == 0) continue;
                    this.throwex();
                }
                int rc = this.step(stmt);
                if (rc != 101) {
                    this.reset(stmt);
                    if (rc == 100) {
                        throw new BatchUpdateException("batch entry " + i + ": query returns results", changes);
                    }
                    this.throwex();
                }
                changes[i] = this.changes();
            }
        }
        finally {
            this.ensureAutoCommit();
        }
        this.reset(stmt);
        return changes;
    }

    final synchronized boolean execute(Stmt stmt, Object[] vals) throws SQLException {
        if (vals != null) {
            int params = this.bind_parameter_count(stmt.pointer);
            if (params != vals.length) {
                throw new SQLException("assertion failure: param count (" + params + ") != value count (" + vals.length + ")");
            }
            for (int i = 0; i < params; ++i) {
                if (this.sqlbind(stmt.pointer, i, vals[i]) == 0) continue;
                this.throwex();
            }
        }
        int statusCode = this.step(stmt.pointer);
        switch (statusCode) {
            case 101: {
                this.reset(stmt.pointer);
                this.ensureAutoCommit();
                return false;
            }
            case 100: {
                return true;
            }
            case 5: 
            case 6: 
            case 21: {
                throw this.newSQLException(statusCode);
            }
        }
        this.finalize(stmt);
        throw this.newSQLException(statusCode);
    }

    final synchronized boolean execute(String sql) throws SQLException {
        int statusCode = this._exec(sql);
        switch (statusCode) {
            case 0: {
                return false;
            }
            case 101: {
                this.ensureAutoCommit();
                return false;
            }
            case 100: {
                return true;
            }
        }
        throw this.newSQLException(statusCode);
    }

    final synchronized int executeUpdate(Stmt stmt, Object[] vals) throws SQLException {
        if (this.execute(stmt, vals)) {
            throw new SQLException("query returns results");
        }
        this.reset(stmt.pointer);
        return this.changes();
    }

    final void throwex() throws SQLException {
        throw new SQLException(this.errmsg());
    }

    final void throwex(int errorCode) throws SQLException {
        throw this.newSQLException(errorCode);
    }

    final void throwex(int errorCode, String errorMessage) throws SQLException {
        throw DB.newSQLException(errorCode, errorMessage);
    }

    static SQLException newSQLException(int errorCode, String errorMessage) throws SQLException {
        SQLiteErrorCode code = SQLiteErrorCode.getErrorCode(errorCode);
        return new SQLException(String.format("%s (%s)", new Object[]{code, errorMessage}));
    }

    private SQLException newSQLException(int errorCode) throws SQLException {
        return DB.newSQLException(errorCode, this.errmsg());
    }

    final void ensureAutoCommit() throws SQLException {
        if (!this.conn.getAutoCommit()) {
            return;
        }
        if (this.begin == 0) {
            this.begin = this.prepare("begin;");
        }
        if (this.commit == 0) {
            this.commit = this.prepare("commit;");
        }
        try {
            if (this.step(this.begin) != 101) {
                return;
            }
            if (this.step(this.commit) != 101) {
                this.reset(this.commit);
                this.throwex();
            }
        }
        finally {
            this.reset(this.begin);
            this.reset(this.commit);
        }
    }

    public static interface ProgressObserver {
        public void progress(int var1, int var2);
    }

}

