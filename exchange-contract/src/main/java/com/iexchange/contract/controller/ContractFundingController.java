package com.iexchange.contract.controller;

import com.iexchange.common.response.R;
import com.iexchange.contract.dto.FundingRateRequest;
import com.iexchange.contract.dto.FundingRateResponse;
import com.iexchange.contract.dto.FundingSettleRequest;
import com.iexchange.contract.dto.FundingSettleResponse;
import com.iexchange.contract.service.ContractFundingService;
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
 * 资金费率接口。
 */
@Tag(name = "资金费率", description = "资金费率计算与结算接口（多空资金交换）")
@RestController
@RequestMapping("/api/contract/funding")
@Validated
public class ContractFundingController {

    private final ContractFundingService fundingService;

    public ContractFundingController(ContractFundingService fundingService) {
        this.fundingService = fundingService;
    }

    /**
     * 手动计算资金费率（演示）。
     */
    @Operation(summary = "计算资金费率", description = "根据标记价与指数价计算资金费率（正数=多头支付）")
    @PostMapping("/rate/calc")
    public R<FundingRateResponse> calculateRate(@Valid @RequestBody FundingRateRequest request) {
        try {
            FundingRateResponse response = fundingService.calculateRate(request);
            return R.ok("计算成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询最新资金费率。
     */
    @Operation(summary = "查询最新资金费率", description = "按交易对查询最新费率与下次结算时间")
    @GetMapping("/rate")
    public R<FundingRateResponse> latestRate(@Parameter(description = "交易对", required = true)
                                             @RequestParam("symbol") String symbol) {
        try {
            FundingRateResponse response = fundingService.getLatestRate(symbol);
            return R.ok("查询成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 结算资金费率。
     */
    @Operation(summary = "结算资金费率", description = "对持仓进行资金费率结算并计入账户余额")
    @PostMapping("/settle")
    public R<FundingSettleResponse> settle(@Valid @RequestBody FundingSettleRequest request) {
        try {
            FundingSettleResponse response = fundingService.settleFunding(request);
            return R.ok("结算完成", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }
}
