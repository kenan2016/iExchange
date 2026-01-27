# Dubbo 接口调用说明（答疑）

## 问题
在 `com.iexchange.spot.client.WalletAccountClient#freeze` 调用
`com.iexchange.api.wallet.WalletAccountService#freeze` 时，
为什么 `exchange-api` 里只有接口，没有看到集成钱包服务的代码？

## 结论
这是 Dubbo 的正常用法：`exchange-api` 只提供“接口契约”，不包含实现。
消费者只依赖接口 jar，运行时由 Dubbo 通过注册中心找到提供者并生成代理调用。

## 调用链说明
1. **接口契约（API）**  
   `exchange-api` 定义接口与 DTO，不关心实现细节。  
   例如：`com.iexchange.api.wallet.WalletAccountService`

2. **提供者实现（Provider）**  
   `exchange-wallet` 实现接口并暴露 Dubbo 服务。  
   例如：`com.iexchange.wallet.service.WalletAccountDubboService`

3. **消费者引用（Consumer）**  
   `exchange-spot` 通过 `@DubboReference` 注入接口代理并调用。  
   例如：`com.iexchange.spot.client.WalletAccountClient`

## 为什么这样设计
- **解耦**：API 只描述“能做什么”，实现由独立服务负责，便于演进与扩展。  
- **复用**：多个服务可复用同一套接口契约。  
- **运行时路由**：通过 Nacos 注册发现自动定位服务实例。

## 小结
因此，在 `exchange-api` 看不到钱包实现代码是正常的，  
真正的业务实现都在 `exchange-wallet`，Dubbo 运行时完成路由与代理调用。

---

## 行情消息推送与数据流（答疑）

### 简化时序图
```
[SpotMatchingEngine]
    |
    | 1) 成交 -> build SpotTradeEvent
    v
[SpotTradeEventProducer] --Kafka(topic: spot.trade)--> [SpotTradeConsumer]
                                                    |
                                                    | 2) 反序列化
                                                    v
                                            [MarketTradeDisruptor]
                                                    |
                                                    | 3) 串行消费
                                                    v
                                          [MarketTradeEventHandler]
                                                    |
                       ------------------------------------------------
                       |                |               |            |
                       v                v               v            v
              [TickerService]     [KlineService]   [DepthService]  [WebSocket]
                       |                |               |            |
                       +----缓存/落库----+----缓存/落库----+----缓存----+--广播
```

### 推送链路说明
1. **撮合成交产生事件**  
   成交后在撮合引擎中组装 `SpotTradeEvent`，交给 `SpotTradeEventProducer`。
2. **Kafka 传递事件**  
   事件写入 Kafka 主题 `spot.trade`（配置在 `exchange-spot`）。
3. **行情服务消费事件**  
   `SpotTradeConsumer` 监听主题并反序列化，再投递到 Disruptor。
4. **行情聚合与推送**  
   `MarketTradeEventHandler` 串行处理成交：更新 Ticker/Kline/Depth，然后通过 WebSocket 广播。

### WebSocket 订阅与推送示例

**订阅示例**
- 单交易对：`ws://localhost:19090/ws?symbol=BTC_USDT`
- 全部交易对：`ws://localhost:19090/ws`（等价 `symbol=ALL`）

**推送消息结构**
```json
{
  "type": "trade|ticker|depth|kline",
  "time": "2026-01-22T15:10:30.123",
  "data": {}
}
```

---

## 链上充提与归集（答疑）

### 充值地址是真实存在的吗？能被监听吗？
- 充值地址是合法的 EOA 地址（由私钥生成），无需“先上链”才存在。
- 只要链上有转账到该地址，ERC20 的 `Transfer` 事件里会记录 `to` 地址，扫描日志即可监听。

### 为什么要归集？不归集可以吗？
- 可以不归集，但资金分散、每个地址都要 gas、运维成本高。
- 归集把充值地址资金汇总到热钱包，便于风控、出账与管理。

### 归集后用户余额怎么显示？
- 用户余额来自交易所内部账本。
- 充值确认时已记账，归集只是链上资金位置变化，不影响用户账本余额。
- 归集记录通常不对用户展示（可选）。

### 提币从哪个地址转出？
- 从交易所热钱包转出，不从用户充值地址转出。
- 充值地址用于收款，热钱包用于统一出账。

### 充币/提币成功如何确认？
- 充币：扫描 `Transfer` 事件 + 确认数。
- 提币：发送交易后轮询回执 + 确认数。
- 生产通常是“订阅 + 定时补扫”的混合模式。

### 为什么不直接打到热钱包？
- ERC20 转账没有 memo/备注字段，链上只看到 `from/to/amount`。
- 所有人打到同一地址，链上无法区分是谁的充值。
- 分配独立地址可以准确归属用户。

### “ERC20 没有 memo”是什么意思？
- 转账数据不包含“备注”，只有 `from/to/amount`。
- 所以无法通过链上信息判断“这笔属于哪个用户”，只能依赖地址区分。

## OTC 法币交易（答疑）

### 买方付款后卖方不放币怎么办？
- 下单时平台已冻结卖方资产。
- 买方申诉后平台仲裁，可直接放币或退款。

### 平台如何“强制放币”？
- 在内部账本里扣减卖方冻结余额，增加买方可用余额。
- 不涉及链上强制转账。

### 平台怎么拿到币？
- 用户必须先充值到交易所，平台才有托管资产。
- OTC 只是账本内划转，不会链上转账。

## KYC（答疑）

### 用户信息怎么验证真假？
- 证件 OCR + 版式/校验码校验
- 人脸比对 + 活体检测
- 数据一致性与风控（IP/设备/黑名单）
- 必要时人工复核
- 注意：无法 100% 保证，只能综合判断。

### 有哪些第三方可对接？
- 国际：Onfido、Sumsub、Trulioo、Jumio、Veriff、Persona、IDnow、Mitek
- AML：ComplyAdvantage、World-Check、LexisNexis
- 链上 AML：Chainalysis、TRM Labs、Elliptic
- 国内：阿里云/腾讯云/百度云/旷视/商汤等

### KYC 的过程是怎样的？涉及哪些第三方技术？
- 流程：资料提交 → OCR/证件校验 → 人脸比对/活体检测 → AML/制裁名单筛查 → 人工复核 → 通过/拒绝
- 技术与服务：证件 OCR、版式/校验码校验、人脸识别/活体检测、名单筛查、风控设备指纹/地理/IP 风险
- 对接形式：SDK + REST API + Webhook 回调

## 钱包私钥安全（答疑）

### 热钱包/冷钱包私钥怎么保存？
- 生产建议：对接 KMS/HSM 或 Vault，私钥不落明文库，只暴露签名能力
- 常见服务：AWS KMS/CloudHSM、Azure Key Vault、GCP KMS、HashiCorp Vault
- 冷钱包：离线存储 + 多签/MPC + 人工审批
- 演示环境：可用加密存储，但不建议明文

**成交推送**
```json
{
  "type": "trade",
  "time": "2026-01-22T15:10:30.123",
  "data": {
    "symbol": "BTC_USDT",
    "price": 30000,
    "quantity": 0.0001,
    "takerSide": "BUY",
    "tradeTime": "2026-01-22T15:10:30.120"
  }
}
```

**Ticker 推送**
```json
{
  "type": "ticker",
  "time": "2026-01-22T15:10:30.124",
  "data": {
    "symbol": "BTC_USDT",
    "lastPrice": 30000,
    "volume": 1.2345,
    "lastTradeTime": "2026-01-22T15:10:30.120"
  }
}
```

**深度推送**
```json
{
  "type": "depth",
  "time": "2026-01-22T15:10:30.125",
  "data": {
    "symbol": "BTC_USDT",
    "updateTime": "2026-01-22T15:10:30.124",
    "bids": [[30000, 0.5], [29999, 1.2]],
    "asks": [[30001, 0.4], [30002, 0.8]]
  }
}
```

**K 线推送**
```json
{
  "type": "kline",
  "time": "2026-01-22T15:10:30.126",
  "data": {
    "interval": "1m",
    "kline": {
      "symbol": "BTC_USDT",
      "startTime": 1769065800,
      "endTime": 1769065860,
      "open": 30000,
      "high": 30010,
      "low": 29990,
      "close": 30005,
      "volume": 2.345
    }
  }
}
```
