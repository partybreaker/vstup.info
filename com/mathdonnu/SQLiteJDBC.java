/*
 * Decompiled with CFR 0_114.
 */
package com.mathdonnu;

import com.mathdonnu.Entry;
import com.mathdonnu.University;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SQLiteJDBC {
    private String DBname = "Default";
    private Connection connection = null;
    private Statement statement = null;

    public static void main(String[] args) {
    }

    public void OpenDatabase(String DBname) {
        this.DBname = DBname;
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + DBname + ".db");
            this.statement = this.connection.createStatement();
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        System.out.println("Database " + DBname + ".db has been opened (created).");
    }

    public void CloseDatabase() {
        try {
            this.statement.close();
            this.connection.close();
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        System.out.println("Database " + this.DBname + " has been closed.");
    }

    public void CreateSpecialityTableInCurrentDB(String TableName) {
        String sql = null;
        sql = "CREATE TABLE IF NOT EXISTS " + TableName + " (ID               INT PRIMARY KEY    NOT NULL," + " FULLNAME          TEXT               NOT NULL, " + " PRIORITY          INT     NOT NULL, " + " RATE_SUMM         REAL    NOT NULL, " + " SCHOOL_RATE       REAL    NOT NULL," + " ZNO               TEXT    NOT NULL," + " UNIVERSITY_EXAM   TEXT    NOT NULL," + " ADDITIONAL_SUMM   TEXT    NOT NULL," + " RIGHT_TO_EXTRAORDINARY_ADMISSION TEXT NOT NULL," + " RIGHT_TO_NON_COMPETITIVE_ADMISSION TEXT NOT NULL," + " TARGET_DIRECTION TEXT NOT NULL)";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.out.println("Error when creating table " + TableName);
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        System.out.println("Table " + TableName + " in " + this.DBname + " has been created by pattern.");
    }

    public void CreateUniversityTableInCurrentDB() {
        String sql = null;
        sql = "CREATE TABLE IF NOT EXISTS UNIVERSITIES (ID                    INT PRIMARY KEY     NOT NULL, UNIVERSITY_NAME       TEXT                NOT NULL,  UNIVERSITY_TYPE       INT                 NOT NULL, POST_CODE             INT                 NOT NULL, ADDRESS               TEXT                NOT NULL, PHONE                 TEXT                NOT NULL, WEB                   TEXT                NOT NULL, EMAIL                 TEXT                NOT NULL, URL                   TEXT                NOT NULL)";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        System.out.println("Universities table has been created.");
    }

    public void InsertEntryToTable(String TableName, Entry entry) {
        String sql = null;
        sql = "INSERT INTO " + TableName + " (ID,FULLNAME,PRIORITY,RATE_SUMM,SCHOOL_RATE,ZNO," + "UNIVERSITY_EXAM,ADDITIONAL_SUMM,RIGHT_TO_EXTRAORDINARY_ADMISSION" + ",RIGHT_TO_NON_COMPETITIVE_ADMISSION,TARGET_DIRECTION) " + entry.getSQL_VALUE();
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.out.println("Error when adding Entry " + entry.toString() + " to table " + TableName);
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
    }

    public void InsertUniversityToTable(University university) {
        String sql = null;
        sql = "INSERT INTO UNIVERSITIES (ID,UNIVERSITY_NAME,UNIVERSITY_TYPE,POST_CODE,ADDRESS,PHONE,WEB,EMAIL,URL) " + university.getSQL_VALUE();
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.out.println("Error add university to table.");
            System.exit(0);
        }
        System.out.println(university.ID + " University #" + university.UNIVERSITY_NAME + "# has been added to table.");
    }

    public void CreateMappingTableForUniType() {
        String sql = null;
        sql = "CREATE TABLE IF NOT EXISTS UNI_TYPE_MAPPING (ID                    INT PRIMARY KEY     NOT NULL, TYPE_NAME             TEXT                NOT NULL)";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        sql = "INSERT INTO UNI_TYPE_MAPPING (ID,TYPE_NAME) VALUES (1, '\u0423\u043d\u0456\u0432\u0435\u0440\u0441\u0438\u0442\u0435\u0442');";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        sql = "INSERT INTO UNI_TYPE_MAPPING (ID,TYPE_NAME) VALUES (2, '\u0406\u043d\u0441\u0442\u0438\u0442\u0443\u0442');";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        sql = "INSERT INTO UNI_TYPE_MAPPING (ID,TYPE_NAME) VALUES (3, '\u0410\u043a\u0430\u0434\u0435\u043c\u0456\u044f');";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        sql = "INSERT INTO UNI_TYPE_MAPPING (ID,TYPE_NAME) VALUES (4, '\u041a\u043e\u043d\u0441\u0435\u0440\u0432\u0430\u0442\u043e\u0440\u0456\u044f (\u043c\u0443\u0437\u0438\u0447\u043d\u0430 \u0430\u043a\u0430\u0434\u0435\u043c\u0456\u044f)');";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        sql = "INSERT INTO UNI_TYPE_MAPPING (ID,TYPE_NAME) VALUES (5, '\u041a\u043e\u043b\u0435\u0434\u0436');";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
        sql = "INSERT INTO UNI_TYPE_MAPPING (ID,TYPE_NAME) VALUES (6, '\u0422\u0435\u0445\u043d\u0456\u043a\u0443\u043c (\u0443\u0447\u0438\u043b\u0438\u0449\u0435)');";
        try {
            this.statement.executeUpdate(sql);
        }
        catch (Exception exception) {
            System.err.println(exception.getClass().getName() + ": " + exception.getMessage());
            System.exit(0);
        }
    }
}

