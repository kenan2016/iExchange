package com.iexchange.spot.controller;

import com.iexchange.common.response.R;
import com.iexchange.spot.dto.CancelOrderRequest;
import com.iexchange.spot.dto.PlaceOrderRequest;
import com.iexchange.spot.dto.SpotOrderDetailResponse;
import com.iexchange.spot.dto.SpotOrderResponse;
import com.iexchange.spot.es.SpotOrderDocument;
import com.iexchange.spot.service.SpotOrderService;
import com.iexchange.spot.service.SpotOrderSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 现货订单接口。
 */
@Tag(name = "现货订单", description = "现货下单与撤单接口")
@RestController
@RequestMapping("/api/spot")
@Validated
public class SpotOrderController {

    private final SpotOrderService orderService;
    private final SpotOrderSearchService searchService;

    public SpotOrderController(SpotOrderService orderService, SpotOrderSearchService searchService) {
        this.orderService = orderService;
        this.searchService = searchService;
    }

    /**
     * 提交现货订单（限价/市价）。
     */
    @Operation(summary = "提交现货订单", description = "支持限价与市价订单")
    @PostMapping("/order")
    public R<SpotOrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        try {
            SpotOrderResponse response = orderService.placeOrder(request);
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 撤销现货订单。
     */
    @Operation(summary = "撤销现货订单", description = "撤销未成交/部分成交订单")
    @PostMapping("/order/cancel")
    public R<SpotOrderResponse> cancelOrder(@Valid @RequestBody CancelOrderRequest request) {
        try {
            SpotOrderResponse response = orderService.cancelOrder(request);
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询现货订单详情。
     */
    @Operation(summary = "查询现货订单", description = "按订单ID查询详情")
    @GetMapping("/order")
    public R<SpotOrderDetailResponse> getOrder(@Parameter(description = "订单ID", required = true)
                                               @RequestParam("orderId") Long orderId) {
        try {
            SpotOrderDetailResponse response = orderService.getOrder(orderId);
            return R.ok("查询成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 使用 ES 查询订单（演示）。
     */
    @Operation(summary = "订单搜索(ES)", description = "通过 ES 条件查询现货订单")
    @GetMapping("/order/search")
    public R<List<SpotOrderDocument>> search(@Parameter(description = "用户ID")
                                             @RequestParam(value = "userId", required = false) Long userId,
                                             @Parameter(description = "交易对")
                                             @RequestParam(value = "symbol", required = false) String symbol,
                                             @Parameter(description = "状态")
                                             @RequestParam(value = "status", required = false) String status,
                                             @Parameter(description = "方向")
                                             @RequestParam(value = "side", required = false) String side,
                                             @Parameter(description = "类型")
                                             @RequestParam(value = "type", required = false) String type,
                                             @Parameter(description = "开始时间 yyyy-MM-dd HH:mm:ss")
                                             @RequestParam(value = "startTime", required = false) String startTime,
                                             @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss")
                                             @RequestParam(value = "endTime", required = false) String endTime,
                                             @Parameter(description = "返回条数")
                                             @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            List<SpotOrderDocument> list = searchService.search(
                userId, symbol, status, side, type, startTime, endTime, limit);
            return R.ok("查询成功", list);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }
}
