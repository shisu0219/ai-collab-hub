package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.NoticeBoxInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息盒 Mapper。
 * <p>
 * 常规查询用 BaseMapper + LambdaQueryWrapper 就够，没有额外 XML。
 *
 * @author qll
 */
@Mapper
public interface NoticeBoxInfoMapper extends BaseMapper<NoticeBoxInfo> {
}
