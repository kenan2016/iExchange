package com.iexchange.user.service.impl;

import com.iexchange.api.wallet.WalletAccountDTO;
import com.iexchange.api.wallet.WalletAccountService;
import com.iexchange.user.dto.DemoSeataRequest;
import com.iexchange.user.dto.DemoSeataResponse;
import com.iexchange.user.entity.DemoSeataOrderEntity;
import com.iexchange.user.mapper.DemoSeataOrderMapper;
import com.iexchange.user.service.DemoSeataService;
import io.seata.spring.annotation.GlobalTransactional;
import java.time.LocalDateTime;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * Seata 极简演示服务实现。
 */
@Service
public class DemoSeataServiceImpl implements DemoSeataService {

    private final DemoSeataOrderMapper orderMapper;

    @DubboReference
    private WalletAccountService walletAccountService;

    public DemoSeataServiceImpl(DemoSeataOrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    @GlobalTransactional(name = "demo-seata-tx", rollbackFor = Exception.class)
    public DemoSeataResponse execute(DemoSeataRequest request) {
        // 1. 落库一条本地演示订单
        DemoSeataOrderEntity order = new DemoSeataOrderEntity();
        order.setUserId(request.getUserId());
        order.setAsset(request.getAsset());
        order.setAmount(request.getAmount());
        order.setRemark(request.getRemark());
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        // 2. 调用钱包服务入账，参与全局事务
        WalletAccountDTO account = walletAccountService.tradeIn(
            request.getUserId(),
            request.getAsset(),
            request.getAmount(),
            "demo-seata-" + order.getId()
        );
        if (account == null) {
            throw new IllegalStateException("钱包入账失败");
        }

        // 3. ：可手动触发回滚
        if (Boolean.TRUE.equals(request.getForceFail())) {
            throw new IllegalArgumentException("演示回滚：强制失败");
        }

        DemoSeataResponse response = new DemoSeataResponse();
        response.setOrderId(order.getId());
        response.setAvailableBalance(account.getAvailableBalance());
        response.setTotalBalance(account.getTotalBalance());
        return response;
    }
}
