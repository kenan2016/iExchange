package com.iexchange.user.service;

import com.iexchange.api.user.UserDTO;
import com.iexchange.api.user.UserQueryService;
import com.iexchange.user.entity.UserEntity;
import com.iexchange.user.service.UserService;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户查询 Dubbo 服务实现。
 */
@DubboService
public class UserQueryServiceImpl implements UserQueryService {

    private final UserService userService;

    public UserQueryServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDTO getById(Long userId) {
        // 通过数据库查询用户
        UserEntity user = userService.findById(userId);
        if (user == null) {
            return null;
        }
        return new UserDTO(user.getId(), user.getUsername());
    }
}
