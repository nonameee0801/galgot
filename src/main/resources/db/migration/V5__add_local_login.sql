-- 갈곳 V5: 이메일 회원가입·로그인
-- 비밀번호는 BCrypt 해시(60자)로 저장한다. 카카오 전용 회원은 NULL.
-- 이메일은 소문자로 정리해 저장하고 중복을 막는다(카카오 회원은 이메일이 NULL이라 충돌하지 않음).
ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER email,
    ADD UNIQUE KEY uk_users_email (email);
