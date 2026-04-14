package com.example.bragbike.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("user")
    private UserInfo user;

    public String getToken() { return token; }
    public UserInfo getUser() { return user; }

    public static class UserInfo {
        @SerializedName("id")
        private int id;
        @SerializedName("full_name")
        private String fullName;
        @SerializedName("phone")
        private String phone;
        @SerializedName("role")
        private String role;
        @SerializedName("avatar_url")
        private String avatarUrl;

        public int getId() { return id; }
        public String getFullName() { return fullName; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
        public String getAvatarUrl() { return avatarUrl; }
    }
}