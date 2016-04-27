/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.Conn;
import org.sqlite.DB;

public abstract class Function {
    private Conn conn;
    private DB db;
    long context = 0;
    long value = 0;
    int args = 0;

    public static final void create(Connection conn, String name, Function f) throws SQLException {
        if (conn == null || !(conn instanceof Conn)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }
        f.conn = (Conn)conn;
        f.db = f.conn.db();
        if (name == null || name.length() > 255) {
            throw new SQLException("invalid function name: '" + name + "'");
        }
        if (f.db.create_function(name, f) != 0) {
            throw new SQLException("error creating function");
        }
    }

    public static final void destroy(Connection conn, String name) throws SQLException {
        if (conn == null || !(conn instanceof Conn)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        ((Conn)conn).db().destroy_function(name);
    }

    protected abstract void xFunc() throws SQLException;

    protected final synchronized int args() throws SQLException {
        this.checkContext();
        return this.args;
    }

    protected final synchronized void result(byte[] value) throws SQLException {
        this.checkContext();
        this.db.result_blob(this.context, value);
    }

    protected final synchronized void result(double value) throws SQLException {
        this.checkContext();
        this.db.result_double(this.context, value);
    }

    protected final synchronized void result(int value) throws SQLException {
        this.checkContext();
        this.db.result_int(this.context, value);
    }

    protected final synchronized void result(long value) throws SQLException {
        this.checkContext();
        this.db.result_long(this.context, value);
    }

    protected final synchronized void result() throws SQLException {
        this.checkContext();
        this.db.result_null(this.context);
    }

    protected final synchronized void result(String value) throws SQLException {
        this.checkContext();
        this.db.result_text(this.context, value);
    }

    protected final synchronized void error(String err) throws SQLException {
        this.checkContext();
        this.db.result_error(this.context, err);
    }

    protected final synchronized int value_bytes(int arg) throws SQLException {
        this.checkValue(arg);
        return this.db.value_bytes(this, arg);
    }

    protected final synchronized String value_text(int arg) throws SQLException {
        this.checkValue(arg);
        return this.db.value_text(this, arg);
    }

    protected final synchronized byte[] value_blob(int arg) throws SQLException {
        this.checkValue(arg);
        return this.db.value_blob(this, arg);
    }

    protected final synchronized double value_double(int arg) throws SQLException {
        this.checkValue(arg);
        return this.db.value_double(this, arg);
    }

    protected final synchronized int value_int(int arg) throws SQLException {
        this.checkValue(arg);
        return this.db.value_int(this, arg);
    }

    protected final synchronized long value_long(int arg) throws SQLException {
        this.checkValue(arg);
        return this.db.value_long(this, arg);
    }

    protected final synchronized int value_type(int arg) throws SQLException {
        this.checkValue(arg);
        return this.db.value_type(this, arg);
    }

    private void checkContext() throws SQLException {
        if (this.conn == null || this.conn.db() == null || this.context == 0) {
            throw new SQLException("no context, not allowed to read value");
        }
    }

    private void checkValue(int arg) throws SQLException {
        if (this.conn == null || this.conn.db() == null || this.value == 0) {
            throw new SQLException("not in value access state");
        }
        if (arg >= this.args) {
            throw new SQLException("arg " + arg + " out bounds [0," + this.args + ")");
        }
    }

    public static abstract class Aggregate
    extends Function
    implements Cloneable {
        protected final void xFunc() {
        }

        protected abstract void xStep() throws SQLException;

        protected abstract void xFinal() throws SQLException;

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

}

