package com.iexchange.user.controller;

import com.iexchange.common.response.R;
import com.iexchange.user.dto.DemoSeataRequest;
import com.iexchange.user.dto.DemoSeataResponse;
import com.iexchange.user.service.DemoSeataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seata 极简演示接口。
 */
@Tag(name = "Seata 演示", description = "全局事务极简演示接口")
@RestController
@RequestMapping("/api/demo/seata")
@Validated
public class DemoSeataController {

    private final DemoSeataService demoSeataService;

    public DemoSeataController(DemoSeataService demoSeataService) {
        this.demoSeataService = demoSeataService;
    }

    /**
     * 执行 Seata 全局事务演示。
     */
    @Operation(summary = "执行全局事务", description = "演示 Seata 全局事务回滚与提交")
    @PostMapping("/execute")
    public R<DemoSeataResponse> execute(@Valid @RequestBody DemoSeataRequest request) {
        try {
            DemoSeataResponse response = demoSeataService.execute(request);
            return R.ok("执行成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            return R.fail("执行失败：" + ex.getMessage());
        }
    }
}
