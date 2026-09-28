package com.qll.ucch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.po.SysRolePermission;

import java.util.List;

/**
 * 角色权限关联业务接口
 */
public interface SysRolePermissionService extends IService<SysRolePermission> {

    /**
     * 给角色分配权限（先清掉旧的再批量插）
     */
    void bindRolePermissions(Long roleId, List<Long> permissionIds);

    /**
     * 查角色拥有的权限ID列表
     */
    List<Long> listPermissionIdsByRoleId(Long roleId);

    /**
     * 查角色拥有的多个权限ID（多个角色一起去重合并）
     */
    List<Long> listPermissionIdsByRoleIds(List<Long> roleIds);
}
