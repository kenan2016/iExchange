# 阶段22-登录鉴权 JWT 改造

## 目标
- 登录服务签发 JWT，去掉服务端保存 Token 的依赖
- 网关统一校验 JWT，并透传用户身份

## 改造点
- 用户服务：登录成功后生成 JWT
- 网关服务：校验 JWT 有效性并注入 `X-User-Id`、`X-Username`
- 配置：新增 JWT 密钥与发行者

## 依赖调整
- 用户服务与网关服务新增 `jjwt` 依赖

## 配置示例
```yaml
jwt:
  secret: iexchange-demo-secret-please-change-32bytes # JWT 密钥
  issuer: iexchange # JWT 发行者
  expire-seconds: 7200 # 过期时间（秒，仅用户服务需要）
```
说明：用户服务与网关服务需保持 `jwt.secret` 与 `jwt.issuer` 一致。

## 核心实现说明
1. 用户服务 `TokenService` 使用 JJWT 生成 token
   - 载荷包含 `userId`、`username`
   - 设置 `issuer` 与 `expiration`
2. 网关 `AuthGlobalFilter` 调用 `JwtTokenService.parse` 校验
   - 失败直接返回 401
   - 成功透传 `X-User-Id`、`X-Username`

## 验证步骤
1. 调用 `/api/auth/login` 获取 `X-Token`
2. 访问非白名单接口时携带 `X-Token`
3. 观察网关是否透传 `X-User-Id` 与 `X-Username`

## 提示
- JWT 为无状态方案，避免集中式 Token 存储
- 若需要强制下线，可引入黑名单或刷新令牌机制
