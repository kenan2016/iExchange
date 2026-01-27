package com.iexchange.wallet.chain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.wallet.entity.WalletChainAddressEntity;
import com.iexchange.wallet.entity.WalletChainCursorEntity;
import com.iexchange.wallet.entity.WalletChainWithdrawEntity;
import com.iexchange.wallet.mapper.WalletChainWithdrawMapper;
import com.iexchange.wallet.service.WalletService;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Log;

/**
 * 链上充提业务服务。
 */
@Slf4j
@Service
public class WalletChainService {

    private final WalletChainProperties properties;
    private final WalletChainClient chainClient;
    private final WalletChainAddressService addressService;
    private final WalletChainCursorService cursorService;
    private final WalletChainWithdrawMapper withdrawMapper;
    private final WalletChainConfigService configService;
    private final WalletService walletService;

    public WalletChainService(WalletChainProperties properties,
                              WalletChainClient chainClient,
                              WalletChainAddressService addressService,
                              WalletChainCursorService cursorService,
                              WalletChainWithdrawMapper withdrawMapper,
                              WalletChainConfigService configService,
                              WalletService walletService) {
        this.properties = properties;
        this.chainClient = chainClient;
        this.addressService = addressService;
        this.cursorService = cursorService;
        this.withdrawMapper = withdrawMapper;
        this.configService = configService;
        this.walletService = walletService;
    }

    /**
     * 扫描链上转账，完成入账。
     */
    @Transactional
    public int syncDeposits() {
        if (!properties.isEnabled()) {
            return 0;
        }
        String tokenAddress = chainClient.requireTokenAddress();
        WalletChainCursorEntity cursor = cursorService.getOrCreateCursor(tokenAddress);
        BigInteger latestBlock = chainClient.getLatestBlockNumber();
        BigInteger confirmedBlock = latestBlock.subtract(BigInteger.valueOf(properties.getConfirmations()));
        BigInteger fromBlock = BigInteger.valueOf(cursor.getLastBlock() + 1);
        if (confirmedBlock.compareTo(fromBlock) < 0) {
            return 0;
        }
        List<Log> logs = chainClient.getTransferLogs(fromBlock, confirmedBlock);
        int handled = 0;
        String hotAddress = normalizeAddress(configService.resolveAddress());
        for (Log logItem : logs) {
            String toAddress = normalizeAddress(parseAddress(logItem.getTopics().get(2)));
            String fromAddress = normalizeAddress(parseAddress(logItem.getTopics().get(1)));
            if (hotAddress.equals(fromAddress)) {
                // 热钱包出账不计为充值
                continue;
            }
            WalletChainAddressEntity address = addressService.getByAddress(properties.getName(), toAddress);
            if (address == null) {
                continue;
            }
            BigInteger rawValue = parseAmount(logItem.getData());
            BigDecimal amount = chainClient.toDecimalAmount(rawValue);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String requestId = "chain-deposit:" + logItem.getTransactionHash() + ":" + logItem.getLogIndex();
            walletService.deposit(address.getUserId(), properties.getToken().getSymbol(), amount, requestId);
            handled++;
        }
        cursorService.updateCursor(cursor, confirmedBlock.longValue());
        return handled;
    }

    /**
     * 创建链上提币订单并提交链上交易。
     */
    @Transactional
    public WalletChainWithdrawEntity requestWithdraw(Long userId, String asset, BigDecimal amount,
                                                     String toAddress, String requestId) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("链上功能未开启");
        }
        validateAsset(asset);
        String normalizedTo = normalizeAddress(toAddress);
        if (!org.web3j.crypto.WalletUtils.isValidAddress(normalizedTo)) {
            throw new IllegalArgumentException("无效提币地址");
        }
        WalletChainWithdrawEntity existing = withdrawMapper.selectOne(
            new LambdaQueryWrapper<WalletChainWithdrawEntity>()
                .eq(WalletChainWithdrawEntity::getRequestId, requestId));
        if (existing != null) {
            return existing;
        }
        walletService.freeze(userId, asset, amount, requestId);
        WalletChainWithdrawEntity withdraw = new WalletChainWithdrawEntity();
        withdraw.setUserId(userId);
        withdraw.setAsset(asset);
        withdraw.setChainName(properties.getName().toLowerCase());
        withdraw.setAmount(amount);
        withdraw.setToAddress(normalizedTo);
        withdraw.setRequestId(requestId);
        withdraw.setStatus(WalletChainWithdrawStatus.PENDING.getCode());
        withdraw.setCreatedAt(LocalDateTime.now());
        withdraw.setUpdatedAt(LocalDateTime.now());
        withdrawMapper.insert(withdraw);
        try {
            String txHash = chainClient.sendTokenTransfer(withdraw.getToAddress(), amount);
            withdraw.setTxHash(txHash);
            withdraw.setStatus(WalletChainWithdrawStatus.SUBMITTED.getCode());
            withdraw.setUpdatedAt(LocalDateTime.now());
            withdrawMapper.updateById(withdraw);
            return withdraw;
        } catch (Exception ex) {
            walletService.unfreeze(userId, asset, amount, requestId);
            withdraw.setStatus(WalletChainWithdrawStatus.FAILED.getCode());
            withdraw.setUpdatedAt(LocalDateTime.now());
            withdrawMapper.updateById(withdraw);
            throw ex;
        }
    }

    /**
     * 扫描链上回执，完成提币扣减。
     */
    @Transactional
    public int confirmWithdraws() {
        if (!properties.isEnabled()) {
            return 0;
        }
        List<WalletChainWithdrawEntity> submitted = withdrawMapper.selectList(
            new LambdaQueryWrapper<WalletChainWithdrawEntity>()
                .eq(WalletChainWithdrawEntity::getStatus, WalletChainWithdrawStatus.SUBMITTED.getCode()));
        if (submitted.isEmpty()) {
            return 0;
        }
        BigInteger latestBlock = chainClient.getLatestBlockNumber();
        int confirmedCount = 0;
        for (WalletChainWithdrawEntity withdraw : submitted) {
            if (withdraw.getTxHash() == null || withdraw.getTxHash().isBlank()) {
                continue;
            }
            EthGetTransactionReceipt receiptResp = chainClient.getReceipt(withdraw.getTxHash());
            var receiptOptional = receiptResp.getTransactionReceipt();
            if (receiptOptional.isEmpty()) {
                continue;
            }
            var receipt = receiptOptional.get();
            BigInteger receiptBlock = receipt.getBlockNumber();
            if (receiptBlock == null) {
                continue;
            }
            BigInteger neededBlock = receiptBlock.add(BigInteger.valueOf(properties.getConfirmations()));
            if (latestBlock.compareTo(neededBlock) < 0) {
                continue;
            }
            boolean success = "0x1".equals(receipt.getStatus());
            if (success) {
                walletService.deductFrozen(withdraw.getUserId(), withdraw.getAsset(), withdraw.getAmount(),
                    withdraw.getRequestId());
                withdraw.setStatus(WalletChainWithdrawStatus.CONFIRMED.getCode());
                confirmedCount++;
            } else {
                walletService.unfreeze(withdraw.getUserId(), withdraw.getAsset(), withdraw.getAmount(),
                    withdraw.getRequestId());
                withdraw.setStatus(WalletChainWithdrawStatus.FAILED.getCode());
            }
            withdraw.setUpdatedAt(LocalDateTime.now());
            withdrawMapper.updateById(withdraw);
        }
        return confirmedCount;
    }

    /**
     * 归集充值地址余额到热钱包。
     */
    @Transactional
    public int sweepDeposits() {
        if (!properties.isEnabled() || !properties.getSweep().isEnabled()) {
            return 0;
        }
        BigDecimal minAmount = properties.getSweep().getMinAmount();
        if (minAmount == null) {
            minAmount = BigDecimal.ZERO;
        }
        String hotAddress = normalizeAddress(configService.resolveAddress());
        List<WalletChainAddressEntity> addresses = addressService.listAll(properties.getName());
        int swept = 0;
        for (WalletChainAddressEntity address : addresses) {
            if (address.getPrivateKey() == null || address.getPrivateKey().isBlank()) {
                continue;
            }
            String normalizedAddress = normalizeAddress(address.getAddress());
            if (normalizedAddress.isBlank() || normalizedAddress.equals(hotAddress)) {
                continue;
            }
            BigDecimal balance = chainClient.getTokenBalance(normalizedAddress);
            if (balance.compareTo(minAmount) <= 0) {
                continue;
            }
            try {
                String txHash = chainClient.sendTokenTransferFrom(address.getPrivateKey(), hotAddress, balance);
                log.info("归集完成 address={}, amount={}, txHash={}", normalizedAddress, balance, txHash);
                swept++;
            } catch (Exception ex) {
                log.warn("归集失败 address={}", normalizedAddress, ex);
            }
        }
        return swept;
    }

    private void validateAsset(String asset) {
        String symbol = properties.getToken().getSymbol();
        if (symbol == null || !symbol.equalsIgnoreCase(asset)) {
            throw new IllegalArgumentException("不支持的资产：" + asset);
        }
    }

    private String parseAddress(String topic) {
        if (topic == null) {
            return "";
        }
        String value = topic.startsWith("0x") ? topic.substring(2) : topic;
        if (value.length() < 40) {
            return "";
        }
        return "0x" + value.substring(value.length() - 40);
    }

    private BigInteger parseAmount(String data) {
        if (data == null || data.length() < 3) {
            return BigInteger.ZERO;
        }
        String value = data.startsWith("0x") ? data.substring(2) : data;
        if (value.isEmpty()) {
            return BigInteger.ZERO;
        }
        return new BigInteger(value, 16);
    }

    private String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        String trimmed = address.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return "0x" + trimmed.substring(2).toLowerCase();
        }
        return "0x" + trimmed.toLowerCase();
    }
}
