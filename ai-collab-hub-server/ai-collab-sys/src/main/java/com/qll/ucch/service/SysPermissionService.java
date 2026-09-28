package com.qll.ucch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.po.SysPermission;

import java.util.List;

/**
 * 权限业务接口
 */
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 按权限标识查权限
     */
    SysPermission getByCode(String code);

    /**
     * 查某个用户最终拥有的权限标识列表（用户 -> 角色 -> 权限）
     */
    List<String> listPermissionCodesByUserId(Long userId);
}
