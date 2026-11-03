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

    public static final int NICKNAME_MAX = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", nullable = false, length = NICKNAME_MAX)
    private String nickname;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    /** BCrypt 해시. 카카오 전용 회원은 null이라 이메일 로그인을 할 수 없다. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    /** 소셜 로그인으로 처음 들어온 회원. */
    public static User create(String nickname, String email) {
        User user = new User();
        String name = (nickname == null || nickname.isBlank()) ? "여행자" : nickname.trim();
        user.nickname = name.length() > NICKNAME_MAX ? name.substring(0, NICKNAME_MAX) : name;
        user.email = email;
        user.createdAt = LocalDateTime.now();
        return user;
    }

    /** 이메일로 가입한 회원. email은 CredentialPolicy로 정리된 값, passwordHash는 이미 해시한 값을 받는다. */
    public static User createLocal(String email, String nickname, String passwordHash) {
        User user = create(nickname, email);
        user.passwordHash = passwordHash;
        return user;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    /**
     * 닉네임 바꾸기(기획서 18장 F). 앞뒤 공백을 지우고 1~50자만 받는다.
     * 가입 때와 달리 비우면 "여행자"로 바꾸지 않고 거부한다. 다른 회원과 같은 닉네임은 허용한다.
     */
    public void changeNickname(String newNickname) {
        String name = newNickname == null ? "" : newNickname.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }
        if (name.length() > NICKNAME_MAX) {
            throw new IllegalArgumentException("닉네임은 " + NICKNAME_MAX + "자까지 쓸 수 있어요.");
        }
        this.nickname = name;
    }

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
