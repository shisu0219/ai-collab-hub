package com.qll.ucch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.dto.OperationLogQueryDTO;
import com.qll.ucch.models.po.SysOperationLog;
import com.qll.ucch.models.vo.OperationLogVO;

/**
 * 操作日志业务接口（新增功能）。
 * <p>
 * 记录方式有两种：
 * 1. 业务代码里显式调用 {@link #record}，适合关键操作（审核通过/拒绝）；
 * 2. AOP 切面自动记录，见 ai-collab-admin 的 OperationLogAspect。
 *
 * @author 人工智能学院双创平台
 */
public interface SysOperationLogService extends IService<SysOperationLog> {

    /**
     * 记一笔日志。
     * <p>
     * 注意：日志写失败不能影响主业务，所以实现里会自己吞掉异常只打 warn，
     * 调用方不用 try-catch。
     *
     * @param userId      操作人用户ID，可为空（系统自动触发时）
     * @param userAccount 操作人账号，可为空
     * @param module      业务模块
     * @param action      操作动作
     * @param targetId    操作对象ID，可为空
     * @param detail      详情
     * @param ip          操作IP
     */
    void record(Long userId, String userAccount, String module, String action,
                Long targetId, String detail, String ip);

    /**
     * 记一笔日志（不带 IP，内部会从请求上下文取）。
     */
    void record(Long userId, String userAccount, String module, String action,
                Long targetId, String detail);

    /**
     * 分页查询操作日志。
     * 支持按关键词（操作人账号/操作动作）、模块、操作人、日期区间过滤。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<OperationLogVO> pageLogs(OperationLogQueryDTO query);
}
