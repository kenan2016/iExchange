package com.iexchange.wallet.controller;

import com.iexchange.common.response.R;
import com.iexchange.wallet.chain.WalletChainAddressService;
import com.iexchange.wallet.chain.WalletChainService;
import com.iexchange.wallet.dto.WalletChainAddressRequest;
import com.iexchange.wallet.dto.WalletChainAddressResponse;
import com.iexchange.wallet.dto.WalletChainWithdrawRequest;
import com.iexchange.wallet.dto.WalletChainWithdrawResponse;
import com.iexchange.wallet.entity.WalletChainAddressEntity;
import com.iexchange.wallet.entity.WalletChainWithdrawEntity;
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
 * 链上充提接口（演示版）。
 */
@Tag(name = "链上充提", description = "链上充值地址与提币接口")
@RestController
@RequestMapping("/api/wallet/chain")
@Validated
public class WalletChainController {

    private final WalletChainAddressService addressService;
    private final WalletChainService chainService;

    public WalletChainController(WalletChainAddressService addressService,
                                 WalletChainService chainService) {
        this.addressService = addressService;
        this.chainService = chainService;
    }

    @Operation(summary = "申请充值地址", description = "交易所生成并返回用户的链上充值地址")
    @PostMapping("/address")
    public R<WalletChainAddressResponse> createAddress(@Valid @RequestBody WalletChainAddressRequest request) {
        WalletChainAddressEntity entity = addressService.getOrCreateAddress(
            request.getUserId(),
            request.getChainName());
        WalletChainAddressResponse response = new WalletChainAddressResponse();
        response.setUserId(entity.getUserId());
        response.setChainName(entity.getChainName());
        response.setAddress(entity.getAddress());
        return R.ok("申请成功", response);
    }

    @Operation(summary = "查询链上地址", description = "查询用户的链上充值地址")
    @GetMapping("/address")
    public R<WalletChainAddressResponse> getAddress(@Parameter(description = "用户ID", required = true)
                                                    @RequestParam("userId") Long userId,
                                                    @Parameter(description = "链名称标识（可选）")
                                                    @RequestParam(value = "chainName", required = false) String chainName) {
        WalletChainAddressEntity entity = addressService.getAddress(userId, chainName);
        if (entity == null) {
            return R.fail("未申请链上地址");
        }
        WalletChainAddressResponse response = new WalletChainAddressResponse();
        response.setUserId(entity.getUserId());
        response.setChainName(entity.getChainName());
        response.setAddress(entity.getAddress());
        return R.ok("查询成功", response);
    }

    @Operation(summary = "链上提币", description = "从交易所热钱包提币到用户地址")
    @PostMapping("/withdraw")
    public R<WalletChainWithdrawResponse> withdraw(@Valid @RequestBody WalletChainWithdrawRequest request) {
        WalletChainWithdrawEntity entity = chainService.requestWithdraw(
            request.getUserId(),
            request.getAsset(),
            request.getAmount(),
            request.getToAddress(),
            request.getRequestId());
        WalletChainWithdrawResponse response = new WalletChainWithdrawResponse();
        response.setRequestId(entity.getRequestId());
        response.setTxHash(entity.getTxHash());
        response.setStatus(entity.getStatus());
        return R.ok("提币提交成功", response);
    }
}
