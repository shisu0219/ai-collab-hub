package com.qll.ucch.openapi.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.po.BlogArticleInfo;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.service.IBlogArticleService;
import com.qll.ucch.service.IBlogRegistrationService;
import com.qll.ucch.service.SysUserRoleService;
import com.qll.ucch.service.SysUserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板统计接口（新增功能）。
 * <p>
 * 原来平台只在技术栈里装了 ECharts，没有任何统计页面，管理员看不到数据，
 * 团委要汇报只能去数据库里捞。这里把常用的几个数字和趋势都算出来，
 * 前端直接画图就行。
 * <p>
 * 数据量不大的情况下实时聚合就够用了，没必要上缓存。
 * 如果以后数据量上来了，可以往 sys_stat_snapshot 表里写快照。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/sys/stat")
@RequiredArgsConstructor
@Tag(name = "数据看板")
public class SysStatController {

    private final SysUserService sysUserService;

    private final SysUserRoleService sysUserRoleService;

    private final IBlogArticleService blogArticleService;

    private final IBlogRegistrationService blogRegistrationService;

    /** 角色ID：管理员 */
    private static final Long ROLE_ADMIN = 1L;

    /** 角色ID：学生 */
    private static final Long ROLE_STUDENT = 2L;

    /** 角色ID：老师 */
    private static final Long ROLE_TEACHER = 4L;

    /** 内容状态：待审核 */
    private static final Long STATUS_PENDING = 1L;

    /** 内容状态：已发布 */
    private static final Long STATUS_PUBLISHED = 2L;

    /**
     * 看板总览。
     * 首页那 6 个数字卡片 + 待办数量都用这一个接口，前端少发几次请求。
     */
    @GetMapping("/overview")
    @Operation(summary = "看板总览数据")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();

        // ---------- 用户 ----------
        long totalUser = sysUserService.count(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getAuditStatus, 1)
        );
        long studentCount = countUserByRole(ROLE_STUDENT);
        // 【本次修正】原来只统计学生和老师之外的角色，老师压根没算进来 ——
        // 现在改成按实际开放的三端（学生/老师/管理员）统计。
        long teacherCount = countUserByRole(ROLE_TEACHER);
        long adminCount = countUserByRole(ROLE_ADMIN);

        // 待审核账户数
        long pendingUserCount = sysUserService.count(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getAuditStatus, 0)
        );

        data.put("totalUser", totalUser);
        data.put("studentCount", studentCount);
        data.put("teacherCount", teacherCount);
        data.put("adminCount", adminCount);
        data.put("pendingUserCount", pendingUserCount);

        // ---------- 内容 ----------
        long totalArticle = blogArticleService.count();
        long publishedArticle = blogArticleService.count(
                Wrappers.<BlogArticleInfo>lambdaQuery().eq(BlogArticleInfo::getStatusId, STATUS_PUBLISHED)
        );
        long pendingArticleCount = blogArticleService.count(
                Wrappers.<BlogArticleInfo>lambdaQuery().eq(BlogArticleInfo::getStatusId, STATUS_PENDING)
        );

        data.put("totalArticle", totalArticle);
        data.put("publishedArticle", publishedArticle);
        data.put("pendingArticleCount", pendingArticleCount);

        // ---------- 对接 ----------
        long totalRegistration = blogRegistrationService.count();
        long passedRegistration = blogRegistrationService.count(
                Wrappers.<com.qll.ucch.models.po.BlogRegistration>lambdaQuery()
                        .eq(com.qll.ucch.models.po.BlogRegistration::getPass, 1)
        );

        data.put("totalRegistration", totalRegistration);
        data.put("passedRegistration", passedRegistration);

        // 对接成功率：通过的申请 / 全部申请，保留一位小数
        double rate = 0D;
        if (totalRegistration > 0) {
            rate = Math.round(passedRegistration * 1000.0 / totalRegistration) / 10.0;
        }
        data.put("collabRate", rate);

        return Result.success(data);
    }

    /**
     * 注册趋势，默认最近 30 天。
     * 返回 counts 数组，按日期从早到晚排，前端直接喂给折线图。
     */
    @GetMapping("/register-trend")
    @Operation(summary = "注册趋势")
    public Result<Map<String, Object>> registerTrend(@RequestParam(defaultValue = "30") Integer days) {
        int span = normalizeDays(days);
        LocalDate start = LocalDate.now().minusDays(span - 1L);

        // 一次性把这段时间的注册时间捞出来，在内存里按天分组，比循环查 30 次库快得多
        List<SysUser> users = sysUserService.list(
                Wrappers.<SysUser>lambdaQuery()
                        .ge(SysUser::getCreateTime, start.atStartOfDay())
                        .select(SysUser::getId, SysUser::getCreateTime)
        );

        Map<String, Long> dayMap = groupByDay(
                users.stream().map(SysUser::getCreateTime).toList()
        );

        return Result.success(buildTrendResult(span, dayMap));
    }

    /**
     * 内容发布趋势，默认最近 30 天。
     */
    @GetMapping("/article-trend")
    @Operation(summary = "内容发布趋势")
    public Result<Map<String, Object>> articleTrend(@RequestParam(defaultValue = "30") Integer days) {
        int span = normalizeDays(days);
        LocalDate start = LocalDate.now().minusDays(span - 1L);

        List<BlogArticleInfo> articles = blogArticleService.list(
                Wrappers.<BlogArticleInfo>lambdaQuery()
                        .ge(BlogArticleInfo::getCreateTime, start.atStartOfDay())
                        .select(BlogArticleInfo::getId, BlogArticleInfo::getCreateTime)
        );

        Map<String, Long> dayMap = groupByDay(
                articles.stream().map(BlogArticleInfo::getCreateTime).toList()
        );

        return Result.success(buildTrendResult(span, dayMap));
    }

    /**
     * 对接情况统计。
     * 按 status 分档：待处理、已通过、已拒绝、洽谈中及以后。
     */
    @GetMapping("/collab")
    @Operation(summary = "对接情况统计")
    public Result<Map<String, Object>> collab() {
        Map<String, Object> data = new HashMap<>();

        long pending = blogRegistrationService.count(
                Wrappers.<com.qll.ucch.models.po.BlogRegistration>lambdaQuery()
                        .isNull(com.qll.ucch.models.po.BlogRegistration::getPass)
        );
        long passed = blogRegistrationService.count(
                Wrappers.<com.qll.ucch.models.po.BlogRegistration>lambdaQuery()
                        .eq(com.qll.ucch.models.po.BlogRegistration::getPass, 1)
        );
        long rejected = blogRegistrationService.count(
                Wrappers.<com.qll.ucch.models.po.BlogRegistration>lambdaQuery()
                        .eq(com.qll.ucch.models.po.BlogRegistration::getPass, 0)
        );

        data.put("pending", pending);
        data.put("passed", passed);
        data.put("rejected", rejected);
        data.put("total", pending + passed + rejected);

        return Result.success(data);
    }

    // ==================== 内部工具 ====================

    /** 按角色ID统计用户数（已通过审核的） */
    private long countUserByRole(Long roleId) {
        List<Long> userIds = sysUserRoleService.listUserIdsByRoleId(roleId);
        if (userIds == null || userIds.isEmpty()) {
            return 0L;
        }
        return sysUserService.count(
                Wrappers.<SysUser>lambdaQuery()
                        .in(SysUser::getId, userIds)
                        .eq(SysUser::getAuditStatus, 1)
        );
    }

    /** 天数兜底：限制在 1~365，防止前端传个离谱的值 */
    private int normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return 30;
        }
        return Math.min(days, 365);
    }

    /** 把一堆时间戳按 yyyy-MM-dd 分组计数 */
    private Map<String, Long> groupByDay(List<LocalDateTime> times) {
        Map<String, Long> map = new HashMap<>();
        if (times == null) {
            return map;
        }
        for (LocalDateTime t : times) {
            if (t == null) {
                continue;
            }
            String key = t.toLocalDate().toString();
            map.merge(key, 1L, Long::sum);
        }
        return map;
    }

    /** 生成连续的日期轴 + 每天的数值，中间没有数据的天补 0（不然图表会断） */
    private Map<String, Object> buildTrendResult(int span, Map<String, Long> dayMap) {
        List<String> dates = new ArrayList<>(span);
        List<Long> counts = new ArrayList<>(span);

        LocalDate cursor = LocalDate.now().minusDays(span - 1L);
        for (int i = 0; i < span; i++) {
            String key = cursor.toString();
            dates.add(key);
            counts.add(dayMap.getOrDefault(key, 0L));
            cursor = cursor.plusDays(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return result;
    }
}
