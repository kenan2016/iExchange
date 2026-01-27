package com.iexchange.wallet.otc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.wallet.entity.OtcOrderEntity;
import com.iexchange.wallet.mapper.OtcOrderMapper;
import com.iexchange.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OTC 订单服务（演示版）。
 */
@Slf4j
@Service
public class OtcOrderService {

    private final WalletService walletService;
    private final OtcOrderMapper orderMapper;

    public OtcOrderService(WalletService walletService, OtcOrderMapper orderMapper) {
        this.walletService = walletService;
        this.orderMapper = orderMapper;
    }

    /**
     * 创建订单并冻结卖方资产。
     */
    @Transactional
    public OtcOrderEntity createOrder(String orderNo, Long buyerId, Long sellerId, String asset, BigDecimal amount) {
        OtcOrderEntity existing = getByOrderNo(orderNo);
        if (existing != null) {
            return existing;
        }
        String requestId = "otc-freeze:" + orderNo;
        walletService.freeze(sellerId, asset, amount, requestId);
        OtcOrderEntity order = new OtcOrderEntity();
        order.setOrderNo(orderNo);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setAsset(asset);
        order.setAmount(amount);
        order.setStatus(OtcOrderStatus.WAIT_PAY.getCode());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    /**
     * 买方标记已付款（进入待放币状态）。
     */
    @Transactional
    public OtcOrderEntity markPaid(String orderNo) {
        OtcOrderEntity order = requireOrder(orderNo);
        if (!OtcOrderStatus.WAIT_PAY.getCode().equals(order.getStatus())) {
            return order;
        }
        order.setStatus(OtcOrderStatus.WAIT_RELEASE.getCode());
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /**
     * 卖方确认收款或平台仲裁后放币。
     */
    @Transactional
    public OtcOrderEntity release(String orderNo) {
        OtcOrderEntity order = requireOrder(orderNo);
        if (OtcOrderStatus.DONE.getCode().equals(order.getStatus())) {
            return order;
        }
        if (!OtcOrderStatus.WAIT_RELEASE.getCode().equals(order.getStatus())
            && !OtcOrderStatus.APPEAL.getCode().equals(order.getStatus())) {
            return order;
        }
        String requestId = "otc-release:" + orderNo;
        walletService.deductFrozen(order.getSellerId(), order.getAsset(), order.getAmount(), requestId);
        walletService.tradeIn(order.getBuyerId(), order.getAsset(), order.getAmount(), requestId);
        order.setStatus(OtcOrderStatus.DONE.getCode());
        order.setReleasedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /**
     * 未付款取消或仲裁退款（解冻卖方资产）。
     */
    @Transactional
    public OtcOrderEntity cancel(String orderNo) {
        OtcOrderEntity order = requireOrder(orderNo);
        if (OtcOrderStatus.DONE.getCode().equals(order.getStatus())
            || OtcOrderStatus.CANCELED.getCode().equals(order.getStatus())) {
            return order;
        }
        String requestId = "otc-cancel:" + orderNo;
        walletService.unfreeze(order.getSellerId(), order.getAsset(), order.getAmount(), requestId);
        order.setStatus(OtcOrderStatus.CANCELED.getCode());
        order.setCanceledAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    public OtcOrderEntity getOrder(String orderNo) {
        return getByOrderNo(orderNo);
    }

    public List<OtcOrderEntity> listByUser(Long userId) {
        return orderMapper.selectList(new LambdaQueryWrapper<OtcOrderEntity>()
            .eq(OtcOrderEntity::getBuyerId, userId)
            .or()
            .eq(OtcOrderEntity::getSellerId, userId));
    }

    /**
     * 自动取消超时未付款订单。
     */
    @Transactional
    public int autoCancelExpired(LocalDateTime deadline) {
        List<OtcOrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<OtcOrderEntity>()
            .eq(OtcOrderEntity::getStatus, OtcOrderStatus.WAIT_PAY.getCode())
            .lt(OtcOrderEntity::getCreatedAt, deadline));
        int handled = 0;
        for (OtcOrderEntity order : orders) {
            try {
                cancel(order.getOrderNo());
                handled++;
            } catch (Exception ex) {
                log.warn("自动取消失败 orderNo={}", order.getOrderNo(), ex);
            }
        }
        return handled;
    }

    /**
     * 自动转入申诉的超时未放币订单。
     */
    @Transactional
    public int autoAppealExpired(LocalDateTime deadline) {
        List<OtcOrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<OtcOrderEntity>()
            .eq(OtcOrderEntity::getStatus, OtcOrderStatus.WAIT_RELEASE.getCode())
            .lt(OtcOrderEntity::getPaidAt, deadline));
        int handled = 0;
        for (OtcOrderEntity order : orders) {
            order.setStatus(OtcOrderStatus.APPEAL.getCode());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            handled++;
        }
        return handled;
    }

    private OtcOrderEntity getByOrderNo(String orderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<OtcOrderEntity>()
            .eq(OtcOrderEntity::getOrderNo, orderNo));
    }

    private OtcOrderEntity requireOrder(String orderNo) {
        OtcOrderEntity order = getByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("OTC 订单不存在");
        }
        return order;
    }
}
