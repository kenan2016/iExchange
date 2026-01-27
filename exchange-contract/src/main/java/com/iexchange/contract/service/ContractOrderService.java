package com.iexchange.contract.service;

import com.iexchange.contract.dto.ContractOrderCancelRequest;
import com.iexchange.contract.dto.ContractOrderRequest;
import com.iexchange.contract.dto.ContractOrderResponse;
import com.iexchange.contract.dto.ContractPositionResponse;
import com.iexchange.contract.entity.ContractOrderEntity;
import java.math.BigDecimal;

/**
 * 合约订单服务。
 */
public interface ContractOrderService {

    ContractOrderResponse submitOrder(ContractOrderRequest request);

    /**
     * 撤单。
     */
    ContractOrderResponse cancelOrder(ContractOrderCancelRequest request);

    ContractPositionResponse getPosition(Long userId, String symbol, String side, String marginMode);

    /**
     * 撮合挂单。
     */
    boolean matchPendingOrder(ContractOrderEntity order, BigDecimal markPrice);
}
