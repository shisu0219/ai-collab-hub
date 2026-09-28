package com.qll.ucch.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.mapper.SysRolePermissionMapper;
import com.qll.ucch.models.po.SysRolePermission;
import com.qll.ucch.service.SysRolePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色权限关联业务实现
 */
@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>
        implements SysRolePermissionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindRolePermissions(Long roleId, List<Long> permissionIds) {
        if (roleId == null) {
            return;
        }
        // 先清后插，逻辑简单不出错，权限数量本来也不多
        remove(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (CollUtil.isEmpty(permissionIds)) {
            return;
        }
        List<SysRolePermission> list = new ArrayList<>();
        for (Long permissionId : new HashSet<>(permissionIds)) {
            if (permissionId == null) {
                continue;
            }
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            list.add(rp);
        }
        if (!list.isEmpty()) {
            saveBatch(list);
        }
    }

    @Override
    public List<Long> listPermissionIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listPermissionIdsByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<SysRolePermission> list = list(new LambdaQueryWrapper<SysRolePermission>()
                .in(SysRolePermission::getRoleId, roleIds));
        Set<Long> ids = list.stream()
                .map(SysRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new ArrayList<>(ids);
    }
}
