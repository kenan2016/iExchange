package com.iexchange.wallet.chain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.wallet.entity.WalletChainCursorEntity;
import com.iexchange.wallet.mapper.WalletChainCursorMapper;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 链上扫描游标服务。
 */
@Slf4j
@Service
public class WalletChainCursorService {

    private final WalletChainCursorMapper cursorMapper;
    private final WalletChainProperties properties;

    public WalletChainCursorService(WalletChainCursorMapper cursorMapper, WalletChainProperties properties) {
        this.cursorMapper = cursorMapper;
        this.properties = properties;
    }

    public WalletChainCursorEntity getOrCreateCursor(String tokenAddress) {
        String chainName = properties.getName().toLowerCase();
        String normalizedToken = tokenAddress == null ? "" : tokenAddress.trim().toLowerCase();
        WalletChainCursorEntity existing = cursorMapper.selectOne(new LambdaQueryWrapper<WalletChainCursorEntity>()
            .eq(WalletChainCursorEntity::getChainName, chainName)
            .eq(WalletChainCursorEntity::getTokenAddress, normalizedToken));
        if (existing != null) {
            return existing;
        }
        WalletChainCursorEntity cursor = new WalletChainCursorEntity();
        cursor.setChainName(chainName);
        cursor.setTokenAddress(normalizedToken);
        cursor.setLastBlock(properties.getScan().getStartBlock());
        cursor.setUpdatedAt(LocalDateTime.now());
        cursorMapper.insert(cursor);
        return cursor;
    }

    public void updateCursor(WalletChainCursorEntity cursor, long lastBlock) {
        cursor.setLastBlock(lastBlock);
        cursor.setUpdatedAt(LocalDateTime.now());
        cursorMapper.updateById(cursor);
    }
}
