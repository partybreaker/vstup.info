/*
 * Decompiled with CFR 0_114.
 */
package com.mathdonnu;

public class Entry {
    Integer ID = 0;
    String FULLNAME = null;
    Integer PRIORITY = 0;
    float RATE_SUMM = 0.0f;
    float SCHOOL_RATE = 0.0f;
    String ZNO = null;
    String UNIVERSITY_EXAM = null;
    String ADDITIONAL_SUMM = null;
    String RIGHT_TO_EXTRAORDINARY_ADMISSION = null;
    String RIGHT_TO_NON_COMPETITIVE_ADMISSION = null;
    String TARGET_DIRECTION = null;

    public Entry(Integer ID, String FULLNAME, Integer PRIORITY, float RATE_SUMM, float SCHOOL_RATE, String ZNO, String UNIVERSITY_EXAM, String ADDITIONAL_SUMM, String RIGHT_TO_EXTRAORDINARY_ADMISSION, String RIGHT_TO_NON_COMPETITIVE_ADMISSION, String TARGET_DIRECTION) {
        this.ID = ID;
        this.FULLNAME = FULLNAME;
        this.PRIORITY = PRIORITY;
        this.RATE_SUMM = RATE_SUMM;
        this.SCHOOL_RATE = SCHOOL_RATE;
        this.ZNO = ZNO;
        this.UNIVERSITY_EXAM = UNIVERSITY_EXAM;
        this.ADDITIONAL_SUMM = ADDITIONAL_SUMM;
        this.RIGHT_TO_EXTRAORDINARY_ADMISSION = RIGHT_TO_EXTRAORDINARY_ADMISSION;
        this.RIGHT_TO_NON_COMPETITIVE_ADMISSION = RIGHT_TO_NON_COMPETITIVE_ADMISSION;
        this.TARGET_DIRECTION = TARGET_DIRECTION;
    }

    public String getSQL_VALUE() {
        return "VALUES (" + this.ID + ", '" + this.FULLNAME + "', " + this.PRIORITY + ", " + this.RATE_SUMM + ", " + this.SCHOOL_RATE + ", '" + this.ZNO + "', '" + this.UNIVERSITY_EXAM + "', '" + this.ADDITIONAL_SUMM + "', '" + this.RIGHT_TO_EXTRAORDINARY_ADMISSION + "', '" + this.RIGHT_TO_NON_COMPETITIVE_ADMISSION + "', '" + this.TARGET_DIRECTION + "');";
    }
}

