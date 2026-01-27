# 阶段 15：行情 WebSocket 推送补齐

## 目标
- 在 WebSocket 推送中补齐 ticker/depth/kline
- 支持 ALL 与按交易对订阅

## 涉及模块
- `exchange-market`

## 步骤 1：扩展 WebSocket 推送能力
改动文件：
- `exchange-market/src/main/java/com/iexchange/market/websocket/MarketWebSocketServer.java`

新增推送：
- `broadcastTicker`
- `broadcastDepth`
- `broadcastKline`

## 步骤 2：成交事件处理后补齐推送
改动文件：
- `exchange-market/src/main/java/com/iexchange/market/messaging/MarketTradeEventHandler.java`
- `exchange-market/src/main/java/com/iexchange/market/messaging/MarketTradeDisruptor.java`

说明：
- 成交事件处理完成后，拉取最新行情并推送。

## 步骤 3：新增配置项
改动文件：
- `exchange-market/src/main/resources/application.yml`

新增配置：
```yaml
market:
  websocket:
    depth-levels: 20
    kline-interval: 1m
```

## WebSocket 订阅示例
- 订阅全部交易对：
```
ws://127.0.0.1:19090/ws
```

- 订阅指定交易对：
```
ws://127.0.0.1:19090/ws?symbol=BTC_USDT
```

推送示例（结构）：
```json
{"type":"ticker","time":"2026-01-01T12:00:00","data":{...}}
```

## 小结
- 行情推送从仅成交扩展为 ticker/depth/kline。
- 学生可通过浏览器或脚本直接验证推送数据。
