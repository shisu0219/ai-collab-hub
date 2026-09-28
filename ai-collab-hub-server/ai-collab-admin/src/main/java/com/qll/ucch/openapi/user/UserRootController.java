package com.qll.ucch.openapi.user;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.UserPageQueryDTO;
import com.qll.ucch.models.vo.UserPageVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

/**
 * 用户管理接口（管理员）。
 * 跟 /user/review 的区别：那边是审材料，这边是管已经存在的账号（列表 / 启停）。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/root/user")
@RequiredArgsConstructor
@Tag(name = "用户管理（管理员）")
public class UserRootController {

    private final SysUserService sysUserService;

    @GetMapping("/list")
    @Operation(summary = "用户列表")
    public Result<PageResult<UserPageVO>> userList(@Valid UserPageQueryDTO query) {
        return Result.success(PageResult.of(sysUserService.pageUsers(query)));
    }

    @PostMapping("/enable")
    @Operation(summary = "启用 / 禁用用户")
    public Result<Void> changeEnable(@Valid @RequestBody EnableRequest request) {
        Long operatorId = SecurityContext.requireUserId();
        if (request.getUserId().equals(operatorId)) {
            throw new BusinessException("不能对自己的账号做这个操作");
        }
        sysUserService.changeEnable(request.getUserId(), request.getEnable(), operatorId);

        String tip = request.getEnable() == 1 ? "账号已启用" : "账号已禁用";
        log.info("管理员 {} {} 用户 {} 的账号", operatorId, request.getEnable() == 1 ? "启用" : "禁用",
                request.getUserId());
        return Result.success(tip, null);
    }

    /** 启用 / 禁用入参 */
    @Data
    public static class EnableRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotNull(message = "用户ID不能为空")
        private Long userId;

        /** 1 启用 0 禁用 */
        @NotNull(message = "请选择启用状态")
        private Integer enable;
    }
}
