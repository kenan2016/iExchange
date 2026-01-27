package com.iexchange.contract.controller;

import com.iexchange.common.response.R;
import com.iexchange.contract.dto.ContractAccountRequest;
import com.iexchange.contract.dto.ContractAccountResponse;
import com.iexchange.contract.dto.ContractOrderCancelRequest;
import com.iexchange.contract.dto.ContractOrderRequest;
import com.iexchange.contract.dto.ContractOrderResponse;
import com.iexchange.contract.dto.ContractPositionResponse;
import com.iexchange.contract.es.ContractOrderDocument;
import com.iexchange.contract.entity.ContractAccountEntity;
import com.iexchange.contract.service.ContractAccountService;
import com.iexchange.contract.service.ContractOrderService;
import com.iexchange.contract.service.ContractOrderSearchService;
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
 * 合约接口。
 */
@Tag(name = "合约交易", description = "合约账户与订单接口")
@RestController
@RequestMapping("/api/contract")
@Validated
public class ContractController {

    private final ContractAccountService accountService;
    private final ContractOrderService orderService;
    private final ContractOrderSearchService searchService;

    public ContractController(ContractAccountService accountService,
                              ContractOrderService orderService,
                              ContractOrderSearchService searchService) {
        this.accountService = accountService;
        this.orderService = orderService;
        this.searchService = searchService;
    }

    /**
     * 合约账户入金。
     */
    @Operation(summary = "合约账户入金", description = "向合约保证金账户充值（用于开仓保证金）")
    @PostMapping("/account/deposit")
    public R<ContractAccountResponse> deposit(@Valid @RequestBody ContractAccountRequest request) {
        try {
            ContractAccountEntity account = accountService.deposit(request.getUserId(), request.getAmount());
            ContractAccountResponse response = ContractAccountResponse.ok(account.getUserId(), account.getBalance());
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询合约账户余额。
     */
    @Operation(summary = "查询合约账户", description = "查询合约保证金账户余额（可用保证金）")
    @GetMapping("/account")
    public R<ContractAccountResponse> account(@Parameter(description = "用户ID", required = true)
                                              @RequestParam("userId") Long userId) {
        ContractAccountEntity account = accountService.getOrCreate(userId);
        ContractAccountResponse response = ContractAccountResponse.ok(account.getUserId(), account.getBalance());
        return R.ok("查询成功", response);
    }

    /**
     * 提交合约订单。
     */
    @Operation(summary = "提交合约订单", description = "支持开仓/平仓、做多/做空，以及全仓/逐仓")
    @PostMapping("/order")
    public R<ContractOrderResponse> submitOrder(@Valid @RequestBody ContractOrderRequest request) {
        try {
            ContractOrderResponse response = orderService.submitOrder(request);
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 撤销合约订单。
     */
    @Operation(summary = "撤销合约订单", description = "撤销未成交订单（仅本人订单）")
    @PostMapping("/order/cancel")
    public R<ContractOrderResponse> cancelOrder(@Valid @RequestBody ContractOrderCancelRequest request) {
        try {
            ContractOrderResponse response = orderService.cancelOrder(request);
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询合约持仓信息。
     */
    @Operation(summary = "查询合约持仓", description = "按用户、方向与保证金模式查询持仓")
    @GetMapping("/position")
    public R<ContractPositionResponse> position(@Parameter(description = "用户ID", required = true)
                                                @RequestParam("userId") Long userId,
                                                @Parameter(description = "交易对", required = true)
                                                @RequestParam("symbol") String symbol,
                                                @Parameter(description = "方向：LONG=看涨做多，SHORT=看跌做空", required = true)
                                                @RequestParam("side") String side,
                                                @Parameter(description = "保证金模式：CROSS=全仓，ISOLATED=逐仓", required = true)
                                                @RequestParam("marginMode") String marginMode) {
        try {
            ContractPositionResponse response = orderService.getPosition(userId, symbol, side, marginMode);
            return R.ok("查询成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 使用 ES 查询合约订单（演示）。
     */
    @Operation(summary = "订单搜索(ES)", description = "通过 ES 条件查询合约订单")
    @GetMapping("/order/search")
    public R<List<ContractOrderDocument>> search(@Parameter(description = "用户ID")
                                                 @RequestParam(value = "userId", required = false) Long userId,
                                                 @Parameter(description = "交易对")
                                                 @RequestParam(value = "symbol", required = false) String symbol,
                                                 @Parameter(description = "状态")
                                                 @RequestParam(value = "status", required = false) String status,
                                                 @Parameter(description = "开平：OPEN/CLOSE")
                                                 @RequestParam(value = "action", required = false) String action,
                                                 @Parameter(description = "方向：LONG/SHORT")
                                                 @RequestParam(value = "side", required = false) String side,
                                                 @Parameter(description = "保证金模式：CROSS/ISOLATED")
                                                 @RequestParam(value = "marginMode", required = false) String marginMode,
                                                 @Parameter(description = "开始时间 yyyy-MM-dd HH:mm:ss")
                                                 @RequestParam(value = "startTime", required = false) String startTime,
                                                 @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss")
                                                 @RequestParam(value = "endTime", required = false) String endTime,
                                                 @Parameter(description = "返回条数")
                                                 @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            List<ContractOrderDocument> list = searchService.search(
                userId, symbol, status, action, side, marginMode, startTime, endTime, limit);
            return R.ok("查询成功", list);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }
}
