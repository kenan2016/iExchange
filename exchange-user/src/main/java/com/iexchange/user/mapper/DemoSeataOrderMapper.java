package com.iexchange.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iexchange.user.entity.DemoSeataOrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Seata 极简演示订单 Mapper。
 */
@Mapper
public interface DemoSeataOrderMapper extends BaseMapper<DemoSeataOrderEntity> {
}
