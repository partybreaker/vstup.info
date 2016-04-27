/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.io.Reader;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import org.sqlite.Codes;
import org.sqlite.DB;
import org.sqlite.Stmt;
import org.sqlite.Unused;

final class RS
extends Unused
implements ResultSet,
ResultSetMetaData,
Codes {
    private final Stmt stmt;
    private final DB db;
    boolean open = false;
    int maxRows;
    String[] cols = null;
    String[] colsMeta = null;
    boolean[][] meta = null;
    private int limitRows;
    private int row = 0;
    private int lastCol;

    RS(Stmt stmt) {
        this.stmt = stmt;
        this.db = stmt.db;
    }

    boolean isOpen() {
        return this.open;
    }

    void checkOpen() throws SQLException {
        if (!this.open) {
            throw new SQLException("ResultSet closed");
        }
    }

    private int checkCol(int col) throws SQLException {
        if (this.colsMeta == null) {
            throw new IllegalStateException("SQLite JDBC: inconsistent internal state");
        }
        if (col < 1 || col > this.colsMeta.length) {
            throw new SQLException("column " + col + " out of bounds [1," + this.colsMeta.length + "]");
        }
        return --col;
    }

    private int markCol(int col) throws SQLException {
        this.checkOpen();
        this.checkCol(col);
        this.lastCol = col--;
        return col;
    }

    private void checkMeta() throws SQLException {
        this.checkCol(1);
        if (this.meta == null) {
            this.meta = this.db.column_metadata(this.stmt.pointer);
        }
    }

    public void close() throws SQLException {
        this.cols = null;
        this.colsMeta = null;
        this.meta = null;
        this.open = false;
        this.limitRows = 0;
        this.row = 0;
        this.lastCol = -1;
        if (this.stmt == null) {
            return;
        }
        if (this.stmt != null && this.stmt.pointer != 0) {
            this.db.reset(this.stmt.pointer);
        }
    }

    public int findColumn(String col) throws SQLException {
        this.checkOpen();
        int c = -1;
        for (int i = 0; i < this.cols.length; ++i) {
            if (!col.equalsIgnoreCase(this.cols[i]) && (!this.cols[i].toUpperCase().endsWith(col.toUpperCase()) || this.cols[i].charAt(this.cols[i].length() - col.length()) != '.')) continue;
            if (c == -1) {
                c = i;
                continue;
            }
            throw new SQLException("ambiguous column: '" + col + "'");
        }
        if (c == -1) {
            throw new SQLException("no such column: '" + col + "'");
        }
        return c + 1;
    }

    public boolean next() throws SQLException {
        if (!this.open) {
            return false;
        }
        this.lastCol = -1;
        if (this.row == 0) {
            ++this.row;
            return true;
        }
        if (this.maxRows != 0 && this.row > this.maxRows) {
            return false;
        }
        int statusCode = this.db.step(this.stmt.pointer);
        switch (statusCode) {
            case 101: {
                this.close();
                return false;
            }
            case 100: {
                ++this.row;
                return true;
            }
        }
        this.db.throwex(statusCode);
        return false;
    }

    public int getType() throws SQLException {
        return 1003;
    }

    public int getFetchSize() throws SQLException {
        return this.limitRows;
    }

    public void setFetchSize(int rows) throws SQLException {
        if (0 > rows || this.maxRows != 0 && rows > this.maxRows) {
            throw new SQLException("fetch size " + rows + " out of bounds " + this.maxRows);
        }
        this.limitRows = rows;
    }

    public int getFetchDirection() throws SQLException {
        this.checkOpen();
        return 1000;
    }

    public void setFetchDirection(int d) throws SQLException {
        this.checkOpen();
        if (d != 1000) {
            throw new SQLException("only FETCH_FORWARD direction supported");
        }
    }

    public boolean isAfterLast() throws SQLException {
        return !this.open;
    }

    public boolean isBeforeFirst() throws SQLException {
        return this.open && this.row == 0;
    }

    public boolean isFirst() throws SQLException {
        return this.row == 1;
    }

    public boolean isLast() throws SQLException {
        throw new SQLException("function not yet implemented for SQLite");
    }

    protected void finalize() throws SQLException {
        this.close();
    }

    public int getRow() throws SQLException {
        return this.row;
    }

    public boolean wasNull() throws SQLException {
        return this.db.column_type(this.stmt.pointer, this.markCol(this.lastCol)) == 5;
    }

    public boolean getBoolean(int col) throws SQLException {
        return this.getInt(col) != 0;
    }

    public boolean getBoolean(String col) throws SQLException {
        return this.getBoolean(this.findColumn(col));
    }

    public byte getByte(int col) throws SQLException {
        return (byte)this.getInt(col);
    }

    public byte getByte(String col) throws SQLException {
        return this.getByte(this.findColumn(col));
    }

    public byte[] getBytes(int col) throws SQLException {
        return this.db.column_blob(this.stmt.pointer, this.markCol(col));
    }

    public byte[] getBytes(String col) throws SQLException {
        return this.getBytes(this.findColumn(col));
    }

    public Reader getCharacterStream(int col) throws SQLException {
        return new StringReader(this.getString(col));
    }

    public Reader getCharacterStream(String col) throws SQLException {
        return this.getCharacterStream(this.findColumn(col));
    }

    public java.sql.Date getDate(int col) throws SQLException {
        if (this.db.column_type(this.stmt.pointer, this.markCol(col)) == 5) {
            return null;
        }
        return new java.sql.Date(this.db.column_long(this.stmt.pointer, this.markCol(col)));
    }

    public java.sql.Date getDate(int col, Calendar cal) throws SQLException {
        if (this.db.column_type(this.stmt.pointer, this.markCol(col)) == 5) {
            return null;
        }
        if (cal == null) {
            return this.getDate(col);
        }
        cal.setTimeInMillis(this.db.column_long(this.stmt.pointer, this.markCol(col)));
        return new java.sql.Date(cal.getTime().getTime());
    }

    public java.sql.Date getDate(String col) throws SQLException {
        return this.getDate(this.findColumn(col), Calendar.getInstance());
    }

    public java.sql.Date getDate(String col, Calendar cal) throws SQLException {
        return this.getDate(this.findColumn(col), cal);
    }

    public double getDouble(int col) throws SQLException {
        if (this.db.column_type(this.stmt.pointer, this.markCol(col)) == 5) {
            return 0.0;
        }
        return this.db.column_double(this.stmt.pointer, this.markCol(col));
    }

    public double getDouble(String col) throws SQLException {
        return this.getDouble(this.findColumn(col));
    }

    public float getFloat(int col) throws SQLException {
        if (this.db.column_type(this.stmt.pointer, this.markCol(col)) == 5) {
            return 0.0f;
        }
        return (float)this.db.column_double(this.stmt.pointer, this.markCol(col));
    }

    public float getFloat(String col) throws SQLException {
        return this.getFloat(this.findColumn(col));
    }

    public int getInt(int col) throws SQLException {
        return this.db.column_int(this.stmt.pointer, this.markCol(col));
    }

    public int getInt(String col) throws SQLException {
        return this.getInt(this.findColumn(col));
    }

    public long getLong(int col) throws SQLException {
        return this.db.column_long(this.stmt.pointer, this.markCol(col));
    }

    public long getLong(String col) throws SQLException {
        return this.getLong(this.findColumn(col));
    }

    public short getShort(int col) throws SQLException {
        return (short)this.getInt(col);
    }

    public short getShort(String col) throws SQLException {
        return this.getShort(this.findColumn(col));
    }

    public String getString(int col) throws SQLException {
        return this.db.column_text(this.stmt.pointer, this.markCol(col));
    }

    public String getString(String col) throws SQLException {
        return this.getString(this.findColumn(col));
    }

    public Time getTime(int col) throws SQLException {
        if (this.db.column_type(this.stmt.pointer, this.markCol(col)) == 5) {
            return null;
        }
        return new Time(this.db.column_long(this.stmt.pointer, this.markCol(col)));
    }

    public Time getTime(int col, Calendar cal) throws SQLException {
        if (cal == null) {
            return this.getTime(col);
        }
        if (this.db.column_type(this.stmt.pointer, this.markCol(col)) == 5) {
            return null;
        }
        cal.setTimeInMillis(this.db.column_long(this.stmt.pointer, this.markCol(col)));
        return new Time(cal.getTime().getTime());
    }

    public Time getTime(String col) throws SQLException {
        return this.getTime(this.findColumn(col));
    }

    public Time getTime(String col, Calendar cal) throws SQLException {
        return this.getTime(this.findColumn(col), cal);
    }

    public Timestamp getTimestamp(int col) throws SQLException {
        if (this.db.column_type(this.stmt.pointer, this.markCol(col)) == 5) {
            return null;
        }
        return new Timestamp(this.db.column_long(this.stmt.pointer, this.markCol(col)));
    }

    public Timestamp getTimestamp(int col, Calendar cal) throws SQLException {
        if (cal == null) {
            return this.getTimestamp(col);
        }
        if (this.db.column_type(this.stmt.pointer, this.markCol(col)) == 5) {
            return null;
        }
        cal.setTimeInMillis(this.db.column_long(this.stmt.pointer, this.markCol(col)));
        return new Timestamp(cal.getTime().getTime());
    }

    public Timestamp getTimestamp(String col) throws SQLException {
        return this.getTimestamp(this.findColumn(col));
    }

    public Timestamp getTimestamp(String c, Calendar ca) throws SQLException {
        return this.getTimestamp(this.findColumn(c), ca);
    }

    public Object getObject(int col) throws SQLException {
        switch (this.db.column_type(this.stmt.pointer, this.checkCol(col))) {
            case 1: {
                long val = this.getLong(col);
                if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                    return new Long(val);
                }
                return new Integer((int)val);
            }
            case 2: {
                return new Double(this.getDouble(col));
            }
            case 4: {
                return this.getBytes(col);
            }
            case 5: {
                return null;
            }
        }
        return this.getString(col);
    }

    public Object getObject(String col) throws SQLException {
        return this.getObject(this.findColumn(col));
    }

    public Statement getStatement() {
        return this.stmt;
    }

    public String getCursorName() throws SQLException {
        return null;
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return this;
    }

    public String getCatalogName(int col) throws SQLException {
        return this.db.column_table_name(this.stmt.pointer, this.checkCol(col));
    }

    public String getColumnClassName(int col) throws SQLException {
        this.checkCol(col);
        return "java.lang.Object";
    }

    public int getColumnCount() throws SQLException {
        this.checkCol(1);
        return this.colsMeta.length;
    }

    public int getColumnDisplaySize(int col) throws SQLException {
        return Integer.MAX_VALUE;
    }

    public String getColumnLabel(int col) throws SQLException {
        return this.getColumnName(col);
    }

    public String getColumnName(int col) throws SQLException {
        return this.db.column_name(this.stmt.pointer, this.checkCol(col));
    }

    public int getColumnType(int col) throws SQLException {
        switch (this.db.column_type(this.stmt.pointer, this.checkCol(col))) {
            case 1: {
                return 4;
            }
            case 2: {
                return 6;
            }
            case 4: {
                return 2004;
            }
            case 5: {
                return 0;
            }
        }
        return 12;
    }

    public String getColumnTypeName(int col) throws SQLException {
        switch (this.db.column_type(this.stmt.pointer, this.checkCol(col))) {
            case 1: {
                return "integer";
            }
            case 2: {
                return "float";
            }
            case 4: {
                return "blob";
            }
            case 5: {
                return "null";
            }
        }
        return "text";
    }

    public int getPrecision(int col) throws SQLException {
        return 0;
    }

    public int getScale(int col) throws SQLException {
        return 0;
    }

    public String getSchemaName(int col) throws SQLException {
        return "";
    }

    public String getTableName(int col) throws SQLException {
        return this.db.column_table_name(this.stmt.pointer, this.checkCol(col));
    }

    public int isNullable(int col) throws SQLException {
        this.checkMeta();
        return this.meta[this.checkCol(col)][1] ? 0 : 1;
    }

    public boolean isAutoIncrement(int col) throws SQLException {
        this.checkMeta();
        return this.meta[this.checkCol(col)][2];
    }

    public boolean isCaseSensitive(int col) throws SQLException {
        return true;
    }

    public boolean isCurrency(int col) throws SQLException {
        return false;
    }

    public boolean isDefinitelyWritable(int col) throws SQLException {
        return true;
    }

    public boolean isReadOnly(int col) throws SQLException {
        return false;
    }

    public boolean isSearchable(int col) throws SQLException {
        return true;
    }

    public boolean isSigned(int col) throws SQLException {
        return false;
    }

    public boolean isWritable(int col) throws SQLException {
        return true;
    }

    public int getConcurrency() throws SQLException {
        return 1007;
    }

    public boolean rowDeleted() throws SQLException {
        return false;
    }

    public boolean rowInserted() throws SQLException {
        return false;
    }

    public boolean rowUpdated() throws SQLException {
        return false;
    }
}

