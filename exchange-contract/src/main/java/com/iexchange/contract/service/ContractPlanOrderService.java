package com.iexchange.contract.service;

import com.iexchange.contract.dto.ContractPlanOrderCancelRequest;
import com.iexchange.contract.dto.ContractPlanOrderListResponse;
import com.iexchange.contract.dto.ContractPlanOrderRequest;
import com.iexchange.contract.dto.ContractPlanOrderResponse;

/**
 * 合约计划委托服务。
 */
public interface ContractPlanOrderService {

    /**
     * 下计划单。
     */
    ContractPlanOrderResponse placePlanOrder(ContractPlanOrderRequest request);

    /**
     * 撤销计划单。
     */
    ContractPlanOrderResponse cancelPlanOrder(ContractPlanOrderCancelRequest request);

    /**
     * 查询计划单列表。
     */
    ContractPlanOrderListResponse listOrders(Long userId);
}
