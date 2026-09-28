package com.qll.ucch.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.mapper.SysRoleMapper;
import com.qll.ucch.models.po.SysRole;
import com.qll.ucch.models.po.SysUserRole;
import com.qll.ucch.models.vo.RoleVO;
import com.qll.ucch.service.SysRoleService;
import com.qll.ucch.service.SysUserRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色业务实现
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserRoleService sysUserRoleService;

    /**
     * 这里用 @Lazy 是因为 SysUserService 那边也要反过来用角色 service，
     * 不加会出现循环依赖，Spring Boot 2.6+ 默认不允许多次注入同一个 bean。
     */
    public SysRoleServiceImpl(@Lazy SysUserRoleService sysUserRoleService) {
        this.sysUserRoleService = sysUserRoleService;
    }

    @Override
    public SysRole getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code.trim())
                .last("limit 1"));
    }

    @Override
    public List<SysRole> listByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Long> roleIds = sysUserRoleService.listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return listByIds(roleIds);
    }

    @Override
    public List<String> listRoleCodesByUserId(Long userId) {
        return listByUserId(userId).stream()
                .map(SysRole::getCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listRoleNamesByUserId(Long userId) {
        return listByUserId(userId).stream()
                .map(SysRole::getName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoleVO> listAllRoleVO() {
        List<SysRole> roles = list(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        List<RoleVO> result = new ArrayList<>(roles.size());
        for (SysRole role : roles) {
            RoleVO vo = new RoleVO();
            BeanUtils.copyProperties(role, vo);
            result.add(vo);
        }
        return result;
    }
}
