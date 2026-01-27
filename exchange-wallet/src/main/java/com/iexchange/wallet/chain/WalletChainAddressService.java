package com.iexchange.wallet.chain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.wallet.entity.WalletChainAddressEntity;
import com.iexchange.wallet.mapper.WalletChainAddressMapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

/**
 * 链上充值地址服务。
 */
@Slf4j
@Service
public class WalletChainAddressService {

    private final WalletChainAddressMapper addressMapper;
    private final WalletChainProperties properties;

    public WalletChainAddressService(WalletChainAddressMapper addressMapper, WalletChainProperties properties) {
        this.addressMapper = addressMapper;
        this.properties = properties;
    }

    public WalletChainAddressEntity getOrCreateAddress(Long userId, String chainName) {
        String normalizedChain = normalizeChain(chainName);
        WalletChainAddressEntity existingByUser = addressMapper.selectOne(
            new LambdaQueryWrapper<WalletChainAddressEntity>()
                .eq(WalletChainAddressEntity::getUserId, userId)
                .eq(WalletChainAddressEntity::getChainName, normalizedChain));
        if (existingByUser != null) {
            return existingByUser;
        }
        return generateAddress(userId, normalizedChain);
    }

    public WalletChainAddressEntity getAddress(Long userId, String chainName) {
        String normalizedChain = normalizeChain(chainName);
        return addressMapper.selectOne(new LambdaQueryWrapper<WalletChainAddressEntity>()
            .eq(WalletChainAddressEntity::getUserId, userId)
            .eq(WalletChainAddressEntity::getChainName, normalizedChain));
    }

    public WalletChainAddressEntity getByAddress(String chainName, String address) {
        String normalizedChain = normalizeChain(chainName);
        String normalizedAddress = normalizeAddress(address);
        return addressMapper.selectOne(new LambdaQueryWrapper<WalletChainAddressEntity>()
            .eq(WalletChainAddressEntity::getChainName, normalizedChain)
            .eq(WalletChainAddressEntity::getAddress, normalizedAddress));
    }

    public List<WalletChainAddressEntity> listAll(String chainName) {
        String normalizedChain = normalizeChain(chainName);
        return addressMapper.selectList(new LambdaQueryWrapper<WalletChainAddressEntity>()
            .eq(WalletChainAddressEntity::getChainName, normalizedChain));
    }

    private WalletChainAddressEntity generateAddress(Long userId, String chainName) {
        for (int attempt = 0; attempt < 5; attempt++) {
            ECKeyPair keyPair;
            try {
                keyPair = Keys.createEcKeyPair(new SecureRandom());
            } catch (Exception ex) {
                throw new IllegalStateException("生成充值地址失败", ex);
            }
            Credentials credentials = Credentials.create(keyPair);
            String address = normalizeAddress(credentials.getAddress());
            WalletChainAddressEntity existingByAddress = addressMapper.selectOne(
                new LambdaQueryWrapper<WalletChainAddressEntity>()
                    .eq(WalletChainAddressEntity::getChainName, chainName)
                    .eq(WalletChainAddressEntity::getAddress, address));
            if (existingByAddress != null) {
                continue;
            }
            WalletChainAddressEntity entity = new WalletChainAddressEntity();
            entity.setUserId(userId);
            entity.setChainName(chainName);
            entity.setAddress(address);
            entity.setPrivateKey(Numeric.toHexStringNoPrefixZeroPadded(keyPair.getPrivateKey(), 64));
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            addressMapper.insert(entity);
            log.info("生成用户充值地址 userId={}, chain={}, address={}", userId, chainName, address);
            return entity;
        }
        throw new IllegalStateException("生成充值地址失败，已重试");
    }

    private String normalizeChain(String chainName) {
        String name = chainName == null || chainName.isBlank() ? properties.getName() : chainName;
        return name.trim().toLowerCase();
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
