package com.iexchange.user.service;

import com.iexchange.user.dto.LoginResponse;
import com.iexchange.user.entity.UserEntity;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录认证服务。
 */
@Service
public class AuthService {

    private final UserService userService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService, TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 登录校验并返回 Token。
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录响应
     */
    public LoginResponse login(String username, String password) {
        UserEntity user = userService.findByUsername(username);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        // 校验密码（BCrypt）
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = tokenService.createToken(user.getId(), user.getUsername());
        return LoginResponse.ok(token, user.getId(), user.getUsername(), tokenService.getExpireSeconds());
    }
}
