package com.qll.ucch.openapi.sys;

import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.OperationLogQueryDTO;
import com.qll.ucch.models.vo.OperationLogVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口（新增功能）。
 * <p>
 * 只有管理员能看。列表支持关键词、模块、日期区间过滤。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/sys/log")
@RequiredArgsConstructor
@Tag(name = "操作日志")
public class SysLogController {

    private final SysOperationLogService operationLogService;

    @GetMapping("/list")
    @Operation(summary = "操作日志列表")
    public Result<PageResult<OperationLogVO>> list(@Valid OperationLogQueryDTO query) {
        // 这个接口只在管理后台用，路由守卫已经在角色维度挡过一道，
        // 服务端再确认一次，防止有人直接调接口
        if (!SecurityContext.isAdmin()) {
            return Result.forbidden("只有管理员能查看操作日志");
        }
        return Result.success(PageResult.of(operationLogService.pageLogs(query)));
    }
}
