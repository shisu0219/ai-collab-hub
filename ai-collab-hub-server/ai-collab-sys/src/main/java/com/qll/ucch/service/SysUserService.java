package com.qll.ucch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.dto.ResetByPhoneDTO;
import com.qll.ucch.models.dto.ResetPasswordDTO;
import com.qll.ucch.models.dto.StudentLoginDTO;
import com.qll.ucch.models.dto.StudentRegisterDTO;
import com.qll.ucch.models.dto.TeacherRegisterDTO;
import com.qll.ucch.models.dto.UpdateUserInfoDTO;
import com.qll.ucch.models.dto.UserPageQueryDTO;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.models.vo.LoginVO;
import com.qll.ucch.models.vo.UserInfoVO;
import com.qll.ucch.models.vo.UserPageVO;

/**
 * 用户业务接口。
 * 覆盖登录、注册、找回密码、个人资料、管理员用户管理这几块。
 */
public interface SysUserService extends IService<SysUser> {

    // ---------------- 登录 ----------------

    /**
     * 学生端登录
     *
     * @param dto 账号 + 密码
     * @return 登录结果（含 token 占位，由 Controller 填充真实 token）
     */
    LoginVO loginByStudent(StudentLoginDTO dto);

    /**
     * 管理员登录。
     * <p>
     * 管理员不区分入口，所以不校验角色，只要能通过密码和审核就能进。
     * 登录后前端按 roleCode 判断跳管理后台还是合作平台。
     *
     * @param dto 登录入参（复用学生登录的 DTO，字段一样：账号 + 密码）
     * @return 登录结果
     */
    LoginVO loginByAdmin(StudentLoginDTO dto);

    /**
     * 老师登录。
     * 老师是校内角色，走的入口和学生类似，只是角色校验不一样。
     *
     * @param dto 登录入参（账号 + 密码）
     * @return 登录结果
     */
    LoginVO loginByTeacher(StudentLoginDTO dto);

    // ---------------- 注册 ----------------

    /**
     * 学生注册。注册后 audit_status=0，同时生成一条待审的 sys_user_review 记录。
     *
     * @return 新建用户的ID
     */
    Long registerStudent(StudentRegisterDTO dto);

    /**
     * 老师注册。
     * 老师不需要工商信息，但要把院系和职称存下来备查。
     *
     * @param dto 注册入参
     * @return 新用户ID
     */
    Long registerTeacher(TeacherRegisterDTO dto);

    // ---------------- 找回密码 ----------------

    /**
     * 重置密码。校验「账号 + 邮箱」是否匹配，匹配上才允许改。
     */
    void resetPassword(ResetPasswordDTO dto);

    /**
     * 重置密码 —— 手机号版。
     * <p>
     * 「账号 + 手机号」双重校验，两个都对上才允许重置。
     * 手机号在注册时是必填的，所以每个账号都能用这个方式找回。
     */
    void resetPasswordByPhone(ResetByPhoneDTO dto);

    // ---------------- 用户信息 ----------------

    /**
     * 查询当前登录用户信息
     *
     * @param userId 当前登录用户ID（由 Controller 从 token 里取出来传进来）
     */
    UserInfoVO getCurrentUserInfo(Long userId);

    /**
     * 修改当前登录用户信息（昵称/邮箱/手机号/头像）
     */
    void updateCurrentUserInfo(Long userId, UpdateUserInfoDTO dto);

    /**
     * 上传（其实只是保存）头像地址
     *
     * @param avatarUrl 文件上传后拿到的访问地址
     */
    void updateAvatar(Long userId, String avatarUrl);

    /**
     * 按ID查用户信息，主要用于查看他人主页
     */
    UserInfoVO getUserInfoById(Long userId);

    // ---------------- 管理员用户管理 ----------------

    /**
     * 分页查询用户列表，支持账号/昵称/审核状态/启用状态/角色筛选
     */
    IPage<UserPageVO> pageUsers(UserPageQueryDTO query);

    /**
     * 启用 / 禁用用户
     *
     * @param enable     1启用 0禁用
     * @param operatorId 操作人（管理员）ID，记到 update_by 上
     */
    void changeEnable(Long userId, Integer enable, Long operatorId);

    /**
     * 逻辑删除用户（管理员用，学生毕业/老师离职时清理）
     */
    void removeUser(Long userId, Long operatorId);

    // ---------------- 供审核 service 复用 ----------------

    /**
     * 把审核结果落到 sys_user.audit_status 上，同时记一下 update_by
     */
    void updateAuditStatus(Long userId, Integer auditStatus, Long reviewerId);
}
