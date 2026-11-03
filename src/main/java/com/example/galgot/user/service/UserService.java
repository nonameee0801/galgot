package com.example.galgot.user.service;

import com.example.galgot.user.domain.CredentialPolicy;
import com.example.galgot.user.domain.SocialAccount;
import com.example.galgot.user.domain.User;
import com.example.galgot.user.oauth.OAuthUserInfo;
import com.example.galgot.user.repository.SocialAccountRepository;
import com.example.galgot.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    /** 로그인 직후 세션에 담을 최소 정보 */
    public record LoginUser(Long id, String nickname) {
    }

    static final String LOGIN_FAILED = "이메일 또는 비밀번호가 맞지 않습니다.";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       SocialAccountRepository socialAccountRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 처음 로그인하면 회원과 소셜 계정을 만들고, 이미 있으면 기존 회원을 돌려준다. */
    @Transactional
    public LoginUser loginOrRegister(OAuthUserInfo info) {
        SocialAccount account = socialAccountRepository
                .findByProviderAndProviderUserId(info.provider(), info.providerUserId())
                .orElseGet(() -> {
                    User user = userRepository.save(User.create(info.nickname(), info.email()));
                    return socialAccountRepository.save(
                            new SocialAccount(user, info.provider(), info.providerUserId()));
                });

        User user = account.getUser();
        return new LoginUser(user.getId(), user.getNickname());
    }

    /**
     * 이메일 회원가입. 규칙에 맞지 않으면 400, 이미 있는 이메일이면 409.
     * 동시에 같은 이메일로 가입하면 DB 유니크 키가 막고, 그 경우도 409로 바꿔 준다.
     */
    @Transactional
    public LoginUser signup(String rawEmail, String rawPassword, String nickname) {
        String email = CredentialPolicy.normalizeEmail(rawEmail);
        if (!CredentialPolicy.isValidEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
        if (!CredentialPolicy.isValidPassword(rawPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "비밀번호는 영문과 숫자를 포함해 8~64자로 입력해 주세요.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }

        User user;
        try {
            user = userRepository.saveAndFlush(
                    User.createLocal(email, nickname, passwordEncoder.encode(rawPassword)));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        return new LoginUser(user.getId(), user.getNickname());
    }

    /**
     * 이메일 로그인. 이메일이 없거나, 비밀번호가 틀리거나, 카카오 전용 계정이면
     * 모두 같은 401과 같은 문구를 돌려준다(어느 이메일이 가입돼 있는지 알 수 없게).
     */
    @Transactional(readOnly = true)
    public LoginUser authenticate(String rawEmail, String rawPassword) {
        String email = CredentialPolicy.normalizeEmail(rawEmail);
        Optional<User> found = (email == null || email.isEmpty())
                ? Optional.empty()
                : userRepository.findByEmail(email);

        User user = found.filter(User::hasPassword)
                .filter(u -> rawPassword != null && passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, LOGIN_FAILED));
        return new LoginUser(user.getId(), user.getNickname());
    }

    /** 회원 탈퇴. 소셜 계정, 코스, 코스 장소는 DB 외래 키(ON DELETE CASCADE)로 함께 지워진다. */
    @Transactional
    public void withdraw(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다.");
        }
        userRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<User> find(Long id) {
        return userRepository.findById(id);
    }

    /** 닉네임 바꾸기. 회원이 없으면 401, 규칙에 맞지 않으면 400. */
    @Transactional
    public LoginUser changeNickname(Long userId, String nickname) {
        User user = (userId == null ? Optional.<User>empty() : userRepository.findById(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다."));
        try {
            user.changeNickname(nickname);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return new LoginUser(user.getId(), user.getNickname());
    }
}
