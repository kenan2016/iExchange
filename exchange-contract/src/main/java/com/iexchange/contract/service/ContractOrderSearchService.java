package com.iexchange.contract.service;

import com.iexchange.contract.es.ContractOrderDocument;
import com.iexchange.contract.es.ContractOrderEsMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 合约订单 ES 查询服务（演示版）。
 */
@Service
public class ContractOrderSearchService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ContractOrderEsMapper esMapper;

    public ContractOrderSearchService(ContractOrderEsMapper esMapper) {
        this.esMapper = esMapper;
    }

    public List<ContractOrderDocument> search(Long userId,
                                              String symbol,
                                              String status,
                                              String action,
                                              String side,
                                              String marginMode,
                                              String startTime,
                                              String endTime,
                                              Integer limit) {
        LambdaEsQueryWrapper<ContractOrderDocument> wrapper = new LambdaEsQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(ContractOrderDocument::getUserId, userId);
        }
        if (StringUtils.hasText(symbol)) {
            wrapper.eq(ContractOrderDocument::getSymbol, symbol);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ContractOrderDocument::getStatus, status);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(ContractOrderDocument::getAction, action);
        }
        if (StringUtils.hasText(side)) {
            wrapper.eq(ContractOrderDocument::getSide, side);
        }
        if (StringUtils.hasText(marginMode)) {
            wrapper.eq(ContractOrderDocument::getMarginMode, marginMode);
        }
        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);
        if (start != null) {
            wrapper.ge(ContractOrderDocument::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.le(ContractOrderDocument::getCreatedAt, end);
        }
        wrapper.orderByDesc(ContractOrderDocument::getCreatedAt);
        List<ContractOrderDocument> list = esMapper.selectList(wrapper);
        if (limit == null || limit <= 0) {
            return list;
        }
        return list.stream().limit(limit).toList();
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), TIME_FORMATTER);
        } catch (Exception ex) {
            throw new IllegalArgumentException("时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
    }
}
