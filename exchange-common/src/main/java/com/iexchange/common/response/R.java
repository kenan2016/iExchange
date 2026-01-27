package com.iexchange.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

/**
 * 统一返回对象（示例）。
 *
 * @param <T> 数据类型
 */
@Schema(name = "R", description = "统一返回对象")
@Data
public class R<T> {

    /**
     * 成功码。
     */
    public static final int SUCCESS_CODE = 0;

    /**
     * 默认失败码。
     */
    public static final int ERROR_CODE = 500;

    /**
     * 状态码。
     */
    @Schema(description = "状态码，0 表示成功")
    private int code;

    /**
     * 提示信息。
     */
    @Schema(description = "提示信息")
    private String message;

    /**
     * 返回数据。
     */
    @Schema(description = "业务数据")
    private T data;

    /**
     * 时间戳（毫秒）。
     */
    @Schema(description = "响应时间戳（毫秒）")
    private long timestamp;

    public static <T> R<T> ok(T data) {
        return ok("OK", data);
    }

    public static <T> R<T> ok(String message, T data) {
        R<T> result = new R<>();
        result.code = SUCCESS_CODE;
        result.message = message == null ? "OK" : message;
        result.data = data;
        result.timestamp = Instant.now().toEpochMilli();
        return result;
    }

    public static <T> R<T> fail(String message) {
        return fail(ERROR_CODE, message);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> result = new R<>();
        result.code = code;
        result.message = message == null ? "失败" : message;
        result.data = null;
        result.timestamp = Instant.now().toEpochMilli();
        return result;
    }

    /**
     * 根据业务结果快速包装（示例）。
     */
    public static <T> R<T> of(boolean success, String message, T data) {
        if (success) {
            return ok(message, data);
        }
        return fail(message);
    }
}
