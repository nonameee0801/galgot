package com.example.galgot.user.service;

import com.example.galgot.user.domain.User;
import com.example.galgot.user.repository.SocialAccountRepository;
import com.example.galgot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DB 없이 저장소와 비밀번호 암호화를 가짜(mock)로 바꿔 판단 흐름만 확인한다. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    SocialAccountRepository socialAccountRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, socialAccountRepository, passwordEncoder);
    }

    private static HttpStatus statusOf(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void 가입하면_이메일을_정리하고_비밀번호는_해시로_저장한다() {
        when(userRepository.existsByEmail("trip@galgot.com")).thenReturn(false);
        when(passwordEncoder.encode("abcd1234")).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService.LoginUser result = userService.signup("  Trip@Galgot.com ", "abcd1234", "길동");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertEquals("trip@galgot.com", saved.getValue().getEmail());
        assertEquals("HASHED", saved.getValue().getPasswordHash());
        assertEquals("길동", result.nickname());
    }

    @Test
    void 이미_가입된_이메일이면_409() {
        when(userRepository.existsByEmail("trip@galgot.com")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.signup("trip@galgot.com", "abcd1234", null));

        assertEquals(HttpStatus.CONFLICT, statusOf(e));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시에_같은_이메일로_가입해서_DB가_막아도_409() {
        when(userRepository.existsByEmail("trip@galgot.com")).thenReturn(false);
        when(passwordEncoder.encode("abcd1234")).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.signup("trip@galgot.com", "abcd1234", null));

        assertEquals(HttpStatus.CONFLICT, statusOf(e));
    }

    @Test
    void 비밀번호_규칙에_맞지_않으면_400이고_저장하지_않는다() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.signup("trip@galgot.com", "abcdefgh", null));

        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이메일과_비밀번호가_맞으면_로그인된다() {
        User user = User.createLocal("trip@galgot.com", "길동", "HASHED");
        when(userRepository.findByEmail("trip@galgot.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("abcd1234", "HASHED")).thenReturn(true);

        UserService.LoginUser result = userService.authenticate("TRIP@galgot.com", "abcd1234");

        assertEquals("길동", result.nickname());
    }

    @Test
    void 비밀번호가_틀리면_401() {
        User user = User.createLocal("trip@galgot.com", "길동", "HASHED");
        when(userRepository.findByEmail("trip@galgot.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong1234", "HASHED")).thenReturn(false);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.authenticate("trip@galgot.com", "wrong1234"));

        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(e));
        assertEquals(UserService.LOGIN_FAILED, e.getReason());
    }

    @Test
    void 없는_이메일도_같은_401과_같은_문구() {
        when(userRepository.findByEmail("none@galgot.com")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.authenticate("none@galgot.com", "abcd1234"));

        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(e));
        assertEquals(UserService.LOGIN_FAILED, e.getReason());
    }

    @Test
    void 카카오_전용_계정은_이메일로_로그인할_수_없다() {
        User kakaoOnly = User.create("카카오회원", "kakao@galgot.com");
        when(userRepository.findByEmail("kakao@galgot.com")).thenReturn(Optional.of(kakaoOnly));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.authenticate("kakao@galgot.com", "abcd1234"));

        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(e));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void 탈퇴하면_회원을_삭제한다() {
        when(userRepository.existsById(7L)).thenReturn(true);

        userService.withdraw(7L);

        verify(userRepository).deleteById(7L);
    }

    @Test
    void 없는_회원은_탈퇴할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> userService.withdraw(7L));

        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(e));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void 닉네임을_바꾸면_바뀐_닉네임을_돌려준다() {
        User user = User.create("여행자", null);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserService.LoginUser result = userService.changeNickname(7L, " 길동 ");

        assertEquals("길동", result.nickname());
        assertEquals("길동", user.getNickname());
    }

    @Test
    void 빈_닉네임으로_바꾸면_400() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(User.create("여행자", null)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.changeNickname(7L, ""));

        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
    }

    @Test
    void 없는_회원은_닉네임을_바꿀_수_없다() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.changeNickname(7L, "길동"));

        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(e));
    }
}
