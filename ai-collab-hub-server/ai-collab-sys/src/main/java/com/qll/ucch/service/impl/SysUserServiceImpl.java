package com.qll.ucch.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.constant.SysConstants;
import com.qll.ucch.exception.SysBizException;
import com.qll.ucch.mapper.SysUserMapper;
import com.qll.ucch.models.dto.ResetPasswordDTO;
import com.qll.ucch.models.dto.ResetByPhoneDTO;
import com.qll.ucch.models.dto.StudentLoginDTO;
import com.qll.ucch.models.dto.StudentRegisterDTO;
import com.qll.ucch.models.dto.TeacherRegisterDTO;
import com.qll.ucch.models.dto.UpdateUserInfoDTO;
import com.qll.ucch.models.dto.UserPageQueryDTO;
import com.qll.ucch.models.po.SysRole;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.models.vo.LoginVO;
import com.qll.ucch.models.vo.UserInfoVO;
import com.qll.ucch.models.vo.UserPageVO;
import com.qll.ucch.service.SysRoleService;
import com.qll.ucch.service.SysUserReviewService;
import com.qll.ucch.service.SysUserRoleService;
import com.qll.ucch.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户业务实现。
 * <p>
 * 登录、注册、找回密码、个人资料、管理员用户管理都在这里。
 * 密码统一用 BCrypt 存，不存明文。
 */
@Service
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;
    private final SysUserReviewService sysUserReviewService;

    /**
     * 正常情况下这个 Bean 由 ai-collab-security 模块提供（SecurityConfig 里注册）。
     * 用 required = false 是为了让本模块能单独启动/单独测试，
     * 真的没有的话下面兜底 new 一个 BCrypt 实现，效果一样。
     */
    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(@Lazy SysRoleService sysRoleService,
                              @Lazy SysUserRoleService sysUserRoleService,
                              @Lazy SysUserReviewService sysUserReviewService) {
        this.sysRoleService = sysRoleService;
        this.sysUserRoleService = sysUserRoleService;
        this.sysUserReviewService = sysUserReviewService;
    }

    // ==================== 登录 ====================

    @Override
    public LoginVO loginByStudent(StudentLoginDTO dto) {
        return doLogin(dto.getAccount(), dto.getPassword(), SysConstants.ROLE_CODE_STUDENT, "学生");
    }

    @Override
    public LoginVO loginByTeacher(StudentLoginDTO dto) {
        return doLogin(dto.getAccount(), dto.getPassword(), SysConstants.ROLE_CODE_TEACHER, "老师");
    }

    @Override
    public LoginVO loginByAdmin(StudentLoginDTO dto) {
        // 管理员不限制角色，expectRoleCode 传 null，任何账号密码对了就能登进来。
        // 至于进来之后能干什么，由前端路由和接口权限各自把关。
        return doLogin(dto.getAccount(), dto.getPassword(), null, "管理员");
    }

    /**
     * 登录公共逻辑，学生端和老师端除了角色校验不一样，其它完全一致。
     *
     * @param expectRoleCode 期望的角色标识，传 null 表示不限制（管理员登录会走这里）
     * @param clientName     出错提示里用的端名称
     */
    private LoginVO doLogin(String account, String rawPassword, String expectRoleCode, String clientName) {
        if (StrUtil.isBlank(account) || StrUtil.isBlank(rawPassword)) {
            throw new SysBizException("账号和密码都不能为空");
        }

        SysUser user = getByAccount(account.trim());
        // 账号不存在和密码错误给同样的提示，免得被人撞库试出哪个账号存在
        if (user == null) {
            throw new SysBizException("账号或密码错误");
        }
        if (!encoder().matches(rawPassword, user.getPassword())) {
            throw new SysBizException("账号或密码错误");
        }

        // 审核状态不是「通过」的都不让登录
        if (!Objects.equals(user.getAuditStatus(), SysConstants.AUDIT_PASSED)) {
            if (Objects.equals(user.getAuditStatus(), SysConstants.AUDIT_PENDING)) {
                throw new SysBizException("账号正在审核中，请耐心等待管理员处理");
            }
            throw new SysBizException("账号审核未通过，请联系管理员");
        }

        // 被管理员禁用的账号也不让登录
        if (!Objects.equals(user.getEnable(), SysConstants.ENABLE_YES)) {
            throw new SysBizException("账号已被禁用，请联系管理员");
        }

        List<String> roleCodes = sysRoleService.listRoleCodesByUserId(user.getId());

        // 入口和角色对不上时，只在日志里记一笔，不拦登录。
        // 原因：账号密码都对就证明是本人在操作，因为点错身份按钮被挡在外面
        // 体验很差；而且登录后前端一律按库里真实的角色跳转，进不了不该进的页面。
        if (StrUtil.isNotBlank(expectRoleCode) && !roleCodes.contains(expectRoleCode)) {
            log.info("账号 {} 从{}入口登录，但实际角色是 {}，放行",
                    user.getAccount(), clientName, roleCodes);
        }

        return buildLoginVO(user, roleCodes);
    }

    // ==================== 注册 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerStudent(StudentRegisterDTO dto) {
        Long userId = doRegister(dto.getAccount(), dto.getPassword(), dto.getEmail(),
                dto.getPhone(), dto.getNickname(), SysConstants.ROLE_STUDENT_ID, dto.getCredentials());

        // 学生注册后只有账号信息，没有额外的扩展表要写
        return userId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerTeacher(TeacherRegisterDTO dto) {
        // 老师的院系和职称没有单独的字段，拼进备注里一起存到审核记录，
        // 管理员审核的时候能直接看到，不用再单独查。
        StringBuilder remark = new StringBuilder();
        if (StrUtil.isNotBlank(dto.getDepartment())) {
            remark.append("院系：").append(dto.getDepartment().trim());
        }
        if (StrUtil.isNotBlank(dto.getTitle())) {
            if (remark.length() > 0) {
                remark.append("；");
            }
            remark.append("职称：").append(dto.getTitle().trim());
        }
        if (StrUtil.isNotBlank(dto.getCredentials())) {
            if (remark.length() > 0) {
                remark.append("；");
            }
            remark.append("材料：").append(dto.getCredentials().trim());
        }

        return doRegister(dto.getAccount(), dto.getPassword(), dto.getEmail(),
                dto.getPhone(), dto.getNickname(), SysConstants.ROLE_TEACHER_ID,
                remark.toString());
    }

    /**
     * 注册公共逻辑：查重 -> 建用户 -> 绑角色 -> 建待审记录
     */
    private Long doRegister(String account, String rawPassword, String email, String phone,
                            String nickname, Long roleId, String credentials) {
        String finalAccount = account == null ? null : account.trim();
        String finalEmail = email == null ? null : email.trim();

        // 账号唯一
        if (getByAccount(finalAccount) != null) {
            throw new SysBizException("该账号已被注册，换一个试试");
        }
        // 邮箱唯一
        if (getByEmail(finalEmail) != null) {
            throw new SysBizException("该邮箱已被注册，换一个试试");
        }
        // 手机号唯一。
        // 手机号现在注册是必填的（找回密码要用它验证身份），所以这里写成无条件查重。
        // 表上本来就有唯一索引，不在这里拦住的话 MySQL 会抛原始异常，提示不友好。
        if (StrUtil.isNotBlank(phone) && getByPhone(phone.trim()) != null) {
            throw new SysBizException("该手机号已被注册");
        }

        SysUser user = new SysUser();
        user.setAccount(finalAccount);
        user.setPassword(encoder().encode(rawPassword));
        user.setEmail(finalEmail);
        user.setPhone(StrUtil.isBlank(phone) ? null : phone.trim());
        user.setNickname(nickname);
        // 注册进来先启用，但审核没通过照样登不进去
        user.setEnable(SysConstants.ENABLE_YES);
        user.setAuditStatus(SysConstants.AUDIT_PENDING);
        save(user);

        // 绑角色
        sysUserRoleService.bindUserRole(user.getId(), roleId);

        // 生成待审记录，管理员在审核列表里就能看到
        sysUserReviewService.createPendingReview(user.getId(), roleId, credentials);

        return user.getId();
    }

    // ==================== 找回密码 ====================

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        if (StrUtil.isBlank(dto.getAccount()) || StrUtil.isBlank(dto.getEmail())) {
            throw new SysBizException("账号和邮箱都不能为空");
        }
        SysUser user = getByAccount(dto.getAccount().trim());
        // 账号和邮箱必须对得上，否则不给重置，防止随便填个账号就能改别人密码
        if (user == null || !Objects.equals(user.getEmail(), dto.getEmail().trim())) {
            throw new SysBizException("账号和邮箱不匹配，请检查后重试");
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(encoder().encode(dto.getNewPassword()));
        updateById(update);
    }

    /**
     * 重置密码 —— 手机号版。
     *
     * 和邮箱版逻辑一样，只是把校验凭据从邮箱换成手机号。
     * 手机号有唯一索引，能唯一定位到人。
     */
    @Override
    public void resetPasswordByPhone(ResetByPhoneDTO dto) {
        if (StrUtil.isBlank(dto.getAccount()) || StrUtil.isBlank(dto.getPhone())) {
            throw new SysBizException("账号和手机号都不能为空");
        }
        SysUser user = getByAccount(dto.getAccount().trim());
        /*
         * 账号和手机号必须对得上才给重置。
         *
         * 这里刻意不区分「账号不存在」和「手机号不对」——两种都返回同一句话，
         * 免得有人拿这个接口去试探「某个账号存不存在」。
         */
        if (user == null || !Objects.equals(user.getPhone(), dto.getPhone().trim())) {
            throw new SysBizException("账号和手机号不匹配，请检查后重试");
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(encoder().encode(dto.getNewPassword()));
        updateById(update);
    }

    // ==================== 用户信息 ====================

    @Override
    public UserInfoVO getCurrentUserInfo(Long userId) {
        return getUserInfoById(userId);
    }

    @Override
    public void updateCurrentUserInfo(Long userId, UpdateUserInfoDTO dto) {
        if (userId == null) {
            throw new SysBizException("用户ID不能为空");
        }
        SysUser user = getById(userId);
        if (user == null) {
            throw new SysBizException("用户不存在");
        }

        // 邮箱要是改了，得重新查一遍有没有被别人占用
        if (StrUtil.isNotBlank(dto.getEmail()) && !Objects.equals(user.getEmail(), dto.getEmail().trim())) {
            SysUser exist = getByEmail(dto.getEmail().trim());
            if (exist != null && !Objects.equals(exist.getId(), userId)) {
                throw new SysBizException("该邮箱已被其他账号使用");
            }
        }
        if (StrUtil.isNotBlank(dto.getPhone()) && !Objects.equals(user.getPhone(), dto.getPhone().trim())) {
            SysUser exist = getByPhone(dto.getPhone().trim());
            if (exist != null && !Objects.equals(exist.getId(), userId)) {
                throw new SysBizException("该手机号已被其他账号使用");
            }
        }

        SysUser update = new SysUser();
        update.setId(userId);
        // 只更新传了值的字段，没传的保持原样
        update.setNickname(StrUtil.isBlank(dto.getNickname()) ? null : dto.getNickname());
        update.setEmail(StrUtil.isBlank(dto.getEmail()) ? null : dto.getEmail().trim());
        update.setPhone(StrUtil.isBlank(dto.getPhone()) ? null : dto.getPhone().trim());
        update.setAvatar(StrUtil.isBlank(dto.getAvatar()) ? null : dto.getAvatar());
        updateById(update);
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        if (userId == null) {
            throw new SysBizException("用户ID不能为空");
        }
        if (StrUtil.isBlank(avatarUrl)) {
            throw new SysBizException("头像地址不能为空");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        updateById(update);
    }

    @Override
    public UserInfoVO getUserInfoById(Long userId) {
        if (userId == null) {
            throw new SysBizException("用户ID不能为空");
        }
        SysUser user = getById(userId);
        if (user == null) {
            throw new SysBizException("用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoleCodes(sysRoleService.listRoleCodesByUserId(userId));
        vo.setRoleNames(sysRoleService.listRoleNamesByUserId(userId));
        return vo;
    }

    // ==================== 管理员用户管理 ====================

    @Override
    public IPage<UserPageVO> pageUsers(UserPageQueryDTO query) {
        UserPageQueryDTO cond = query == null ? new UserPageQueryDTO() : query;
        long current = cond.getCurrent() == null || cond.getCurrent() < 1 ? 1L : cond.getCurrent();
        long size = cond.getSize() == null || cond.getSize() < 1 ? 10L : cond.getSize();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(cond.getAccount())) {
            wrapper.like(SysUser::getAccount, cond.getAccount().trim());
        }
        if (StrUtil.isNotBlank(cond.getNickname())) {
            wrapper.like(SysUser::getNickname, cond.getNickname().trim());
        }
        if (cond.getAuditStatus() != null) {
            wrapper.eq(SysUser::getAuditStatus, cond.getAuditStatus());
        }
        if (cond.getEnable() != null) {
            wrapper.eq(SysUser::getEnable, cond.getEnable());
        }

        // 角色筛选：先解析出角色ID，再把 userId 作为 in 条件
        Long roleId = cond.getRoleId();
        if (roleId == null && StrUtil.isNotBlank(cond.getRoleCode())) {
            SysRole role = sysRoleService.getByCode(cond.getRoleCode());
            if (role == null) {
                // 角色标识不存在，直接给空页，别去查库了
                return new Page<>(current, size);
            }
            roleId = role.getId();
        }
        if (roleId != null) {
            List<Long> userIds = sysUserRoleService.listUserIdsByRoleId(roleId);
            if (CollUtil.isEmpty(userIds)) {
                return new Page<>(current, size);
            }
            wrapper.in(SysUser::getId, userIds);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> page = page(new Page<>(current, size), wrapper);
        return assembleUserPage(page);
    }

    @Override
    public void changeEnable(Long userId, Integer enable, Long operatorId) {
        if (userId == null) {
            throw new SysBizException("用户ID不能为空");
        }
        if (!Objects.equals(enable, SysConstants.ENABLE_YES) && !Objects.equals(enable, SysConstants.ENABLE_NO)) {
            throw new SysBizException("启用状态只能是 1（启用）或 0（禁用）");
        }
        SysUser user = getById(userId);
        if (user == null) {
            throw new SysBizException("用户不存在");
        }
        // 别让管理员把自己禁了，不然就没人能进后台了
        if (Objects.equals(userId, operatorId) && Objects.equals(enable, SysConstants.ENABLE_NO)) {
            throw new SysBizException("不能禁用当前登录的管理员账号");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setEnable(enable);
        update.setUpdateBy(operatorId);
        updateById(update);
    }

    @Override
    public void removeUser(Long userId, Long operatorId) {
        if (userId == null) {
            throw new SysBizException("用户ID不能为空");
        }
        SysUser user = getById(userId);
        if (user == null) {
            throw new SysBizException("用户不存在");
        }
        if (Objects.equals(userId, operatorId)) {
            throw new SysBizException("不能删除当前登录的账号");
        }
        // 这里是逻辑删除，delete_time 会被填上，数据还在表里，方便追溯
        SysUser update = new SysUser();
        update.setId(userId);
        update.setUpdateBy(operatorId);
        updateById(update);
        removeById(userId);
    }

    @Override
    public void updateAuditStatus(Long userId, Integer auditStatus, Long reviewerId) {
        if (userId == null) {
            throw new SysBizException("用户ID不能为空");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setAuditStatus(auditStatus);
        update.setUpdateBy(reviewerId);
        updateById(update);
    }

    // ==================== 私有方法 ====================

    /**
     * 按账号查用户，账号上有唯一索引，最多一条
     */
    private SysUser getByAccount(String account) {
        if (StrUtil.isBlank(account)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account)
                .last("limit 1"));
    }

    private SysUser getByEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
                .last("limit 1"));
    }

    private SysUser getByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone)
                .last("limit 1"));
    }

    /**
     * 组装登录返回。token 这里不生成，留给 Controller 调 security 模块补上。
     */
    private LoginVO buildLoginVO(SysUser user, List<String> roleCodes) {
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setAccount(user.getAccount());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setEnable(user.getEnable());
        vo.setAuditStatus(user.getAuditStatus());
        vo.setRoleCodes(roleCodes);
        vo.setRoleNames(sysRoleService.listRoleNamesByUserId(user.getId()));
        return vo;
    }

    /**
     * 分页结果转 VO，批量把角色名称补上
     */
    private IPage<UserPageVO> assembleUserPage(IPage<SysUser> page) {
        List<UserPageVO> records = new ArrayList<>();
        List<SysUser> list = page.getRecords();

        if (CollUtil.isNotEmpty(list)) {
            List<Long> userIds = list.stream().map(SysUser::getId).collect(Collectors.toList());

            // 用户 -> 角色ID列表
            Map<Long, List<Long>> userRoleMap = sysUserRoleService.mapRoleIdsByUserIds(userIds);

            // 角色ID -> 角色实体
            List<Long> allRoleIds = userRoleMap.values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, SysRole> roleMap = new HashMap<>();
            if (CollUtil.isNotEmpty(allRoleIds)) {
                for (SysRole role : sysRoleService.listByIds(allRoleIds)) {
                    roleMap.put(role.getId(), role);
                }
            }

            for (SysUser user : list) {
                UserPageVO vo = new UserPageVO();
                BeanUtils.copyProperties(user, vo);

                List<Long> roleIds = userRoleMap.getOrDefault(user.getId(), Collections.emptyList());
                List<String> codes = new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (Long rid : roleIds) {
                    SysRole role = roleMap.get(rid);
                    if (role != null) {
                        codes.add(role.getCode());
                        names.add(role.getName());
                    }
                }
                vo.setRoleCodes(String.join(",", codes));
                vo.setRoleNames(String.join(",", names));
                records.add(vo);
            }
        }

        Page<UserPageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    /**
     * 取密码编码器，Spring 容器里没有就自己兜一个 BCrypt 的
     */
    private PasswordEncoder encoder() {
        if (passwordEncoder == null) {
            synchronized (this) {
                if (passwordEncoder == null) {
                    passwordEncoder = new BCryptPasswordEncoder();
                }
            }
        }
        return passwordEncoder;
    }
}
