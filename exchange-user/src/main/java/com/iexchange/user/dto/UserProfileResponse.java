package com.iexchange.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户信息响应对象。
 */
@Schema(name = "UserProfileResponse", description = "用户信息响应")
@Data
public class UserProfileResponse {

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

    public static UserProfileResponse ok(Long userId, String username) {
        UserProfileResponse response = new UserProfileResponse();
        response.userId = userId;
        response.username = username;
        return response;
    }
}
