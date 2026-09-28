package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.BlogArticleReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章审核记录 Mapper。
 *
 * @author qll
 */
@Mapper
public interface BlogArticleReviewMapper extends BaseMapper<BlogArticleReview> {
}
