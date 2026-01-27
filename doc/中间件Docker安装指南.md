# 中间件 Docker 安装指南（版）

## 目标
- 使用 Docker 一键启动项目依赖的中间件
- 统一端口、账号与持久化目录
- 提供按需启动与排查命令

## 前置条件
- 已安装 Docker（建议 Docker Desktop）
- 当前工作目录为项目根目录：`/Users/nanke/workSpace/iExchange`

## 推荐方式：Docker Compose 一键启动
项目已提供完整编排文件：`infra/docker-compose.yml`

启动全部中间件：
```bash
docker compose -f infra/docker-compose.yml up -d
```

按需启动单个服务：
```bash
docker compose -f infra/docker-compose.yml up -d mysql
docker compose -f infra/docker-compose.yml up -d redis
docker compose -f infra/docker-compose.yml up -d kafka
```

停止并删除容器（保留数据卷）：
```bash
docker compose -f infra/docker-compose.yml down
```

## 常用安装命令（单容器）
### Sentinel 控制台
```bash
docker run --name iexchange-sentinel \
  -d -p 8858:8858 \
  bladex/sentinel-dashboard:1.8.6
```

### Seata Server（DB 模式 + Nacos）
```bash
docker run --name iexchange-seata \
  -d -p 8091:8091 -p 7091:7091 \
  -e SEATA_IP=127.0.0.1 \
  -e SEATA_PORT=8091 \
  -e SEATA_CONFIG_NAME=file:/seata-server/resources/application.yml \
  -v /Users/nanke/workSpace/iExchange/infra/seata/application.yml:/seata-server/resources/application.yml \
  -v /Users/nanke/workSpace/iExchange/infra/data/seata:/seata-server/data \
  seataio/seata-server:1.7.1
```

说明：
- 如果 Nacos/MySQL 不是本机，请先修改 `infra/seata/application.yml`
- Seata DB 模式需要初始化 `infra/mysql/init-seata.sql`

## 中间件清单与端口
- MySQL：`3306`（账号/密码：`root/root`）
- MongoDB：`27017`
- Redis：`6379`
- Zookeeper：`2181`
- Kafka：`9092`
- Nacos：`8848`、`9848`
- Sentinel：`8858`
- Seata Server：`8091`（服务端口）、`7091`（控制台）

## 数据持久化目录
所有数据卷在 `infra/data/` 下：
- MySQL：`infra/data/mysql`
- MongoDB：`infra/data/mongo`
- Redis：`infra/data/redis`
- Nacos：`infra/data/nacos`
- Seata：`infra/data/seata`

## Seata 特别说明（DB 模式 + Nacos）
1) Seata 配置文件：`infra/seata/application.yml`  
2) 需要将 `infra/seata/seata-server.properties` 导入 Nacos  
   - DataId：`seata-server.properties`  
   - Group：`SEATA_GROUP`  
   - Namespace：`seata`（注意使用命名空间 ID）
3) Seata 数据库初始化：`infra/mysql/init-seata.sql`  
   - 包含 `global_table`/`branch_table`/`lock_table`/`distributed_lock`

如果你的 Nacos/MySQL 在本机，请把 `infra/seata/application.yml` 中的地址改为 `127.0.0.1`。

## 常用运维命令
查看容器：
```bash
docker ps
```

查看日志：
```bash
docker logs -f iexchange-mysql
docker logs -f iexchange-nacos
docker logs -f iexchange-seata
```

重启容器：
```bash
docker restart iexchange-seata
```

进入容器（排查用）：
```bash
docker exec -it iexchange-mysql bash
```

## 常见问题（提示）
- **MySQL 初始化脚本未执行**：容器已存在且数据已持久化时，初始化脚本不会重复执行，需要手动执行 SQL。  
- **Kafka 外部访问失败**：确认 `KAFKA_CFG_ADVERTISED_LISTENERS` 是否为当前主机 IP。  
- **Seata 报 dbType 为空**：说明 Nacos 中未导入 `seata-server.properties` 或命名空间不一致。
