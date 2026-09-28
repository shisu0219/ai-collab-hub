package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.BlogType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章类型字典 Mapper。
 *
 * @author qll
 */
@Mapper
public interface BlogTypeMapper extends BaseMapper<BlogType> {
}
