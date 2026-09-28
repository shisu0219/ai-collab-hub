package com.qll.ucch.constance;

/**
 * 全局通用常量。
 * 这里放的都是满项目到处在用的东西，改之前先全局搜一遍。
 *
 * @author 人工智能学院双创平台
 */
public interface CommonConst {

    /** 逻辑删除：未删除 */
    Integer NOT_DELETED = 0;

    /** 逻辑删除：已删除 */
    Integer DELETED = 1;

    /** 通用启用 */
    Integer ENABLE = 1;

    /** 通用禁用 */
    Integer DISABLE = 0;

    /** 审核状态：待审核 */
    Integer AUDIT_PENDING = 0;

    /** 审核状态：已通过 */
    Integer AUDIT_PASS = 1;

    /** 审核状态：已拒绝 */
    Integer AUDIT_REJECT = 2;

    /** 默认页码 */
    long DEFAULT_PAGE_NUMBER = 1L;

    /** 默认每页条数 */
    long DEFAULT_PAGE_SIZE = 10L;

    /** 每页最大条数，防止前端传个 99999 把库拖死 */
    long MAX_PAGE_SIZE = 100L;

    /** 角色ID：系统管理员 */
    Long ROLE_ADMIN = 1L;

    /** 角色ID：学生 */
    Long ROLE_STUDENT = 2L;

    /** 角色标识：系统管理员 */
    String ROLE_CODE_ADMIN = "ADMIN";

    /** 角色标识：学生 */
    String ROLE_CODE_STUDENT = "STUDENT";

    /** 对接状态：待处理 */
    Integer COLLAB_PENDING = 0;

    /** 对接状态：已通过 */
    Integer COLLAB_PASSED = 1;

    /** 对接状态：洽谈中 */
    Integer COLLAB_TALKING = 2;

    /** 对接状态：已合作 */
    Integer COLLAB_COOPERATED = 3;

    /** 对接状态：已结束 */
    Integer COLLAB_FINISHED = 4;

    /** 对接状态：已拒绝 */
    Integer COLLAB_REJECTED = 5;

    /** 消息类型：审核结果 */
    Integer MSG_AUDIT = 1;

    /** 消息类型：收到申请 */
    Integer MSG_APPLICATION = 2;

    /** 消息类型：系统通知 */
    Integer MSG_SYSTEM = 3;

    /** 消息类型：会话消息 */
    Integer MSG_CHAT = 4;
}
