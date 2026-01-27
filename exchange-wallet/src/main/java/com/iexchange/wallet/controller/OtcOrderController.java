package com.iexchange.wallet.controller;

import com.iexchange.common.response.R;
import com.iexchange.wallet.dto.OtcOrderActionRequest;
import com.iexchange.wallet.dto.OtcOrderCreateRequest;
import com.iexchange.wallet.dto.OtcOrderResponse;
import com.iexchange.wallet.entity.OtcOrderEntity;
import com.iexchange.wallet.otc.OtcOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OTC 订单接口（演示版）。
 */
@Tag(name = "OTC 订单", description = "OTC 订单示意接口")
@RestController
@RequestMapping("/api/wallet/otc/order")
@Validated
public class OtcOrderController {

    private final OtcOrderService orderService;

    public OtcOrderController(OtcOrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "创建订单", description = "创建 OTC 订单并冻结卖方资产")
    @PostMapping
    public R<OtcOrderResponse> create(@Valid @RequestBody OtcOrderCreateRequest request) {
        try {
            OtcOrderEntity order = orderService.createOrder(
                request.getOrderNo(),
                request.getBuyerId(),
                request.getSellerId(),
                request.getAsset(),
                request.getAmount());
            return R.ok("创建成功", toResponse(order));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @Operation(summary = "标记已付款", description = "买方标记已付款")
    @PostMapping("/paid")
    public R<OtcOrderResponse> markPaid(@Valid @RequestBody OtcOrderActionRequest request) {
        try {
            OtcOrderEntity order = orderService.markPaid(request.getOrderNo());
            return R.ok("操作成功", toResponse(order));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @Operation(summary = "放币", description = "卖方确认或仲裁后放币")
    @PostMapping("/release")
    public R<OtcOrderResponse> release(@Valid @RequestBody OtcOrderActionRequest request) {
        try {
            OtcOrderEntity order = orderService.release(request.getOrderNo());
            return R.ok("操作成功", toResponse(order));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @Operation(summary = "取消订单", description = "取消订单并解冻卖方资产")
    @PostMapping("/cancel")
    public R<OtcOrderResponse> cancel(@Valid @RequestBody OtcOrderActionRequest request) {
        try {
            OtcOrderEntity order = orderService.cancel(request.getOrderNo());
            return R.ok("操作成功", toResponse(order));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @Operation(summary = "查询订单", description = "查询指定订单")
    @GetMapping
    public R<OtcOrderResponse> getOrder(@Parameter(description = "订单号", required = true)
                                        @RequestParam("orderNo") String orderNo) {
        OtcOrderEntity order = orderService.getOrder(orderNo);
        if (order == null) {
            return R.fail("订单不存在");
        }
        return R.ok("查询成功", toResponse(order));
    }

    @Operation(summary = "查询用户订单", description = "按用户查询订单列表")
    @GetMapping("/list")
    public R<List<OtcOrderResponse>> list(@Parameter(description = "用户ID", required = true)
                                          @RequestParam("userId") Long userId) {
        List<OtcOrderResponse> responses = orderService.listByUser(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return R.ok("查询成功", responses);
    }

    private OtcOrderResponse toResponse(OtcOrderEntity order) {
        OtcOrderResponse response = new OtcOrderResponse();
        response.setOrderNo(order.getOrderNo());
        response.setBuyerId(order.getBuyerId());
        response.setSellerId(order.getSellerId());
        response.setAsset(order.getAsset());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus());
        response.setPaidAt(order.getPaidAt());
        response.setReleasedAt(order.getReleasedAt());
        response.setCanceledAt(order.getCanceledAt());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }
}
