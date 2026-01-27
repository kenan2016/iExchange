# 阶段29-ES订单检索与Canal同步（演示）

## 目标
- 使用 Easy-ES 写入 ES 索引
- 使用 Canal 监听 MySQL Binlog，同步订单到 ES
- 通过 ES 查询现货与合约订单

## 同步范围
- `spot_order` → 索引 `spot_order`
- `contract_order` → 索引 `contract_order`

## 环境准备（示意）
### ES
```bash
docker run -d --name iexchange-es -p 9200:9200 -e "discovery.type=single-node" elasticsearch:7.17.17
```

### Canal（示意）
```bash
docker run -d --name iexchange-canal -p 11111:11111 \
  -e canal.destinations=example \
  -e canal.instance.master.address=127.0.0.1:3306 \
  -e canal.instance.dbUsername=root \
  -e canal.instance.dbPassword=root \
  canal/canal-server:v1.1.7
```

> 注意：需开启 MySQL binlog 且权限正确。

## 配置
### exchange-spot
```yaml
es:
  host: http://127.0.0.1:9200
canal:
  enabled: true
  host: 127.0.0.1
  port: 11111
  destination: example
  filter: iexchange.spot_order
```

### exchange-contract
```yaml
es:
  host: http://127.0.0.1:9200
canal:
  enabled: true
  host: 127.0.0.1
  port: 11111
  destination: example
  filter: iexchange.contract_order
```

## 查询接口
### 现货订单（ES）
`GET /api/spot/order/search`

示例：
```
/api/spot/order/search?userId=1&symbol=BTC_USDT&status=FILLED&limit=50
```

### 合约订单（ES）
`GET /api/contract/order/search`

示例：
```
/api/contract/order/search?userId=1&symbol=BTCUSDT-PERP&status=FILLED&limit=50
```

### 通过 ES 查询的示例
```bash
curl "http://localhost:18083/api/spot/order/search?userId=1&symbol=BTC_USDT&status=NEW&limit=20"
```

## ES数据同步原理与过程（简述）
- 订单先落库到 MySQL，产生 binlog（行级变更事件）
- Canal Server 订阅 binlog，并将变更事件推送给 Canal Client
- 服务内的 Canal Client 拉取 Entry，解析出 row data（插入/更新/删除）
- 将 row data 映射为 ES 文档，执行 upsert/删除同步到对应索引
- 同步成功后提交 ack，失败时可重试或重新消费

## 说明
- ES 仅作为查询加速，交易主链路仍以 MySQL 为准
- Canal 为增量同步示意，生产需考虑断点、重放与容灾
