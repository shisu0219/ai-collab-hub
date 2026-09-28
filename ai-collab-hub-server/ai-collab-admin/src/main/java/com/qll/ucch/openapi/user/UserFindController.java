package com.qll.ucch.openapi.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户查找接口（所有登录用户都能用）。
 *
 * 【为什么单独开一个接口，而不是复用 /root/user/list】
 * /root 前缀是管理员专属的，学生和老师调不了。
 * 但组长邀请成员时必须在界面上搜人 —— 总不能让他手填用户ID。
 * 所以这里开一个轻量接口，只返回「选人」需要的几个字段，
 * 并且只放行已审核通过的账号，不暴露邮箱手机号这些敏感信息。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/user/find")
@RequiredArgsConstructor
@Tag(name = "用户查找")
public class UserFindController {

    private final SysUserService sysUserService;

    /**
     * 按账号或昵称模糊搜人。用于组长邀请成员时的下拉选择。
     *
     * @param keyword 账号或昵称关键字
     * @param roleCode 按角色过滤，可选：STUDENT / TEACHER
     */
    @GetMapping("/list")
    @Operation(summary = "按账号或昵称搜人（邀请成员用）")
    public Result<PageResult<Map<String, Object>>> find(@RequestParam(value = "keyword", required = false) String keyword,
                                                        @RequestParam(value = "roleCode", required = false) String roleCode,
                                                        @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize) {
        SecurityContext.requireUserId();
        if (!StringUtils.hasText(keyword)) {
            return Result.success(PageResult.of(new Page<>(pageNum, pageSize)));
        }
        Page<SysUser> page = new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50));
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(w -> w.like(SysUser::getAccount, keyword).or().like(SysUser::getNickname, keyword))
                // 只放已审核通过的账号，避免把待审的人拉进组
                .eq(SysUser::getAuditStatus, 1)
                .eq(SysUser::getEnable, 1)
                .orderByAsc(SysUser::getId);
        var result = sysUserService.page(page, wrapper);

        // 只回必要字段。邮箱手机号一律不给 —— 搜人不需要这些。
        List<Map<String, Object>> records = new ArrayList<>();
        for (SysUser u : result.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("account", u.getAccount());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar());
            records.add(m);
        }
        Page<Map<String, Object>> out = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        out.setRecords(records);
        return Result.success(PageResult.of(out));
    }
}
