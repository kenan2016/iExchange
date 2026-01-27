package com.iexchange.spot.controller;

import com.iexchange.common.response.R;
import com.iexchange.spot.dto.PlanOrderCancelRequest;
import com.iexchange.spot.dto.PlanOrderListResponse;
import com.iexchange.spot.dto.PlanOrderRequest;
import com.iexchange.spot.dto.PlanOrderResponse;
import com.iexchange.spot.service.SpotPlanOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计划委托接口。
 */
@Tag(name = "现货计划委托", description = "现货计划委托接口")
@RestController
@RequestMapping("/api/spot/plan")
@Validated
public class SpotPlanOrderController {

    private final SpotPlanOrderService planOrderService;

    public SpotPlanOrderController(SpotPlanOrderService planOrderService) {
        this.planOrderService = planOrderService;
    }

    /**
     * 创建现货计划委托。
     */
    @Operation(summary = "创建计划委托", description = "创建现货计划委托订单")
    @PostMapping
    public R<PlanOrderResponse> placePlanOrder(@Valid @RequestBody PlanOrderRequest request) {
        try {
            PlanOrderResponse response = planOrderService.placePlanOrder(request);
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 撤销现货计划委托。
     */
    @Operation(summary = "撤销计划委托", description = "撤销未触发的计划委托")
    @PostMapping("/cancel")
    public R<PlanOrderResponse> cancelPlanOrder(@Valid @RequestBody PlanOrderCancelRequest request) {
        try {
            PlanOrderResponse response = planOrderService.cancelPlanOrder(request);
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询用户计划委托列表。
     */
    @Operation(summary = "查询计划委托列表", description = "按用户查询计划委托列表")
    @GetMapping
    public R<PlanOrderListResponse> listPlanOrders(@Parameter(description = "用户ID", required = true)
                                                   @RequestParam("userId") @NotNull Long userId) {
        PlanOrderListResponse response = planOrderService.listOrders(userId);
        return R.ok("查询成功", response);
    }
}
