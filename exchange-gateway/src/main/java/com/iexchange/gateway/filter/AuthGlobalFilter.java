package com.iexchange.gateway.filter;

import com.iexchange.gateway.config.GatewayAuthProperties;
import com.iexchange.gateway.security.JwtTokenService;
import com.iexchange.gateway.security.JwtTokenService.JwtUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关统一鉴权过滤器。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_HEADER = "X-Token";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";

    private final GatewayAuthProperties authProperties;
    private final JwtTokenService jwtTokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthGlobalFilter(GatewayAuthProperties authProperties, JwtTokenService jwtTokenService) {
        this.authProperties = authProperties;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!authProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        // 白名单放行（登录/行情等公开接口）
        if (isIgnored(path, authProperties.getIgnorePaths())) {
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst(TOKEN_HEADER);
        if (token == null || token.trim().isEmpty()) {
            return unauthorized(exchange, "Token 缺失");
        }
        // 校验 JWT 并解析用户信息
        JwtUser jwtUser = jwtTokenService.parse(token);
        if (jwtUser == null) {
            return unauthorized(exchange, "Token 无效或已过期");
        }
        String userId = String.valueOf(jwtUser.getUserId());
        String username = jwtUser.getUsername() == null ? "" : jwtUser.getUsername();
        // 将用户身份透传给下游服务
        return chain.filter(exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header(USER_ID_HEADER, userId)
                .header(USERNAME_HEADER, username)
                .build())
            .build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isIgnored(String path, List<String> ignorePaths) {
        if (ignorePaths == null || ignorePaths.isEmpty()) {
            return false;
        }
        for (String pattern : ignorePaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // ：返回统一结构的 JSON
        long timestamp = System.currentTimeMillis();
        String payload = "{\"code\":401,\"message\":\"" + message
            + "\",\"data\":null,\"timestamp\":" + timestamp + "}";
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
