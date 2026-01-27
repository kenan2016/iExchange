package com.iexchange.wallet.chain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.wallet.entity.WalletChainConfigEntity;
import com.iexchange.wallet.mapper.WalletChainConfigMapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

/**
 * 链上配置服务（开发环境使用）。
 */
@Slf4j
@Service
public class WalletChainConfigService {

    private static final String KEY_PRIVATE = "hot_private_key";
    private static final String KEY_ADDRESS = "hot_address";

    private final WalletChainConfigMapper configMapper;
    private final WalletChainProperties properties;

    public WalletChainConfigService(WalletChainConfigMapper configMapper, WalletChainProperties properties) {
        this.configMapper = configMapper;
        this.properties = properties;
    }

    public String resolvePrivateKey() {
        String configured = normalize(properties.getHotWallet().getPrivateKey());
        if (configured != null) {
            return configured;
        }
        WalletChainConfigEntity existing = findConfig(KEY_PRIVATE);
        if (existing != null) {
            return existing.getConfigValue();
        }
        String generated = generatePrivateKey();
        saveConfig(KEY_PRIVATE, generated);
        Credentials credentials = Credentials.create(generated);
        saveConfig(KEY_ADDRESS, credentials.getAddress());
        log.info("链上热钱包已自动生成，请向该地址转入燃料费：{}", credentials.getAddress());
        return generated;
    }

    public String resolveAddress() {
        String configured = normalize(properties.getHotWallet().getAddress());
        if (configured != null) {
            return configured;
        }
        WalletChainConfigEntity existing = findConfig(KEY_ADDRESS);
        if (existing != null) {
            return existing.getConfigValue();
        }
        String privateKey = resolvePrivateKey();
        String address = Credentials.create(privateKey).getAddress();
        saveConfig(KEY_ADDRESS, address);
        return address;
    }

    private WalletChainConfigEntity findConfig(String key) {
        return configMapper.selectOne(new LambdaQueryWrapper<WalletChainConfigEntity>()
            .eq(WalletChainConfigEntity::getConfigKey, key));
    }

    private void saveConfig(String key, String value) {
        WalletChainConfigEntity entity = findConfig(key);
        if (entity == null) {
            entity = new WalletChainConfigEntity();
            entity.setConfigKey(key);
            entity.setConfigValue(value);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            configMapper.insert(entity);
            return;
        }
        entity.setConfigValue(value);
        entity.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(entity);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generatePrivateKey() {
        try {
            ECKeyPair keyPair = Keys.createEcKeyPair(new SecureRandom());
            return keyPair.getPrivateKey().toString(16);
        } catch (Exception ex) {
            throw new IllegalStateException("生成热钱包私钥失败", ex);
        }
    }
}
