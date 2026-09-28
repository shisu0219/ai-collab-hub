package com.qll.ucch.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.mapper.SysUserRoleMapper;
import com.qll.ucch.models.po.SysUserRole;
import com.qll.ucch.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户角色关联业务实现。
 * 表里没有唯一索引，所以绑定前先自己查一遍，避免插出重复数据。
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Override
    public void bindUserRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return;
        }
        Long count = count(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
        if (count != null && count > 0) {
            return;
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        save(userRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindAllRoles(Long userId) {
        if (userId == null) {
            return;
        }
        remove(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<SysUserRole> list = list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        return list.stream()
                .map(SysUserRole::getRoleId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<Long>> mapRoleIdsByUserIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<SysUserRole> list = list(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds));
        Map<Long, List<Long>> result = new HashMap<>();
        for (SysUserRole userRole : list) {
            result.computeIfAbsent(userRole.getUserId(), k -> new ArrayList<>()).add(userRole.getRoleId());
        }
        return result;
    }

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        List<SysUserRole> list = list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
        Set<Long> userIds = list.stream()
                .map(SysUserRole::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        return new ArrayList<>(userIds);
    }
}
