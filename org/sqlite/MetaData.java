/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import org.sqlite.Conn;

class MetaData
implements DatabaseMetaData {
    private Conn conn;
    private PreparedStatement getTables = null;
    private PreparedStatement getTableTypes = null;
    private PreparedStatement getTypeInfo = null;
    private PreparedStatement getCatalogs = null;
    private PreparedStatement getSchemas = null;
    private PreparedStatement getUDTs = null;
    private PreparedStatement getColumnsTblName = null;
    private PreparedStatement getSuperTypes = null;
    private PreparedStatement getSuperTables = null;
    private PreparedStatement getTablePrivileges = null;
    private PreparedStatement getIndexInfo = null;
    private PreparedStatement getProcedures = null;
    private PreparedStatement getProcedureColumns = null;
    private PreparedStatement getAttributes = null;
    private PreparedStatement getBestRowIdentifier = null;
    private PreparedStatement getVersionColumns = null;
    private PreparedStatement getColumnPrivileges = null;
    private PreparedStatement getGeneratedKeys = null;

    MetaData(Conn conn) {
        this.conn = conn;
    }

    void checkOpen() throws SQLException {
        if (this.conn == null) {
            throw new SQLException("connection closed");
        }
    }

    synchronized void close() throws SQLException {
        if (this.conn == null) {
            return;
        }
        try {
            if (this.getTables != null) {
                this.getTables.close();
            }
            if (this.getTableTypes != null) {
                this.getTableTypes.close();
            }
            if (this.getTypeInfo != null) {
                this.getTypeInfo.close();
            }
            if (this.getCatalogs != null) {
                this.getCatalogs.close();
            }
            if (this.getSchemas != null) {
                this.getSchemas.close();
            }
            if (this.getUDTs != null) {
                this.getUDTs.close();
            }
            if (this.getColumnsTblName != null) {
                this.getColumnsTblName.close();
            }
            if (this.getSuperTypes != null) {
                this.getSuperTypes.close();
            }
            if (this.getSuperTables != null) {
                this.getSuperTables.close();
            }
            if (this.getTablePrivileges != null) {
                this.getTablePrivileges.close();
            }
            if (this.getIndexInfo != null) {
                this.getIndexInfo.close();
            }
            if (this.getProcedures != null) {
                this.getProcedures.close();
            }
            if (this.getProcedureColumns != null) {
                this.getProcedureColumns.close();
            }
            if (this.getAttributes != null) {
                this.getAttributes.close();
            }
            if (this.getBestRowIdentifier != null) {
                this.getBestRowIdentifier.close();
            }
            if (this.getVersionColumns != null) {
                this.getVersionColumns.close();
            }
            if (this.getColumnPrivileges != null) {
                this.getColumnPrivileges.close();
            }
            if (this.getGeneratedKeys != null) {
                this.getGeneratedKeys.close();
            }
            this.getTables = null;
            this.getTableTypes = null;
            this.getTypeInfo = null;
            this.getCatalogs = null;
            this.getSchemas = null;
            this.getUDTs = null;
            this.getColumnsTblName = null;
            this.getSuperTypes = null;
            this.getSuperTables = null;
            this.getTablePrivileges = null;
            this.getIndexInfo = null;
            this.getProcedures = null;
            this.getProcedureColumns = null;
            this.getAttributes = null;
            this.getBestRowIdentifier = null;
            this.getVersionColumns = null;
            this.getColumnPrivileges = null;
            this.getGeneratedKeys = null;
        }
        finally {
            this.conn = null;
        }
    }

    public Connection getConnection() {
        return this.conn;
    }

    public int getDatabaseMajorVersion() {
        return 3;
    }

    public int getDatabaseMinorVersion() {
        return 0;
    }

    public int getDriverMajorVersion() {
        return 1;
    }

    public int getDriverMinorVersion() {
        return 1;
    }

    public int getJDBCMajorVersion() {
        return 2;
    }

    public int getJDBCMinorVersion() {
        return 1;
    }

    public int getDefaultTransactionIsolation() {
        return 8;
    }

    public int getMaxBinaryLiteralLength() {
        return 0;
    }

    public int getMaxCatalogNameLength() {
        return 0;
    }

    public int getMaxCharLiteralLength() {
        return 0;
    }

    public int getMaxColumnNameLength() {
        return 0;
    }

    public int getMaxColumnsInGroupBy() {
        return 0;
    }

    public int getMaxColumnsInIndex() {
        return 0;
    }

    public int getMaxColumnsInOrderBy() {
        return 0;
    }

    public int getMaxColumnsInSelect() {
        return 0;
    }

    public int getMaxColumnsInTable() {
        return 0;
    }

    public int getMaxConnections() {
        return 0;
    }

    public int getMaxCursorNameLength() {
        return 0;
    }

    public int getMaxIndexLength() {
        return 0;
    }

    public int getMaxProcedureNameLength() {
        return 0;
    }

    public int getMaxRowSize() {
        return 0;
    }

    public int getMaxSchemaNameLength() {
        return 0;
    }

    public int getMaxStatementLength() {
        return 0;
    }

    public int getMaxStatements() {
        return 0;
    }

    public int getMaxTableNameLength() {
        return 0;
    }

    public int getMaxTablesInSelect() {
        return 0;
    }

    public int getMaxUserNameLength() {
        return 0;
    }

    public int getResultSetHoldability() {
        return 2;
    }

    public int getSQLStateType() {
        return 2;
    }

    public String getDatabaseProductName() {
        return "SQLite";
    }

    public String getDatabaseProductVersion() throws SQLException {
        return this.conn.libversion();
    }

    public String getDriverName() {
        return "SQLiteJDBC";
    }

    public String getDriverVersion() {
        return this.conn.getDriverVersion();
    }

    public String getExtraNameCharacters() {
        return "";
    }

    public String getCatalogSeparator() {
        return ".";
    }

    public String getCatalogTerm() {
        return "catalog";
    }

    public String getSchemaTerm() {
        return "schema";
    }

    public String getProcedureTerm() {
        return "not_implemented";
    }

    public String getSearchStringEscape() {
        return null;
    }

    public String getIdentifierQuoteString() {
        return " ";
    }

    public String getSQLKeywords() {
        return "";
    }

    public String getNumericFunctions() {
        return "";
    }

    public String getStringFunctions() {
        return "";
    }

    public String getSystemFunctions() {
        return "";
    }

    public String getTimeDateFunctions() {
        return "";
    }

    public String getURL() {
        return this.conn.url();
    }

    public String getUserName() {
        return null;
    }

    public boolean allProceduresAreCallable() {
        return false;
    }

    public boolean allTablesAreSelectable() {
        return true;
    }

    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    public boolean dataDefinitionIgnoredInTransactions() {
        return false;
    }

    public boolean doesMaxRowSizeIncludeBlobs() {
        return false;
    }

    public boolean deletesAreDetected(int type) {
        return false;
    }

    public boolean insertsAreDetected(int type) {
        return false;
    }

    public boolean isCatalogAtStart() {
        return true;
    }

    public boolean locatorsUpdateCopy() {
        return false;
    }

    public boolean nullPlusNonNullIsNull() {
        return true;
    }

    public boolean nullsAreSortedAtEnd() {
        return !this.nullsAreSortedAtStart();
    }

    public boolean nullsAreSortedAtStart() {
        return true;
    }

    public boolean nullsAreSortedHigh() {
        return true;
    }

    public boolean nullsAreSortedLow() {
        return !this.nullsAreSortedHigh();
    }

    public boolean othersDeletesAreVisible(int type) {
        return false;
    }

    public boolean othersInsertsAreVisible(int type) {
        return false;
    }

    public boolean othersUpdatesAreVisible(int type) {
        return false;
    }

    public boolean ownDeletesAreVisible(int type) {
        return false;
    }

    public boolean ownInsertsAreVisible(int type) {
        return false;
    }

    public boolean ownUpdatesAreVisible(int type) {
        return false;
    }

    public boolean storesLowerCaseIdentifiers() {
        return false;
    }

    public boolean storesLowerCaseQuotedIdentifiers() {
        return false;
    }

    public boolean storesMixedCaseIdentifiers() {
        return true;
    }

    public boolean storesMixedCaseQuotedIdentifiers() {
        return false;
    }

    public boolean storesUpperCaseIdentifiers() {
        return false;
    }

    public boolean storesUpperCaseQuotedIdentifiers() {
        return false;
    }

    public boolean supportsAlterTableWithAddColumn() {
        return false;
    }

    public boolean supportsAlterTableWithDropColumn() {
        return false;
    }

    public boolean supportsANSI92EntryLevelSQL() {
        return false;
    }

    public boolean supportsANSI92FullSQL() {
        return false;
    }

    public boolean supportsANSI92IntermediateSQL() {
        return false;
    }

    public boolean supportsBatchUpdates() {
        return true;
    }

    public boolean supportsCatalogsInDataManipulation() {
        return false;
    }

    public boolean supportsCatalogsInIndexDefinitions() {
        return false;
    }

    public boolean supportsCatalogsInPrivilegeDefinitions() {
        return false;
    }

    public boolean supportsCatalogsInProcedureCalls() {
        return false;
    }

    public boolean supportsCatalogsInTableDefinitions() {
        return false;
    }

    public boolean supportsColumnAliasing() {
        return true;
    }

    public boolean supportsConvert() {
        return false;
    }

    public boolean supportsConvert(int fromType, int toType) {
        return false;
    }

    public boolean supportsCorrelatedSubqueries() {
        return false;
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return true;
    }

    public boolean supportsDataManipulationTransactionsOnly() {
        return false;
    }

    public boolean supportsDifferentTableCorrelationNames() {
        return false;
    }

    public boolean supportsExpressionsInOrderBy() {
        return true;
    }

    public boolean supportsMinimumSQLGrammar() {
        return true;
    }

    public boolean supportsCoreSQLGrammar() {
        return true;
    }

    public boolean supportsExtendedSQLGrammar() {
        return false;
    }

    public boolean supportsLimitedOuterJoins() {
        return true;
    }

    public boolean supportsFullOuterJoins() {
        return false;
    }

    public boolean supportsGetGeneratedKeys() {
        return false;
    }

    public boolean supportsGroupBy() {
        return true;
    }

    public boolean supportsGroupByBeyondSelect() {
        return false;
    }

    public boolean supportsGroupByUnrelated() {
        return false;
    }

    public boolean supportsIntegrityEnhancementFacility() {
        return false;
    }

    public boolean supportsLikeEscapeClause() {
        return false;
    }

    public boolean supportsMixedCaseIdentifiers() {
        return true;
    }

    public boolean supportsMixedCaseQuotedIdentifiers() {
        return false;
    }

    public boolean supportsMultipleOpenResults() {
        return false;
    }

    public boolean supportsMultipleResultSets() {
        return false;
    }

    public boolean supportsMultipleTransactions() {
        return true;
    }

    public boolean supportsNamedParameters() {
        return true;
    }

    public boolean supportsNonNullableColumns() {
        return true;
    }

    public boolean supportsOpenCursorsAcrossCommit() {
        return false;
    }

    public boolean supportsOpenCursorsAcrossRollback() {
        return false;
    }

    public boolean supportsOpenStatementsAcrossCommit() {
        return false;
    }

    public boolean supportsOpenStatementsAcrossRollback() {
        return false;
    }

    public boolean supportsOrderByUnrelated() {
        return false;
    }

    public boolean supportsOuterJoins() {
        return true;
    }

    public boolean supportsPositionedDelete() {
        return false;
    }

    public boolean supportsPositionedUpdate() {
        return false;
    }

    public boolean supportsResultSetConcurrency(int t, int c) {
        return t == 1003 && c == 1007;
    }

    public boolean supportsResultSetHoldability(int h) {
        return h == 2;
    }

    public boolean supportsResultSetType(int t) {
        return t == 1003;
    }

    public boolean supportsSavepoints() {
        return false;
    }

    public boolean supportsSchemasInDataManipulation() {
        return false;
    }

    public boolean supportsSchemasInIndexDefinitions() {
        return false;
    }

    public boolean supportsSchemasInPrivilegeDefinitions() {
        return false;
    }

    public boolean supportsSchemasInProcedureCalls() {
        return false;
    }

    public boolean supportsSchemasInTableDefinitions() {
        return false;
    }

    public boolean supportsSelectForUpdate() {
        return false;
    }

    public boolean supportsStatementPooling() {
        return false;
    }

    public boolean supportsStoredProcedures() {
        return false;
    }

    public boolean supportsSubqueriesInComparisons() {
        return false;
    }

    public boolean supportsSubqueriesInExists() {
        return true;
    }

    public boolean supportsSubqueriesInIns() {
        return true;
    }

    public boolean supportsSubqueriesInQuantifieds() {
        return false;
    }

    public boolean supportsTableCorrelationNames() {
        return false;
    }

    public boolean supportsTransactionIsolationLevel(int level) {
        return level == 8;
    }

    public boolean supportsTransactions() {
        return true;
    }

    public boolean supportsUnion() {
        return true;
    }

    public boolean supportsUnionAll() {
        return true;
    }

    public boolean updatesAreDetected(int type) {
        return false;
    }

    public boolean usesLocalFilePerTable() {
        return false;
    }

    public boolean usesLocalFiles() {
        return true;
    }

    public boolean isReadOnly() throws SQLException {
        return this.conn.isReadOnly();
    }

    public ResultSet getAttributes(String c, String s, String t, String a) throws SQLException {
        if (this.getAttributes == null) {
            this.getAttributes = this.conn.prepareStatement("select null as TYPE_CAT, null as TYPE_SCHEM, null as TYPE_NAME, null as ATTR_NAME, null as DATA_TYPE, null as ATTR_TYPE_NAME, null as ATTR_SIZE, null as DECIMAL_DIGITS, null as NUM_PREC_RADIX, null as NULLABLE, null as REMARKS, null as ATTR_DEF, null as SQL_DATA_TYPE, null as SQL_DATETIME_SUB, null as CHAR_OCTET_LENGTH, null as ORDINAL_POSITION, null as IS_NULLABLE, null as SCOPE_CATALOG, null as SCOPE_SCHEMA, null as SCOPE_TABLE, null as SOURCE_DATA_TYPE limit 0;");
        }
        return this.getAttributes.executeQuery();
    }

    public ResultSet getBestRowIdentifier(String c, String s, String t, int scope, boolean n) throws SQLException {
        if (this.getBestRowIdentifier == null) {
            this.getBestRowIdentifier = this.conn.prepareStatement("select null as SCOPE, null as COLUMN_NAME, null as DATA_TYPE, null as TYPE_NAME, null as COLUMN_SIZE, null as BUFFER_LENGTH, null as DECIMAL_DIGITS, null as PSEUDO_COLUMN limit 0;");
        }
        return this.getBestRowIdentifier.executeQuery();
    }

    public ResultSet getColumnPrivileges(String c, String s, String t, String colPat) throws SQLException {
        if (this.getColumnPrivileges == null) {
            this.getColumnPrivileges = this.conn.prepareStatement("select null as TABLE_CAT, null as TABLE_SCHEM, null as TABLE_NAME, null as COLUMN_NAME, null as GRANTOR, null as GRANTEE, null as PRIVILEGE, null as IS_GRANTABLE limit 0;");
        }
        return this.getColumnPrivileges.executeQuery();
    }

    public ResultSet getColumns(String c, String s, String tbl, String colPat) throws SQLException {
        Statement stat = this.conn.createStatement();
        this.checkOpen();
        if (this.getColumnsTblName == null) {
            this.getColumnsTblName = this.conn.prepareStatement("select tbl_name from sqlite_master where tbl_name like ?;");
        }
        this.getColumnsTblName.setString(1, tbl);
        ResultSet rs = this.getColumnsTblName.executeQuery();
        if (!rs.next()) {
            return rs;
        }
        tbl = rs.getString(1);
        rs.close();
        String sql = "select null as TABLE_CAT, null as TABLE_SCHEM, '" + this.escape(tbl) + "' as TABLE_NAME, " + "cn as COLUMN_NAME, " + "ct as DATA_TYPE, " + "tn as TYPE_NAME, " + "2000000000 as COLUMN_SIZE, " + "2000000000 as BUFFER_LENGTH, " + "10   as DECIMAL_DIGITS, " + "10   as NUM_PREC_RADIX, " + "colnullable as NULLABLE, " + "null as REMARKS, " + "null as COLUMN_DEF, " + "0    as SQL_DATA_TYPE, " + "0    as SQL_DATETIME_SUB, " + "2000000000 as CHAR_OCTET_LENGTH, " + "ordpos as ORDINAL_POSITION, " + "(case colnullable when 0 then 'N' when 1 then 'Y' else '' end)" + "    as IS_NULLABLE, " + "null as SCOPE_CATLOG, " + "null as SCOPE_SCHEMA, " + "null as SCOPE_TABLE, " + "null as SOURCE_DATA_TYPE from (";
        rs = stat.executeQuery("pragma table_info ('" + this.escape(tbl) + "');");
        boolean colFound = false;
        int i = 0;
        while (rs.next()) {
            String colName = rs.getString(2);
            String colType = rs.getString(3);
            String colNotNull = rs.getString(4);
            int colNullable = 2;
            if (colNotNull != null) {
                int n = colNullable = colNotNull.equals("0") ? 1 : 0;
            }
            if (colFound) {
                sql = sql + " union all ";
            }
            colFound = true;
            colType = colType == null ? "TEXT" : colType.toUpperCase();
            int colJavaType = -1;
            colJavaType = colType.equals("INT") || colType.equals("INTEGER") ? 4 : (colType.equals("TEXT") ? 12 : (colType.equals("FLOAT") ? 6 : 12));
            sql = sql + "select " + i + " as ordpos, " + colNullable + " as colnullable, '" + colJavaType + "' as ct, '" + this.escape(colName) + "' as cn, '" + this.escape(colType) + "' as tn";
            if (colPat != null) {
                sql = sql + " where upper(cn) like upper('" + this.escape(colPat) + "')";
            }
            ++i;
        }
        sql = sql + (colFound ? ");" : "select null as ordpos, null as colnullable, null as cn, null as tn) limit 0;");
        rs.close();
        return stat.executeQuery(sql);
    }

    public ResultSet getCrossReference(String pc, String ps, String pt, String fc, String fs, String ft) throws SQLException {
        if (pt == null) {
            return this.getExportedKeys(fc, fs, ft);
        }
        if (ft == null) {
            return this.getImportedKeys(pc, ps, pt);
        }
        StringBuilder query = new StringBuilder();
        query.append(String.format("select %s as PKTABLE_CAT, %s as PKTABLE_SCHEM, %s as PKTABLE_NAME, ", MetaData.quote(pc), MetaData.quote(ps), MetaData.quote(pt)) + "'' as PKCOLUMN_NAME, " + String.format("%s as FKTABLE_CAT, %s as FKTABLE_SCHEM,  %s as FKTABLE_NAME, ", MetaData.quote(fc), MetaData.quote(fs), MetaData.quote(ft)) + "'' as FKCOLUMN_NAME, -1 as KEY_SEQ, 3 as UPDATE_RULE, " + "3 as DELETE_RULE, '' as FK_NAME, '' as PK_NAME, " + Integer.toString(5) + " as DEFERRABILITY limit 0;");
        return this.conn.createStatement().executeQuery(query.toString());
    }

    public ResultSet getSchemas() throws SQLException {
        if (this.getSchemas == null) {
            this.getSchemas = this.conn.prepareStatement("select null as TABLE_SCHEM, null as TABLE_CATALOG limit 0;");
        }
        this.getSchemas.clearParameters();
        return this.getSchemas.executeQuery();
    }

    public ResultSet getCatalogs() throws SQLException {
        if (this.getCatalogs == null) {
            this.getCatalogs = this.conn.prepareStatement("select null as TABLE_CAT limit 0;");
        }
        this.getCatalogs.clearParameters();
        return this.getCatalogs.executeQuery();
    }

    public ResultSet getPrimaryKeys(String c, String s, String table) throws SQLException {
        Statement stat = this.conn.createStatement();
        ResultSet rs = stat.executeQuery("pragma table_info('" + this.escape(table) + "');");
        String sql = "select null as TABLE_CAT, null as TABLE_SCHEM, '" + this.escape(table) + "' as TABLE_NAME, " + "cn as COLUMN_NAME, " + "0 as KEY_SEQ, " + "null as PK_NAME from (";
        int i = 0;
        while (rs.next()) {
            String colName = rs.getString(2);
            if (!rs.getBoolean(6)) {
                --i;
            } else {
                if (i > 0) {
                    sql = sql + " union all ";
                }
                sql = sql + "select '" + this.escape(colName) + "' as cn";
            }
            ++i;
        }
        sql = sql + (i == 0 ? "select null as cn) limit 0;" : ");");
        rs.close();
        return stat.executeQuery(sql);
    }

    private static String quote(String tableName) {
        if (tableName == null) {
            return "null";
        }
        return String.format("'%s'", tableName);
    }

    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        StringBuilder exportedKeysQuery = new StringBuilder();
        exportedKeysQuery.append(String.format("select %s as PKTABLE_CAT, %s as PKTABLE_SCHEM, %s as PKTABLE_NAME, ", MetaData.quote(catalog), MetaData.quote(schema), MetaData.quote(table)) + String.format("pcn as PKCOLUMN_NAME, %s as FKTABLE_CAT, %s as FKTABLE_SCHEM, ", MetaData.quote(catalog), MetaData.quote(schema)) + "fkn as FKTABLE_NAME, fcn as FKCOLUMN_NAME, " + "ks as KEY_SEQ, " + "ur as UPDATE_RULE, " + "dr as DELETE_RULE, " + "'' as FK_NAME, " + "'' as PK_NAME, " + Integer.toString(5) + " as DEFERRABILITY from (");
        String tableListQuery = String.format("select name from sqlite_master where type = 'table'", new Object[0]);
        Statement stat = this.conn.createStatement();
        ResultSet rs = stat.executeQuery(tableListQuery);
        ArrayList<String> tableList = new ArrayList<String>();
        while (rs.next()) {
            tableList.add(rs.getString(1));
        }
        rs.close();
        int count = 0;
        for (String targetTable : tableList) {
            String foreignKeyQuery = String.format("pragma foreign_key_list('%s');", this.escape(targetTable));
            try {
                ResultSet fk = stat.executeQuery(foreignKeyQuery);
                while (fk.next()) {
                    int keySeq = fk.getInt(2) + 1;
                    String PKTabName = fk.getString(3);
                    String FKColName = fk.getString(4);
                    String PKColName = fk.getString(5);
                    String updateRule = fk.getString(6);
                    String deleteRule = fk.getString(7);
                    if (PKTabName == null || !PKTabName.equals(table)) continue;
                    if (count > 0) {
                        exportedKeysQuery.append(" union all ");
                    }
                    exportedKeysQuery.append("select " + Integer.toString(keySeq) + " as ks," + "'" + this.escape(targetTable) + "' as fkn," + "'" + this.escape(FKColName) + "' as fcn," + "'" + this.escape(PKColName) + "' as pcn," + String.format("case '%s' ", this.escape(updateRule)) + String.format("when 'NO ACTION' then %d ", 3) + String.format("when 'CASCADE' then %d ", 0) + String.format("when 'RESTRICT' then %d  ", 1) + String.format("when 'SET NULL' then %d  ", 2) + String.format("when 'SET DEFAULT' then %d  ", 4) + "end as ur," + String.format("case '%s' ", this.escape(deleteRule)) + String.format("when 'NO ACTION' then %d ", 3) + String.format("when 'CASCADE' then %d ", 0) + String.format("when 'RESTRICT' then %d  ", 1) + String.format("when 'SET NULL' then %d  ", 2) + String.format("when 'SET DEFAULT' then %d  ", 4) + "end as dr");
                    ++count;
                }
                exportedKeysQuery.append(");");
                fk.close();
            }
            catch (SQLException e) {}
        }
        String sql = count > 0 ? exportedKeysQuery.toString() : String.format("select %s as PKTABLE_CAT, %s as PKTABLE_SCHEM, %s as PKTABLE_NAME, ", MetaData.quote(catalog), MetaData.quote(schema), MetaData.quote(table)) + "'' as PKCOLUMN_NAME, " + String.format("%s as FKTABLE_CAT, %s as FKTABLE_SCHEM, ", MetaData.quote(catalog), MetaData.quote(schema)) + "'' as FKTABLE_NAME, " + "'' as FKCOLUMN_NAME, " + "-1 as KEY_SEQ, " + "3 as UPDATE_RULE, " + "3 as DELETE_RULE, " + "'' as FK_NAME, " + "'' as PK_NAME, " + "5 as DEFERRABILITY limit 0;";
        return stat.executeQuery(sql);
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        ResultSet rs = null;
        Statement stat = this.conn.createStatement();
        String sql = String.format("select %s as PKTABLE_CAT, %s as PKTABLE_SCHEM, ", MetaData.quote(catalog), MetaData.quote(schema)) + String.format("ptn as PKTABLE_NAME, pcn as PKCOLUMN_NAME, %s as FKTABLE_CAT, %s as FKTABLE_SCHEM, %s as FKTABLE_NAME, ", MetaData.quote(catalog), MetaData.quote(schema), MetaData.quote(table)) + "fcn as FKCOLUMN_NAME, " + "ks as KEY_SEQ, " + "ur as UPDATE_RULE, " + "dr as DELETE_RULE, " + "'' as FK_NAME, " + "'' as PK_NAME, " + Integer.toString(5) + " as DEFERRABILITY from (";
        try {
            rs = stat.executeQuery("pragma foreign_key_list('" + this.escape(table) + "');");
            int i = 0;
            while (rs.next()) {
                int keySeq = rs.getInt(2) + 1;
                String PKTabName = rs.getString(3);
                String FKColName = rs.getString(4);
                String PKColName = rs.getString(5);
                String updateRule = rs.getString(6);
                String deleteRule = rs.getString(7);
                if (i > 0) {
                    sql = sql + " union all ";
                }
                sql = sql + String.format("select %d as ks,", keySeq) + String.format("'%s' as ptn, '%s' as fcn, '%s' as pcn,", this.escape(PKTabName), this.escape(FKColName), this.escape(PKColName)) + String.format("case '%s' ", this.escape(updateRule)) + String.format("when 'NO ACTION' then %d ", 3) + String.format("when 'CASCADE' then %d ", 0) + String.format("when 'RESTRICT' then %d  ", 1) + String.format("when 'SET NULL' then %d  ", 2) + String.format("when 'SET DEFAULT' then %d  ", 4) + "end as ur," + String.format("case '%s' ", this.escape(deleteRule)) + String.format("when 'NO ACTION' then %d ", 3) + String.format("when 'CASCADE' then %d ", 0) + String.format("when 'RESTRICT' then %d  ", 1) + String.format("when 'SET NULL' then %d  ", 2) + String.format("when 'SET DEFAULT' then %d  ", 4) + "end as dr";
                ++i;
            }
            sql = sql + ");";
            rs.close();
        }
        catch (SQLException e) {
            sql = sql + "select -1 as ks, '' as ptn, '' as fcn, '' as pcn, 3 as ur, 3 as dr) limit 0;";
        }
        return stat.executeQuery(sql);
    }

    public ResultSet getIndexInfo(String c, String s, String t, boolean u, boolean approximate) throws SQLException {
        ResultSet rs = null;
        Statement stat = this.conn.createStatement();
        String sql = "select null as TABLE_CAT, null as TABLE_SCHEM, '" + this.escape(t) + "' as TABLE_NAME, " + "un as NON_UNIQUE, " + "null as INDEX_QUALIFIER, " + "n as INDEX_NAME, " + Integer.toString(3) + " as TYPE, " + "op as ORDINAL_POSITION, " + "cn as COLUMN_NAME, " + "null as ASC_OR_DESC, " + "0 as CARDINALITY, " + "0 as PAGES, " + "null as FILTER_CONDITION from (";
        try {
            ArrayList indexList = new ArrayList();
            rs = stat.executeQuery("pragma index_list('" + this.escape(t) + "');");
            while (rs.next()) {
                indexList.add(new ArrayList());
                ((ArrayList)indexList.get(indexList.size() - 1)).add(rs.getString(2));
                ((ArrayList)indexList.get(indexList.size() - 1)).add(rs.getInt(3));
            }
            rs.close();
            int i = 0;
            for (ArrayList currentIndex : indexList) {
                String indexName = currentIndex.get(0).toString();
                int unique = (Integer)currentIndex.get(1);
                rs = stat.executeQuery("pragma index_info('" + this.escape(indexName) + "');");
                while (rs.next()) {
                    int ordinalPosition = rs.getInt(1) + 1;
                    String colName = rs.getString(3);
                    if (i > 0) {
                        sql = sql + " union all ";
                    }
                    sql = sql + "select " + Integer.toString(1 - unique) + " as un," + "'" + this.escape(indexName) + "' as n," + Integer.toString(ordinalPosition) + " as op," + "'" + this.escape(colName) + "' as cn";
                    ++i;
                    ++i;
                }
                rs.close();
            }
            sql = sql + ");";
        }
        catch (SQLException e) {
            sql = sql + "select null as un, null as n, null as op, null as cn) limit 0;";
        }
        return stat.executeQuery(sql);
    }

    public ResultSet getProcedureColumns(String c, String s, String p, String colPat) throws SQLException {
        if (this.getProcedures == null) {
            this.getProcedureColumns = this.conn.prepareStatement("select null as PROCEDURE_CAT, null as PROCEDURE_SCHEM, null as PROCEDURE_NAME, null as COLUMN_NAME, null as COLUMN_TYPE, null as DATA_TYPE, null as TYPE_NAME, null as PRECISION, null as LENGTH, null as SCALE, null as RADIX, null as NULLABLE, null as REMARKS limit 0;");
        }
        return this.getProcedureColumns.executeQuery();
    }

    public ResultSet getProcedures(String c, String s, String p) throws SQLException {
        if (this.getProcedures == null) {
            this.getProcedures = this.conn.prepareStatement("select null as PROCEDURE_CAT, null as PROCEDURE_SCHEM, null as PROCEDURE_NAME, null as UNDEF1, null as UNDEF2, null as UNDEF3, null as REMARKS, null as PROCEDURE_TYPE limit 0;");
        }
        return this.getProcedures.executeQuery();
    }

    public ResultSet getSuperTables(String c, String s, String t) throws SQLException {
        if (this.getSuperTables == null) {
            this.getSuperTables = this.conn.prepareStatement("select null as TABLE_CAT, null as TABLE_SCHEM, null as TABLE_NAME, null as SUPERTABLE_NAME limit 0;");
        }
        return this.getSuperTables.executeQuery();
    }

    public ResultSet getSuperTypes(String c, String s, String t) throws SQLException {
        if (this.getSuperTypes == null) {
            this.getSuperTypes = this.conn.prepareStatement("select null as TYPE_CAT, null as TYPE_SCHEM, null as TYPE_NAME, null as SUPERTYPE_CAT, null as SUPERTYPE_SCHEM, null as SUPERTYPE_NAME limit 0;");
        }
        return this.getSuperTypes.executeQuery();
    }

    public ResultSet getTablePrivileges(String c, String s, String t) throws SQLException {
        if (this.getTablePrivileges == null) {
            this.getTablePrivileges = this.conn.prepareStatement("select null as TABLE_CAT, null as TABLE_SCHEM, null as TABLE_NAME, null as GRANTOR, null as GRANTEE, null as PRIVILEGE, null as IS_GRANTABLE limit 0;");
        }
        return this.getTablePrivileges.executeQuery();
    }

    public synchronized ResultSet getTables(String c, String s, String t, String[] types) throws SQLException {
        this.checkOpen();
        t = t == null || "".equals(t) ? "%" : t.toUpperCase();
        String sql = "select null as TABLE_CAT, null as TABLE_SCHEM, name as TABLE_NAME, upper(type) as TABLE_TYPE, null as REMARKS, null as TYPE_CAT, null as TYPE_SCHEM, null as TYPE_NAME, null as SELF_REFERENCING_COL_NAME, null as REF_GENERATION from (select name, type from sqlite_master union all       select name, type from sqlite_temp_master) where TABLE_NAME like '" + this.escape(t) + "'";
        if (types != null) {
            sql = sql + " and TABLE_TYPE in (";
            for (int i = 0; i < types.length; ++i) {
                if (i > 0) {
                    sql = sql + ", ";
                }
                sql = sql + "'" + types[i].toUpperCase() + "'";
            }
            sql = sql + ")";
        }
        sql = sql + ";";
        return this.conn.createStatement().executeQuery(sql);
    }

    public ResultSet getTableTypes() throws SQLException {
        this.checkOpen();
        if (this.getTableTypes == null) {
            this.getTableTypes = this.conn.prepareStatement("select 'TABLE' as TABLE_TYPE union select 'VIEW' as TABLE_TYPE;");
        }
        this.getTableTypes.clearParameters();
        return this.getTableTypes.executeQuery();
    }

    public ResultSet getTypeInfo() throws SQLException {
        if (this.getTypeInfo == null) {
            this.getTypeInfo = this.conn.prepareStatement("select tn as TYPE_NAME, dt as DATA_TYPE, 0 as PRECISION, null as LITERAL_PREFIX, null as LITERAL_SUFFIX, null as CREATE_PARAMS, 1 as NULLABLE, 1 as CASE_SENSITIVE, 3 as SEARCHABLE, 0 as UNSIGNED_ATTRIBUTE, 0 as FIXED_PREC_SCALE, 0 as AUTO_INCREMENT, null as LOCAL_TYPE_NAME, 0 as MINIMUM_SCALE, 0 as MAXIMUM_SCALE, 0 as SQL_DATA_TYPE, 0 as SQL_DATETIME_SUB, 10 as NUM_PREC_RADIX from (    select 'BLOB' as tn, 2004 as dt union    select 'NULL' as tn, 0 as dt union    select 'REAL' as tn, 7 as dt union    select 'TEXT' as tn, 12 as dt union    select 'INTEGER' as tn, 4 as dt) order by TYPE_NAME;");
        }
        this.getTypeInfo.clearParameters();
        return this.getTypeInfo.executeQuery();
    }

    public ResultSet getUDTs(String c, String s, String t, int[] types) throws SQLException {
        if (this.getUDTs == null) {
            this.getUDTs = this.conn.prepareStatement("select null as TYPE_CAT, null as TYPE_SCHEM, null as TYPE_NAME, null as CLASS_NAME, null as DATA_TYPE, null as REMARKS, null as BASE_TYPE limit 0;");
        }
        this.getUDTs.clearParameters();
        return this.getUDTs.executeQuery();
    }

    public ResultSet getVersionColumns(String c, String s, String t) throws SQLException {
        if (this.getVersionColumns == null) {
            this.getVersionColumns = this.conn.prepareStatement("select null as SCOPE, null as COLUMN_NAME, null as DATA_TYPE, null as TYPE_NAME, null as COLUMN_SIZE, null as BUFFER_LENGTH, null as DECIMAL_DIGITS, null as PSEUDO_COLUMN limit 0;");
        }
        return this.getVersionColumns.executeQuery();
    }

    ResultSet getGeneratedKeys() throws SQLException {
        if (this.getGeneratedKeys == null) {
            this.getGeneratedKeys = this.conn.prepareStatement("select last_insert_rowid();");
        }
        return this.getGeneratedKeys.executeQuery();
    }

    private String escape(String val) {
        int len = val.length();
        StringBuffer buf = new StringBuffer(len);
        for (int i = 0; i < len; ++i) {
            if (val.charAt(i) == '\'') {
                buf.append('\'');
            }
            buf.append(val.charAt(i));
        }
        return buf.toString();
    }

    public Struct createStruct(String t, Object[] attr) throws SQLException {
        throw new SQLException("Not yet implemented by SQLite JDBC driver");
    }

    public ResultSet getFunctionColumns(String a, String b, String c, String d) throws SQLException {
        throw new SQLException("Not yet implemented by SQLite JDBC driver");
    }
}

