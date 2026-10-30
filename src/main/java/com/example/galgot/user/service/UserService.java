package com.example.galgot.user.service;

import com.example.galgot.user.domain.SocialAccount;
import com.example.galgot.user.domain.User;
import com.example.galgot.user.oauth.OAuthUserInfo;
import com.example.galgot.user.repository.SocialAccountRepository;
import com.example.galgot.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    /** 로그인 직후 세션에 담을 최소 정보 */
    public record LoginUser(Long id, String nickname) {
    }

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public UserService(UserRepository userRepository, SocialAccountRepository socialAccountRepository) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
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

    @Transactional(readOnly = true)
    public Optional<User> find(Long id) {
        return userRepository.findById(id);
    }
}
