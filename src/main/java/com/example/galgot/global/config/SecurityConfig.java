package com.example.galgot.global.config;

import com.example.galgot.user.oauth.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 접근 규칙.
 * - 장소 조회, 공유 코스 조회, 날씨, 추천(/api/places, /api/courses/shared, /api/weather, /api/recommendations)과
 *   이메일 가입·로그인(/api/auth/signup, /api/auth/login)은 로그인 없이 허용
 * - 그 외는 로그인 필요 (카카오 로그인 또는 이메일 로그인)
 * - /api/** 에 로그인 없이 접근하면 로그인 화면으로 보내지 않고 401을 돌려준다
 * - 프론트(React) 주소에서 오는 요청만 CORS 허용 (쿠키 포함)
 * - 로그인 성공/실패 후에는 프론트 주소로 돌려보낸다
 */
@Configuration
public class SecurityConfig {

    /** 프론트 주소. 배포할 때는 환경 변수 FRONTEND_URL로 바꾼다. 끝에 슬래시(/)를 붙이지 않는다. */
    private final String frontendUrl;

    public SecurityConfig(@Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomOAuth2UserService customOAuth2UserService,
                                           SecurityContextRepository securityContextRepository) throws Exception {
        RequestMatcher apiRequests = request -> request.getRequestURI().startsWith("/api/");

        http
                .cors(Customizer.withDefaults())
                // 이메일 로그인(AuthController)이 로그인 상태를 저장하는 곳과 같은 저장소를 쓴다
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                // 세션 쿠키 + CSRF 비활성화(/api/**) 조합은 쿠키의 SameSite=Lax 설정(application.yml)과
                // 아래 CORS(허용 출처 1곳)에 기대고 있다. 프론트와 백엔드를 서로 다른 도메인에 배포할 때 다시 점검할 것.
                .csrf(csrf -> csrf.ignoringRequestMatchers(apiRequests))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/places/**", "/api/courses/shared/**",
                                "/api/weather/**", "/api/recommendations/**",
                                "/api/auth/signup", "/api/auth/login", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiRequests))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .defaultSuccessUrl(frontendUrl + "/", true)
                        .failureUrl(frontendUrl + "/?login=failed"))
                // POST /api/auth/logout : 세션을 끊고 204를 돌려준다
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));
        return http.build();
    }

    /** 로그인 상태(SecurityContext)를 HTTP 세션에 저장한다. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** 프론트 주소에서 /api/** 로 오는 요청만 허용. 쿠키(세션)를 같이 보내도록 allowCredentials를 켠다. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
