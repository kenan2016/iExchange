package com.iexchange.wallet.controller;

import com.iexchange.api.user.UserDTO;
import com.iexchange.api.user.UserQueryService;
import com.iexchange.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dubbo 消费者示例。
 */
@Tag(name = "Dubbo 示例", description = "Dubbo 调用示例接口")
@RestController
@RequestMapping("/wallet/user")
public class UserRemoteController {

    @DubboReference
    private UserQueryService userQueryService;

    /**
     * 通过 Dubbo 查询用户信息。
     */
    @GetMapping("/{userId}")
    @Operation(summary = "查询用户", description = "通过 Dubbo 调用用户服务")
    public R<UserDTO> getUser(@Parameter(description = "用户ID", required = true)
                              @PathVariable("userId") Long userId) {
        // 通过 Dubbo 远程调用用户服务
        UserDTO user = userQueryService.getById(userId);
        if (user == null) {
            return R.fail("用户不存在");
        }
        return R.ok("查询成功", user);
    }
}
