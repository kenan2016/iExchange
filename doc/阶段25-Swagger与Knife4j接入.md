# 阶段 25：Swagger 与 Knife4j 接入

## 目标
- 为各业务服务接入 OpenAPI 文档
- 使用 Knife4j 作为 Swagger UI 展示界面
- 方便时在线查看接口与请求参数

## 依赖接入
在以下服务模块新增依赖（版本由父工程统一管理）：
- `exchange-user`
- `exchange-wallet`
- `exchange-spot`
- `exchange-contract`
- `exchange-market`

依赖坐标：
```xml
<dependency>
  <groupId>com.github.xiaoymin</groupId>
  <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
  <version>${knife4j.version}</version>
</dependency>
```

## 配置说明
各服务 `application.yml` 增加配置：
```yaml
springdoc:
  api-docs:
    enabled: true # 开启 OpenAPI 文档
  swagger-ui:
    enabled: false # 关闭默认 Swagger UI（使用 Knife4j）

knife4j:
  enable: true # 启用 Knife4j UI
  setting:
    language: zh_cn # 中文界面
```

## 接口注解说明（用于完善文档）
- `@Tag`：接口分组与描述（写在 Controller 上）
- `@Operation`：接口摘要与说明（写在方法上）
- `@Parameter`：查询参数/路径参数说明
- `@Schema`：请求与响应字段说明

## 访问地址
启动对应服务后，使用以下地址访问 UI：
- 用户服务：`http://localhost:18081/doc.html`
- 钱包服务：`http://localhost:18082/doc.html`
- 现货服务：`http://localhost:18083/doc.html`
- 合约服务：`http://localhost:18084/doc.html`
- 行情服务：`http://localhost:18085/doc.html`

OpenAPI 原始文档地址：
- `http://localhost:{port}/v3/api-docs`

## 说明
- 请求参数注释已补齐，便于 UI 中查看字段含义
- 生产环境建议关闭 API 文档与 UI
