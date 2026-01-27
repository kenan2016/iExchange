package com.iexchange.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求对象。
 */
@Schema(name = "LoginRequest", description = "登录请求")
@Data
public class LoginRequest {

    /**
     * 用户名。
     */
    @Schema(description = "用户名", example = "demo")
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码（明文传输仅用于示例）。
     */
    @Schema(description = "密码（示例明文）", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
