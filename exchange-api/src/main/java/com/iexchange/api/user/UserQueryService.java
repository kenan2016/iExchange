package com.iexchange.api.user;

/**
 * 用户查询 Dubbo 接口。
 */
public interface UserQueryService {

    /**
     * 根据用户 ID 获取用户信息。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserDTO getById(Long userId);
}
