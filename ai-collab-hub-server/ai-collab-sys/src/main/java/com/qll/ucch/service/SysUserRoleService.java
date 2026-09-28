package com.qll.ucch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.po.SysUserRole;

import java.util.List;
import java.util.Map;

/**
 * 用户角色关联业务接口
 */
public interface SysUserRoleService extends IService<SysUserRole> {

    /**
     * 给用户绑定角色。已经绑过就不重复插。
     */
    void bindUserRole(Long userId, Long roleId);

    /**
     * 解绑用户的全部角色（一般用不上，改角色的时候会先清再绑）
     */
    void unbindAllRoles(Long userId);

    /**
     * 查用户拥有的角色ID列表
     */
    List<Long> listRoleIdsByUserId(Long userId);

    /**
     * 批量查「用户ID -> 角色ID列表」，列表页装配角色信息时避免 N+1 查询
     */
    Map<Long, List<Long>> mapRoleIdsByUserIds(List<Long> userIds);

    /**
     * 按角色ID捞用户ID列表，管理员按角色筛选用户时用
     */
    List<Long> listUserIdsByRoleId(Long roleId);
}
