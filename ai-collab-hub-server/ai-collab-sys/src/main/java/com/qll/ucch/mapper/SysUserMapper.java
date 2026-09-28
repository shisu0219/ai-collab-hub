package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
