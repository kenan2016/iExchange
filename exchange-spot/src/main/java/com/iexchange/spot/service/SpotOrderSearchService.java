package com.iexchange.spot.service;

import com.iexchange.spot.es.SpotOrderDocument;
import com.iexchange.spot.es.SpotOrderEsMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 现货订单 ES 查询服务（演示版）。
 */
@Service
public class SpotOrderSearchService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SpotOrderEsMapper esMapper;

    public SpotOrderSearchService(SpotOrderEsMapper esMapper) {
        this.esMapper = esMapper;
    }

    public List<SpotOrderDocument> search(Long userId,
                                          String symbol,
                                          String status,
                                          String side,
                                          String type,
                                          String startTime,
                                          String endTime,
                                          Integer limit) {
        LambdaEsQueryWrapper<SpotOrderDocument> wrapper = new LambdaEsQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SpotOrderDocument::getUserId, userId);
        }
        if (StringUtils.hasText(symbol)) {
            wrapper.eq(SpotOrderDocument::getSymbol, symbol);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SpotOrderDocument::getStatus, status);
        }
        if (StringUtils.hasText(side)) {
            wrapper.eq(SpotOrderDocument::getSide, side);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(SpotOrderDocument::getType, type);
        }
        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);
        if (start != null) {
            wrapper.ge(SpotOrderDocument::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.le(SpotOrderDocument::getCreatedAt, end);
        }
        wrapper.orderByDesc(SpotOrderDocument::getCreatedAt);
        List<SpotOrderDocument> list = esMapper.selectList(wrapper);
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
