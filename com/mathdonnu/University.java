/*
 * Decompiled with CFR 0_114.
 */
package com.mathdonnu;

public class University {
    Integer ID = 0;
    String UNIVERSITY_NAME = null;
    Integer UNIVERSITY_TYPE = null;
    Integer POST_CODE = 0;
    String ADDRESS = null;
    String PHONE = null;
    String WEB = null;
    String EMAIL = null;
    String URL = null;

    public University(Integer ID, String UNIVERSITY_NAME, Integer UNIVERSITY_TYPE, Integer POST_CODE, String ADDRESS, String PHONE, String WEB, String EMAIL, String URL2) {
        this.ID = ID;
        this.UNIVERSITY_NAME = UNIVERSITY_NAME;
        this.UNIVERSITY_TYPE = UNIVERSITY_TYPE;
        this.POST_CODE = POST_CODE;
        this.ADDRESS = ADDRESS;
        this.PHONE = PHONE;
        this.WEB = WEB;
        this.EMAIL = EMAIL;
        this.URL = URL2;
    }

    public String getSQL_VALUE() {
        return "VALUES (" + this.ID + ", '" + this.UNIVERSITY_NAME + "', " + this.UNIVERSITY_TYPE + ", " + this.POST_CODE + ", '" + this.ADDRESS + "', '" + this.PHONE + "', '" + this.WEB + "', '" + this.EMAIL + "', '" + this.URL + "');";
    }
}

