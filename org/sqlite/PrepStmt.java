/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.io.IOException;
import java.io.Reader;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import org.sqlite.Codes;
import org.sqlite.Conn;
import org.sqlite.DB;
import org.sqlite.RS;
import org.sqlite.Stmt;

final class PrepStmt
extends Stmt
implements PreparedStatement,
ParameterMetaData,
Codes {
    private int columnCount;
    private int paramCount;

    PrepStmt(Conn conn, String sql) throws SQLException {
        super(conn);
        this.sql = sql;
        this.db.prepare(this);
        this.rs.colsMeta = this.db.column_names(this.pointer);
        this.columnCount = this.db.column_count(this.pointer);
        this.paramCount = this.db.bind_parameter_count(this.pointer);
        this.batch = new Object[this.paramCount];
        this.batchPos = 0;
    }

    public void clearParameters() throws SQLException {
        this.checkOpen();
        this.db.reset(this.pointer);
        this.clearBatch();
    }

    protected void finalize() throws SQLException {
        this.close();
    }

    public boolean execute() throws SQLException {
        this.checkOpen();
        this.rs.close();
        this.db.reset(this.pointer);
        this.resultsWaiting = this.db.execute(this, this.batch);
        return this.columnCount != 0;
    }

    public ResultSet executeQuery() throws SQLException {
        this.checkOpen();
        if (this.columnCount == 0) {
            throw new SQLException("query does not return results");
        }
        this.rs.close();
        this.db.reset(this.pointer);
        this.resultsWaiting = this.db.execute(this, this.batch);
        return this.getResultSet();
    }

    public int executeUpdate() throws SQLException {
        this.checkOpen();
        if (this.columnCount != 0) {
            throw new SQLException("query returns results");
        }
        this.rs.close();
        this.db.reset(this.pointer);
        return this.db.executeUpdate(this, this.batch);
    }

    public int[] executeBatch() throws SQLException {
        if (this.batchPos == 0) {
            return new int[0];
        }
        try {
            int[] arrn = this.db.executeBatch(this.pointer, this.batchPos / this.paramCount, this.batch);
            return arrn;
        }
        finally {
            this.clearBatch();
        }
    }

    public int getUpdateCount() throws SQLException {
        this.checkOpen();
        if (this.pointer == 0 || this.resultsWaiting) {
            return -1;
        }
        return this.db.changes();
    }

    public void addBatch() throws SQLException {
        this.checkOpen();
        this.batchPos += this.paramCount;
        if (this.batchPos + this.paramCount > this.batch.length) {
            Object[] nb = new Object[this.batch.length * 2];
            System.arraycopy(this.batch, 0, nb, 0, this.batch.length);
            this.batch = nb;
        }
        System.arraycopy(this.batch, this.batchPos - this.paramCount, this.batch, this.batchPos, this.paramCount);
    }

    public ParameterMetaData getParameterMetaData() {
        return this;
    }

    public int getParameterCount() throws SQLException {
        this.checkOpen();
        return this.paramCount;
    }

    public String getParameterClassName(int param) throws SQLException {
        this.checkOpen();
        return "java.lang.String";
    }

    public String getParameterTypeName(int pos) {
        return "VARCHAR";
    }

    public int getParameterType(int pos) {
        return 12;
    }

    public int getParameterMode(int pos) {
        return 1;
    }

    public int getPrecision(int pos) {
        return 0;
    }

    public int getScale(int pos) {
        return 0;
    }

    public int isNullable(int pos) {
        return 1;
    }

    public boolean isSigned(int pos) {
        return true;
    }

    public Statement getStatement() {
        return this;
    }

    private void batch(int pos, Object value) throws SQLException {
        this.checkOpen();
        if (this.batch == null) {
            this.batch = new Object[this.paramCount];
        }
        this.batch[this.batchPos + pos - 1] = value;
    }

    public void setBoolean(int pos, boolean value) throws SQLException {
        this.setInt(pos, value ? 1 : 0);
    }

    public void setByte(int pos, byte value) throws SQLException {
        this.setInt(pos, value);
    }

    public void setBytes(int pos, byte[] value) throws SQLException {
        this.batch(pos, value);
    }

    public void setDouble(int pos, double value) throws SQLException {
        this.batch(pos, new Double(value));
    }

    public void setFloat(int pos, float value) throws SQLException {
        this.batch(pos, new Float(value));
    }

    public void setInt(int pos, int value) throws SQLException {
        this.batch(pos, new Integer(value));
    }

    public void setLong(int pos, long value) throws SQLException {
        this.batch(pos, new Long(value));
    }

    public void setNull(int pos, int u1) throws SQLException {
        this.setNull(pos, u1, null);
    }

    public void setNull(int pos, int u1, String u2) throws SQLException {
        this.batch(pos, null);
    }

    public void setObject(int pos, Object value) throws SQLException {
        if (value == null) {
            this.batch(pos, null);
        } else if (value instanceof java.util.Date) {
            this.batch(pos, new Long(((java.util.Date)value).getTime()));
        } else if (value instanceof Date) {
            this.batch(pos, new Long(((Date)value).getTime()));
        } else if (value instanceof Time) {
            this.batch(pos, new Long(((Time)value).getTime()));
        } else if (value instanceof Timestamp) {
            this.batch(pos, new Long(((Timestamp)value).getTime()));
        } else if (value instanceof Long) {
            this.batch(pos, value);
        } else if (value instanceof Integer) {
            this.batch(pos, value);
        } else if (value instanceof Short) {
            this.batch(pos, new Integer(((Short)value).intValue()));
        } else if (value instanceof Float) {
            this.batch(pos, value);
        } else if (value instanceof Double) {
            this.batch(pos, value);
        } else if (value instanceof Boolean) {
            this.setBoolean(pos, (Boolean)value);
        } else if (value instanceof byte[]) {
            this.batch(pos, value);
        } else {
            this.batch(pos, value.toString());
        }
    }

    public void setObject(int p, Object v, int t) throws SQLException {
        this.setObject(p, v);
    }

    public void setObject(int p, Object v, int t, int s) throws SQLException {
        this.setObject(p, v);
    }

    public void setShort(int pos, short value) throws SQLException {
        this.setInt(pos, value);
    }

    public void setString(int pos, String value) throws SQLException {
        this.batch(pos, value);
    }

    public void setCharacterStream(int pos, Reader reader, int length) throws SQLException {
        try {
            int cnt;
            StringBuffer sb = new StringBuffer();
            char[] cbuf = new char[8192];
            while ((cnt = reader.read(cbuf)) > 0) {
                sb.append(cbuf, 0, cnt);
            }
            this.setString(pos, sb.toString());
        }
        catch (IOException e) {
            throw new SQLException("Cannot read from character stream, exception message: " + e.getMessage());
        }
    }

    public void setDate(int pos, Date x) throws SQLException {
        this.setObject(pos, x);
    }

    public void setDate(int pos, Date x, Calendar cal) throws SQLException {
        this.setObject(pos, x);
    }

    public void setTime(int pos, Time x) throws SQLException {
        this.setObject(pos, x);
    }

    public void setTime(int pos, Time x, Calendar cal) throws SQLException {
        this.setObject(pos, x);
    }

    public void setTimestamp(int pos, Timestamp x) throws SQLException {
        this.setObject(pos, x);
    }

    public void setTimestamp(int pos, Timestamp x, Calendar cal) throws SQLException {
        this.setObject(pos, x);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        this.checkOpen();
        return this.rs;
    }

    public boolean execute(String sql) throws SQLException {
        throw this.unused();
    }

    public int executeUpdate(String sql) throws SQLException {
        throw this.unused();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        throw this.unused();
    }

    public void addBatch(String sql) throws SQLException {
        throw this.unused();
    }

    private SQLException unused() {
        return new SQLException("not supported by PreparedStatment");
    }
}

