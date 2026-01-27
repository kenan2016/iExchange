package com.iexchange.user.service;

import com.iexchange.user.dto.DemoSeataRequest;
import com.iexchange.user.dto.DemoSeataResponse;

/**
 * Seata 极简演示服务。
 */
public interface DemoSeataService {

    /**
     * 执行分布式事务演示。
     */
    DemoSeataResponse execute(DemoSeataRequest request);
}
