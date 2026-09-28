package com.qll.ucch.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.mapper.SysPermissionMapper;
import com.qll.ucch.models.po.SysPermission;
import com.qll.ucch.service.SysPermissionService;
import com.qll.ucch.service.SysRolePermissionService;
import com.qll.ucch.service.SysUserRoleService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限业务实现。
 * 权限链路：用户 -> 角色 -> 权限，中间隔着两张关联表。
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    private final SysUserRoleService sysUserRoleService;
    private final SysRolePermissionService sysRolePermissionService;

    public SysPermissionServiceImpl(@Lazy SysUserRoleService sysUserRoleService,
                                    @Lazy SysRolePermissionService sysRolePermissionService) {
        this.sysUserRoleService = sysUserRoleService;
        this.sysRolePermissionService = sysRolePermissionService;
    }

    @Override
    public SysPermission getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, code.trim())
                .last("limit 1"));
    }

    @Override
    public List<String> listPermissionCodesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Long> roleIds = sysUserRoleService.listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = sysRolePermissionService.listPermissionIdsByRoleIds(roleIds);
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysPermission> permissions = listByIds(permissionIds);
        if (CollUtil.isEmpty(permissions)) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(SysPermission::getCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }
}
