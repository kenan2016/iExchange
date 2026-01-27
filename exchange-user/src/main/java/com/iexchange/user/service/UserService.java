package com.iexchange.user.service;

import com.iexchange.user.entity.UserEntity;

/**
 * 用户服务接口。
 */
public interface UserService {

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 用户信息
     */
    UserEntity findByUsername(String username);

    /**
     * 根据用户 ID 查询用户。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserEntity findById(Long userId);

    /**
     * 保存用户。
     *
     * @param user 用户信息
     * @return 是否成功
     */
    boolean save(UserEntity user);
}
