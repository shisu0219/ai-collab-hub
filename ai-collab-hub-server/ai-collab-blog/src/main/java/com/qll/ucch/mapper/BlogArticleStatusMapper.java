package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.BlogArticleStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章状态字典 Mapper。
 *
 * @author qll
 */
@Mapper
public interface BlogArticleStatusMapper extends BaseMapper<BlogArticleStatus> {
}
