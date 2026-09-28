package com.qll.ucch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.po.SysRole;
import com.qll.ucch.models.vo.RoleVO;

import java.util.List;

/**
 * 角色业务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 按角色标识查角色，比如 STUDENT / TEACHER
     */
    SysRole getByCode(String code);

    /**
     * 查某个用户拥有的所有角色
     */
    List<SysRole> listByUserId(Long userId);

    /**
     * 查某个用户的角色标识列表，例如 ["STUDENT"]
     */
    List<String> listRoleCodesByUserId(Long userId);

    /**
     * 查某个用户的角色名称列表
     */
    List<String> listRoleNamesByUserId(Long userId);

    /**
     * 全部角色，给下拉框用
     */
    List<RoleVO> listAllRoleVO();
}
