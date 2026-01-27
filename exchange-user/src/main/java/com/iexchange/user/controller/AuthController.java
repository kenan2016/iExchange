package com.iexchange.user.controller;

import com.iexchange.common.response.R;
import com.iexchange.user.dto.LoginRequest;
import com.iexchange.user.dto.LoginResponse;
import com.iexchange.user.dto.UserProfileResponse;
import com.iexchange.user.entity.UserEntity;
import com.iexchange.user.service.AuthService;
import com.iexchange.user.service.TokenService;
import com.iexchange.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录与用户信息接口。
 */
@Tag(name = "认证接口", description = "登录与用户信息")
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserService userService;

    public AuthController(AuthService authService, TokenService tokenService, UserService userService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    /**
     * 登录并返回 JWT。
     */
    @Operation(summary = "登录", description = "账号密码登录并返回 JWT")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 处理登录逻辑并返回 Token
        try {
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            return R.ok("登录成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询当前登录用户信息。
     */
    @Operation(summary = "查询当前用户", description = "通过 Token 查询用户信息")
    @GetMapping("/me")
    public R<UserProfileResponse> me(@Parameter(description = "登录 Token", required = true)
                                     @RequestHeader("X-Token") String token) {
        // 根据 JWT 解析用户信息
        TokenService.JwtUser jwtUser = tokenService.parseToken(token);
        if (jwtUser == null) {
            return R.fail("Token 无效或已过期");
        }
        UserEntity user = userService.findById(jwtUser.getUserId());
        if (user == null) {
            return R.fail("用户不存在");
        }
        UserProfileResponse response = UserProfileResponse.ok(user.getId(), user.getUsername());
        return R.ok("查询成功", response);
    }
}
