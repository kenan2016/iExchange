package com.iexchange.spot.service;

import com.iexchange.spot.dto.CancelOrderRequest;
import com.iexchange.spot.dto.PlaceOrderRequest;
import com.iexchange.spot.dto.SpotOrderDetailResponse;
import com.iexchange.spot.dto.SpotOrderResponse;

/**
 * 现货订单服务。
 */
public interface SpotOrderService {

    SpotOrderResponse placeOrder(PlaceOrderRequest request);

    SpotOrderResponse cancelOrder(CancelOrderRequest request);

    SpotOrderDetailResponse getOrder(Long orderId);
}
