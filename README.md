# iExchange

iExchange 是一个演示的数字资产交易所后端项目，基于 Spring Boot、Spring Cloud Alibaba 与 Dubbo，覆盖用户、钱包、现货、合约、行情、网关等核心模块，并提供配套脚本与文档。

## 功能概览
- 用户与登录鉴权
- 钱包账户与流水
- 现货撮合与订单
- 永续合约与强平/计划委托
- 行情、K 线与推送
- 网关鉴权、限流与统一返回
- Seata 分布式事务与多中间件接入示例

## 模块结构
- `exchange-api`：公共接口与 DTO
- `exchange-common`：通用能力与基础组件
- `exchange-user`：用户服务
- `exchange-wallet`：钱包服务
- `exchange-spot`：现货撮合服务
- `exchange-contract`：合约服务
- `exchange-market`：行情服务
- `exchange-gateway`：API 网关

## 环境要求
- JDK 21
- Maven 3.9+
- Docker Desktop（用于启动依赖）

## 快速开始
1) 启动依赖（MySQL/MongoDB/Redis/Kafka/Nacos/Sentinel/Seata 等）  
```
chmod +x scripts/env-up.sh
./scripts/env-up.sh
```

2) 编译项目  
```
mvn -DskipTests package
```

3) 启动全部服务  
```
chmod +x scripts/start-all.sh
./scripts/start-all.sh
```

4) 停止服务/依赖  
```
chmod +x scripts/stop-all.sh
./scripts/stop-all.sh
chmod +x scripts/env-down.sh
./scripts/env-down.sh
```

## 服务端口
- gateway: `18080`
- user: `18081`
- wallet: `18082`
- spot: `18083`
- contract: `18084`
- market: `18085`

## 文档与示例
- 文档目录：`doc/`
- 中间件安装：`doc/中间件Docker安装指南.md`
- ES 订单检索与 Canal 同步：`doc/阶段29-ES订单检索与Canal同步.md`
- 常见问题：`doc/常见QA.md`

## 可选能力
- ES + Canal 订单检索与增量同步（示例实现，见 `doc/阶段29-ES订单检索与Canal同步.md`）

## 架构示意
```
                +-------------------+
                |  Web/APP/Client   |
                +---------+---------+
                          |
                          v
                   +------+------+
                   |  Gateway   |
                   +------+------+
                          |
        +-----------------+-----------------+
        |        |        |        |        |
        v        v        v        v        v
     User     Wallet     Spot   Contract   Market
        |        |        |        |        |
        +--------+--------+--------+--------+
                          |
        +-----------------+-----------------+
        |      中间件与基础设施              |
        | MySQL / Mongo / Redis / Kafka     |
        | Nacos / Sentinel / Seata          |
        +-----------------+-----------------+
                          |
                +---------+---------+
                | ES + Canal (可选) |
                +-------------------+
```

## License
当前仓库未包含 LICENSE 文件。开源前请补充许可证（例如 MIT/Apache-2.0），并在 README 中同步说明。

## 免责声明
本项目为学习与演示用途，不建议直接用于生产环境。
