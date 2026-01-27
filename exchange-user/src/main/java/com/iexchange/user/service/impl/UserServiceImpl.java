package com.iexchange.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.user.entity.UserEntity;
import com.iexchange.user.mapper.UserMapper;
import com.iexchange.user.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserEntity findByUsername(String username) {
        // 使用 MyBatis-Plus 查询用户
        return userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getUsername, username));
    }

    @Override
    public UserEntity findById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public boolean save(UserEntity user) {
        return userMapper.insert(user) > 0;
    }
}
