package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountInfo {
    private String code;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    private String gender;

    @JsonProperty("encrypted_password")
    private String encryptedPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
