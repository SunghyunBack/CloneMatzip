package com.example.studymatzip.entities;

import java.util.Date;

public class RecoverContactCodeEntity {
    private int index;
    private String contact;
    private String code;
    private String salt;

    public int getIndex() {
        return index;
    }

    public RecoverContactCodeEntity setIndex(int index) {
        this.index = index;
        return this;
    }

    public String getContact() {
        return contact;
    }

    public RecoverContactCodeEntity setContact(String contact) {
        this.contact = contact;
        return this;
    }

    public String getCode() {
        return code;
    }

    public RecoverContactCodeEntity setCode(String code) {
        this.code = code;
        return this;
    }

    public String getSalt() {
        return salt;
    }

    public RecoverContactCodeEntity setSalt(String salt) {
        this.salt = salt;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public RecoverContactCodeEntity setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public RecoverContactCodeEntity setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public RecoverContactCodeEntity setExpired(boolean expired) {
        isExpired = expired;
        return this;
    }

    private Date createdAt;
    private Date expiresAt;
    private boolean isExpired;

}
