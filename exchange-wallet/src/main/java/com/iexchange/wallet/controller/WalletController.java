package com.iexchange.wallet.controller;

import com.iexchange.common.response.R;
import com.iexchange.wallet.dto.BalanceResponse;
import com.iexchange.wallet.dto.WalletRequest;
import com.iexchange.wallet.dto.WalletResponse;
import com.iexchange.wallet.entity.WalletAccountEntity;
import com.iexchange.wallet.service.WalletService;
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
 * 钱包接口。
 */
@Tag(name = "钱包接口", description = "钱包资金相关接口")
@RestController
@RequestMapping("/api/wallet")
@Validated
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * 模拟入金接口。
     */
    @Operation(summary = "模拟入金", description = "用于的模拟入金接口")
    @PostMapping("/deposit")
    public R<WalletResponse> deposit(@Valid @RequestBody WalletRequest request) {
        try {
            WalletAccountEntity account = walletService.deposit(
                request.getUserId(), request.getAsset(), request.getAmount(), request.getRequestId());
            WalletResponse response = WalletResponse.ok(account.getUserId(), account.getAsset(),
                account.getAvailableBalance(), account.getFrozenBalance(), account.getTotalBalance());
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 模拟出金接口。
     */
    @Operation(summary = "模拟出金", description = "用于的模拟出金接口")
    @PostMapping("/withdraw")
    public R<WalletResponse> withdraw(@Valid @RequestBody WalletRequest request) {
        try {
            WalletAccountEntity account = walletService.withdraw(
                request.getUserId(), request.getAsset(), request.getAmount(), request.getRequestId());
            WalletResponse response = WalletResponse.ok(account.getUserId(), account.getAsset(),
                account.getAvailableBalance(), account.getFrozenBalance(), account.getTotalBalance());
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 冻结资金接口。
     */
    @Operation(summary = "冻结资金", description = "下单前冻结资金")
    @PostMapping("/freeze")
    public R<WalletResponse> freeze(@Valid @RequestBody WalletRequest request) {
        try {
            WalletAccountEntity account = walletService.freeze(
                request.getUserId(), request.getAsset(), request.getAmount(), request.getRequestId());
            WalletResponse response = WalletResponse.ok(account.getUserId(), account.getAsset(),
                account.getAvailableBalance(), account.getFrozenBalance(), account.getTotalBalance());
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 解冻资金接口。
     */
    @Operation(summary = "解冻资金", description = "撤单或成交后释放冻结资金")
    @PostMapping("/unfreeze")
    public R<WalletResponse> unfreeze(@Valid @RequestBody WalletRequest request) {
        try {
            WalletAccountEntity account = walletService.unfreeze(
                request.getUserId(), request.getAsset(), request.getAmount(), request.getRequestId());
            WalletResponse response = WalletResponse.ok(account.getUserId(), account.getAsset(),
                account.getAvailableBalance(), account.getFrozenBalance(), account.getTotalBalance());
            return R.ok("操作成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询余额接口。
     */
    @Operation(summary = "查询余额", description = "查询指定用户与资产的余额")
    @GetMapping("/balance")
    public R<BalanceResponse> balance(@Parameter(description = "用户ID", required = true)
                                      @RequestParam("userId") Long userId,
                                      @Parameter(description = "资产类型", required = true)
                                      @RequestParam("asset") String asset) {
        WalletAccountEntity account = walletService.getBalance(userId, asset);
        if (account == null) {
            return R.fail("账户不存在");
        }
        BalanceResponse response = BalanceResponse.ok(account.getUserId(), account.getAsset(),
            account.getAvailableBalance(), account.getFrozenBalance(), account.getTotalBalance());
        return R.ok("查询成功", response);
    }
}
