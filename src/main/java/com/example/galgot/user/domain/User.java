package com.example.galgot.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    private static final int NICKNAME_MAX = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", nullable = false, length = NICKNAME_MAX)
    private String nickname;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public static User create(String nickname, String email) {
        User user = new User();
        String name = (nickname == null || nickname.isBlank()) ? "여행자" : nickname.trim();
        user.nickname = name.length() > NICKNAME_MAX ? name.substring(0, NICKNAME_MAX) : name;
        user.email = email;
        user.createdAt = LocalDateTime.now();
        return user;
    }

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
