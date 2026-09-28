package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.BlogArticleInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章主表 Mapper。
 * 复杂查询全部用 Wrapper 在 Service 层组装，这里只保留 MyBatis-Plus 基础能力。
 *
 * @author qll
 */
@Mapper
public interface BlogArticleInfoMapper extends BaseMapper<BlogArticleInfo> {
}
