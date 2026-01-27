package com.iexchange.spot.service;

import com.iexchange.spot.dto.PlanOrderCancelRequest;
import com.iexchange.spot.dto.PlanOrderListResponse;
import com.iexchange.spot.dto.PlanOrderRequest;
import com.iexchange.spot.dto.PlanOrderResponse;

/**
 * 计划委托服务。
 */
public interface SpotPlanOrderService {

    PlanOrderResponse placePlanOrder(PlanOrderRequest request);

    PlanOrderResponse cancelPlanOrder(PlanOrderCancelRequest request);

    PlanOrderListResponse listOrders(Long userId);
}
