package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.SysEmailConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮箱配置表 Mapper
 */
@Mapper
public interface SysEmailConfigMapper extends BaseMapper<SysEmailConfig> {
}
