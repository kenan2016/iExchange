package com.iexchange.market.controller;

import com.iexchange.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SETNX 幂等演示接口。
 */
@Tag(name = "幂等演示", description = "SETNX 幂等示例")
@RestController
@RequestMapping("/api/market/idempotent")
@Validated
public class MarketIdempotentController {

    private final StringRedisTemplate redisTemplate;

    public MarketIdempotentController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 使用 SETNX 做幂等演示。
     */
    @Operation(summary = "SETNX 幂等演示", description = "同一个 requestId 只允许处理一次")
    @GetMapping("/demo")
    public R<Boolean> demo(@Parameter(description = "请求ID", required = true, example = "demo-1001")
                           @RequestParam("requestId") String requestId) {
        String key = "market:idempotent:" + requestId;
        // SETNX：首次返回 true，重复请求返回 false
        Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        if (Boolean.TRUE.equals(first)) {
            return R.ok("处理成功", true);
        }
        return R.ok("重复请求", false);
    }
}
