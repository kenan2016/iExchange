# 阶段 12：行情 Disruptor 接入

## 目标
- 行情消费链路改为 Disruptor 队列化处理
- 将 Kafka 消费与行情计算解耦，提升可读性与扩展性

## 涉及模块
- `exchange-market`

## 关键改动
- 新增 Disruptor 依赖
- 新增行情事件模型与处理器
- Kafka 消费者改为投递队列
- 新增环形队列配置

## 步骤 1：引入 Disruptor 依赖
文件：`exchange-market/pom.xml`
- 新增 `com.lmax:disruptor` 依赖，版本由根 `pom.xml` 管理。

## 步骤 2：新增行情事件与处理器
新增文件：
- `exchange-market/src/main/java/com/iexchange/market/messaging/MarketTradeEvent.java`
- `exchange-market/src/main/java/com/iexchange/market/messaging/MarketTradeEventFactory.java`
- `exchange-market/src/main/java/com/iexchange/market/messaging/MarketTradeEventHandler.java`
- `exchange-market/src/main/java/com/iexchange/market/messaging/MarketTradeDisruptor.java`

说明：
- `MarketTradeEvent` 为事件载体。
- `MarketTradeEventHandler` 统一处理 ticker、kline、depth 与 WS 推送。
- `MarketTradeDisruptor` 负责启动队列与发布事件。

## 步骤 3：Kafka 消费者改为投递队列
文件：`exchange-market/src/main/java/com/iexchange/market/messaging/SpotTradeConsumer.java`
- 解析消息后不再直接计算行情，改为调用 `MarketTradeDisruptor.publish()`。

## 步骤 4：新增配置项
文件：`exchange-market/src/main/resources/application.yml`
- 新增 `market.trade.buffer-size`，用于控制环形队列大小。

示例：
```yaml
market:
  trade:
    topic: spot.trade
    buffer-size: 1024
```

## 核心流程说明
1. `exchange-spot` 产生成交事件并写入 Kafka。
2. `exchange-market` 的 `SpotTradeConsumer` 消费消息后投递到 Disruptor。
3. `MarketTradeEventHandler` 统一触发 ticker / kline / depth / WS 推送。

## 验证方式
1. 启动 `exchange-spot` 与 `exchange-market`。
2. 在现货下单形成成交后，访问行情接口：
   - `GET /api/market/ticker?symbol=BTC_USDT`
   - `GET /api/market/klines?symbol=BTC_USDT&interval=1m&limit=10`
   - `GET /api/market/depth?symbol=BTC_USDT&limit=5`
3. 可在日志中看到 `market-trade-disruptor-*` 线程处理记录。

## 小结
- 行情链路完成 Disruptor 化，后续可扩展批量处理、异步落库与更多行情指标。
