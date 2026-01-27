package com.iexchange.market.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.document.KlineDocument;
import com.iexchange.market.service.model.DepthSnapshot;
import com.iexchange.market.service.model.TickerSnapshot;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 行情 WebSocket 服务（Netty 示例）。
 */
@Slf4j
@Component
public class MarketWebSocketServer {
    private static final AttributeKey<String> SYMBOL_KEY = AttributeKey.valueOf("symbol");
    private static final String ALL_SYMBOL = "ALL";

    private final ObjectMapper objectMapper;
    private final int port;
    private final ChannelGroup allGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final Map<String, ChannelGroup> symbolGroups = new ConcurrentHashMap<>();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public MarketWebSocketServer(ObjectMapper objectMapper,
                                 @Value("${market.websocket.port:19090}") int port) {
        this.objectMapper = objectMapper;
        this.port = port;
    }

    /**
     * 启动 Netty WebSocket 服务并建立管道。
     */
    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // HTTP 升级为 WebSocket 的标准管道
                        ch.pipeline()
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(65536))
                            .addLast(new WebSocketServerProtocolHandler("/ws", null, true))
                            .addLast(new MarketWebSocketHandler());
                    }
                });
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            log.info("行情 WebSocket 启动成功，端口：{}", port);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("行情 WebSocket 启动中断", ex);
        } catch (Exception ex) {
            log.warn("行情 WebSocket 启动失败", ex);
        }
    }

    /**
     * 停止 WebSocket 服务并释放线程池。
     */
    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 推送成交事件到 WebSocket。
     */
    public void broadcastTrade(SpotTradeEvent event) {
        broadcastBySymbol(event.getSymbol(), "trade", event);
    }

    /**
     * 推送 Ticker。
     */
    public void broadcastTicker(TickerSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        broadcastBySymbol(snapshot.getSymbol(), "ticker", snapshot);
    }

    /**
     * 推送深度。
     */
    public void broadcastDepth(DepthSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        broadcastBySymbol(snapshot.getSymbol(), "depth", snapshot);
    }

    /**
     * 推送 K 线。
     */
    public void broadcastKline(String symbol, String interval, KlineDocument document) {
        if (symbol == null || document == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("interval", interval);
        payload.put("kline", document);
        broadcastBySymbol(symbol, "kline", payload);
    }

    /**
     * 按交易对推送消息，同时广播 ALL 订阅。
     */
    private void broadcastBySymbol(String symbol, String type, Object data) {
        if (symbol == null) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("time", LocalDateTime.now());
            payload.put("data", data);
            String message = objectMapper.writeValueAsString(payload);
            ChannelGroup group = symbolGroups.get(symbol);
            if (group != null) {
                // 指定交易对订阅
                group.writeAndFlush(new TextWebSocketFrame(message));
            }
            // ALL 订阅接收全部行情
            allGroup.writeAndFlush(new TextWebSocketFrame(message));
        } catch (Exception ex) {
            log.warn("行情推送失败", ex);
        }
    }

    private class MarketWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

        /**
         * 握手成功后解析订阅交易对并加入对应分组。
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete handshake) {
                String symbol = parseSymbol(handshake.requestUri());
                // 记录订阅的交易对到 Channel 属性
                ctx.channel().attr(SYMBOL_KEY).set(symbol);
                if (ALL_SYMBOL.equals(symbol)) {
                    allGroup.add(ctx.channel());
                } else {
                    symbolGroups.computeIfAbsent(symbol, key -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE))
                        .add(ctx.channel());
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        /**
         * 目前忽略客户端消息，保留扩展订阅协议的入口。
         */
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
            // ：忽略客户端消息，可自行扩展订阅协议
        }

        /**
         * 连接断开时从订阅分组移除。
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            String symbol = ctx.channel().attr(SYMBOL_KEY).get();
            if (symbol == null || ALL_SYMBOL.equals(symbol)) {
                allGroup.remove(ctx.channel());
            } else {
                ChannelGroup group = symbolGroups.get(symbol);
                if (group != null) {
                    group.remove(ctx.channel());
                }
            }
            ctx.fireChannelInactive();
        }

        /**
         * 连接异常时关闭连接并输出日志。
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.warn("WebSocket 连接异常", cause);
            ctx.close();
        }
    }

    /**
     * 从请求 URL 中解析订阅的交易对，缺省为 ALL。
     */
    private String parseSymbol(String requestUri) {
        try {
            URI uri = new URI(requestUri);
            String query = uri.getQuery();
            if (query == null) {
                return ALL_SYMBOL;
            }
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=");
                if (parts.length == 2 && "symbol".equalsIgnoreCase(parts[0])) {
                    String value = parts[1].trim();
                    return value.isEmpty() ? ALL_SYMBOL : value;
                }
            }
        } catch (Exception ex) {
            log.warn("解析 WebSocket 请求失败", ex);
        }
        return ALL_SYMBOL;
    }
}
