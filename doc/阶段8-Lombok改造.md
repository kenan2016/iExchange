# 阶段 8：Lombok 改造（去掉手写 get/set）

## 目标
- 统一使用 Lombok 生成 getter/setter
- 移除项目中的手写 get/set
- 保持功能不变，代码更简洁

## 前置条件
- JDK 21
- IDE 安装 Lombok 插件（建议）

## 步骤 1：添加 Lombok 依赖
已在根 `pom.xml` 中添加：
- `org.projectlombok:lombok`
- `maven-compiler-plugin` 的 `annotationProcessorPaths`

说明：编译期生成 getter/setter，不影响运行时逻辑。

## 步骤 2：替换手写 getter/setter 与构造方法
改造范围（示例）：
- `exchange-user`、`exchange-wallet`、`exchange-spot`、`exchange-contract`、`exchange-market`、`exchange-api`
- DTO、Entity、Document、Model、Enum

使用方式（示例）：
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userId;
    private String username;
}
```

枚举类型示例：
```java
@Getter
public enum SpotOrderType {
    LIMIT("LIMIT"),
    MARKET("MARKET");

    private final String code;
}
```

说明：
- 简单 DTO/Entity 使用 `@Data + @NoArgsConstructor + @AllArgsConstructor` 生成构造方法
- 涉及初始化逻辑的类（如 K 线桶）改为使用全参构造，并在调用处补齐初始化参数

## 步骤 3：验证编译
```bash
mvn -f pom.xml -pl exchange-user -am -DskipTests clean package
```

如 IDE 报红：请确认 Lombok 插件已安装并启用。
