package com.iexchange.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应对象。
 */
@Schema(name = "LoginResponse", description = "登录响应")
@Data
public class LoginResponse {

    /**
     * 登录 Token。
     */
    @Schema(description = "JWT Token")
    private String token;

    /**
     * 用户 ID。
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 用户名。
     */
    @Schema(description = "用户名", example = "demo")
    private String username;

    /**
     * Token 过期时间（秒）。
     */
    @Schema(description = "Token 过期秒数", example = "7200")
    private Long expireSeconds;

    public static LoginResponse ok(String token, Long userId, String username, Long expireSeconds) {
        LoginResponse response = new LoginResponse();
        response.token = token;
        response.userId = userId;
        response.username = username;
        response.expireSeconds = expireSeconds;
        return response;
    }
}
