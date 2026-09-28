package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper。
 * 只用 MyBatis-Plus 自带的增删改查，没写自定义 SQL。
 *
 * @author 人工智能学院双创平台
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
