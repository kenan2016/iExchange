package com.iexchange.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iexchange.wallet.entity.WalletAccountEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钱包账户 Mapper。
 */
@Mapper
public interface WalletAccountMapper extends BaseMapper<WalletAccountEntity> {
}
