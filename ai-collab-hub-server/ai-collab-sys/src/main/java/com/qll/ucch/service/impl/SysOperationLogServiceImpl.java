package com.qll.ucch.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.mapper.SysOperationLogMapper;
import com.qll.ucch.mapper.SysUserMapper;
import com.qll.ucch.models.dto.OperationLogQueryDTO;
import com.qll.ucch.models.po.SysOperationLog;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.models.vo.OperationLogVO;
import com.qll.ucch.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志实现。
 * <p>
 * 日志是「顺手记一下」的东西，写失败绝不能把主业务弄挂，
 * 所以这里所有落库操作都包了 try-catch，出错只打 warn。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl
        extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements SysOperationLogService {

    private final SysUserMapper sysUserMapper;

    @Override
    public void record(Long userId, String userAccount, String module, String action,
                       Long targetId, String detail, String ip) {
        try {
            SysOperationLog entity = new SysOperationLog();
            entity.setUserId(userId);
            entity.setUserAccount(userAccount);
            entity.setModule(module);
            entity.setAction(action);
            entity.setTargetId(targetId);
            entity.setDetail(detail);
            entity.setIp(ip);
            entity.setCreateTime(LocalDateTime.now());
            this.save(entity);
        } catch (Exception e) {
            // 日志写不进去不算业务失败，打个警告就行
            log.warn("写操作日志失败：module={}, action={}, err={}", module, action, e.getMessage());
        }
    }

    @Override
    public void record(Long userId, String userAccount, String module, String action,
                       Long targetId, String detail) {
        record(userId, userAccount, module, action, targetId, detail, currentIp());
    }

    @Override
    public Page<OperationLogVO> pageLogs(OperationLogQueryDTO query) {
        long pageNumber = (query.getPageNumber() == null || query.getPageNumber() < 1)
                ? 1L : query.getPageNumber();
        long pageSize = (query.getPageSize() == null || query.getPageSize() < 1)
                ? 10L : Math.min(query.getPageSize(), 100L);

        var wrapper = Wrappers.<SysOperationLog>lambdaQuery()
                .orderByDesc(SysOperationLog::getCreateTime);

        // 关键词：同时匹配操作人账号和操作动作
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysOperationLog::getUserAccount, kw)
                    .or().like(SysOperationLog::getAction, kw)
                    .or().like(SysOperationLog::getDetail, kw));
        }
        if (query.getModule() != null && !query.getModule().isBlank()) {
            wrapper.eq(SysOperationLog::getModule, query.getModule().trim());
        }
        if (query.getUserId() != null) {
            wrapper.eq(SysOperationLog::getUserId, query.getUserId());
        }
        // 日期区间：开始日期取 00:00:00，结束日期取 23:59:59，这样含当天
        if (query.getStartDate() != null && !query.getStartDate().isBlank()) {
            LocalDateTime start = LocalDate.parse(query.getStartDate().trim()).atStartOfDay();
            wrapper.ge(SysOperationLog::getCreateTime, start);
        }
        if (query.getEndDate() != null && !query.getEndDate().isBlank()) {
            LocalDateTime end = LocalDate.parse(query.getEndDate().trim()).atTime(LocalTime.MAX);
            wrapper.le(SysOperationLog::getCreateTime, end);
        }

        Page<SysOperationLog> page = this.page(new Page<>(pageNumber, pageSize), wrapper);

        Page<OperationLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVoList(page.getRecords()));
        return result;
    }

    // ==================== 内部工具 ====================

    /** 批量转 VO，顺便把操作人昵称补上，避免逐条查库 */
    private List<OperationLogVO> toVoList(List<SysOperationLog> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集需要查昵称的用户ID
        List<Long> userIds = records.stream()
                .map(SysOperationLog::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectList(
                    Wrappers.<SysUser>lambdaQuery()
                            .in(SysUser::getId, userIds)
                            .select(SysUser::getId, SysUser::getNickname)
            );
            for (SysUser u : users) {
                nicknameMap.put(u.getId(), u.getNickname());
            }
        }

        return records.stream().map(e -> {
            OperationLogVO vo = new OperationLogVO();
            vo.setId(e.getId());
            vo.setUserId(e.getUserId());
            vo.setUserAccount(e.getUserAccount());
            vo.setNickname(nicknameMap.get(e.getUserId()));
            vo.setModule(e.getModule());
            vo.setAction(e.getAction());
            vo.setTargetId(e.getTargetId());
            vo.setDetail(e.getDetail());
            vo.setIp(e.getIp());
            vo.setCreateTime(e.getCreateTime());
            return vo;
        }).toList();
    }

    /** 从当前请求上下文取真实 IP，取不到返回 null */
    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            // 有反向代理时优先取 X-Forwarded-For 的第一段
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
