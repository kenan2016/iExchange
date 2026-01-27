package com.iexchange.contract.controller;

import com.iexchange.common.response.R;
import com.iexchange.contract.dto.ContractPlanOrderCancelRequest;
import com.iexchange.contract.dto.ContractPlanOrderListResponse;
import com.iexchange.contract.dto.ContractPlanOrderRequest;
import com.iexchange.contract.dto.ContractPlanOrderResponse;
import com.iexchange.contract.service.ContractPlanOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合约计划委托接口。
 */
@Tag(name = "合约计划委托", description = "计划委托下单与触发管理接口")
@RestController
@RequestMapping("/api/contract/plan")
@Validated
public class ContractPlanOrderController {

    private final ContractPlanOrderService planOrderService;

    public ContractPlanOrderController(ContractPlanOrderService planOrderService) {
        this.planOrderService = planOrderService;
    }

    /**
     * 创建合约计划委托。
     */
    @Operation(summary = "创建合约计划委托", description = "设置触发价，命中后生成真实合约订单")
    @PostMapping
    public R<ContractPlanOrderResponse> placePlanOrder(@Valid @RequestBody ContractPlanOrderRequest request) {
        try {
            ContractPlanOrderResponse response = planOrderService.placePlanOrder(request);
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 撤销合约计划委托。
     */
    @Operation(summary = "撤销合约计划委托", description = "撤销未触发的计划单（仅本人）")
    @PostMapping("/cancel")
    public R<ContractPlanOrderResponse> cancelPlanOrder(@Valid @RequestBody ContractPlanOrderCancelRequest request) {
        try {
            ContractPlanOrderResponse response = planOrderService.cancelPlanOrder(request);
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询合约计划委托列表。
     */
    @Operation(summary = "查询合约计划委托列表", description = "按用户查询计划委托列表")
    @GetMapping
    public R<ContractPlanOrderListResponse> listPlanOrders(@Parameter(description = "用户ID", required = true)
                                                           @RequestParam("userId") Long userId) {
        ContractPlanOrderListResponse response = planOrderService.listOrders(userId);
        return R.ok("查询成功", response);
    }
}
