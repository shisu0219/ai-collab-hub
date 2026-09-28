package com.qll.ucch.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qll.ucch.mapper.BlogArticleStatusMapper;
import com.qll.ucch.mapper.BlogArticleTagMapper;
import com.qll.ucch.mapper.BlogProgressMapper;
import com.qll.ucch.mapper.BlogTypeMapper;
import com.qll.ucch.models.common.BusinessException;
import com.qll.ucch.models.enums.CollabProgressEnum;
import com.qll.ucch.models.po.BlogArticleStatus;
import com.qll.ucch.models.po.BlogArticleTag;
import com.qll.ucch.models.po.BlogProgress;
import com.qll.ucch.models.po.BlogType;
import com.qll.ucch.models.vo.DictVO;
import com.qll.ucch.service.IBlogDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 静态字典实现。
 * 这几张表数据量都很小（就几条），不做缓存，直接查库，简单省事。
 *
 * @author qll
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogDictServiceImpl implements IBlogDictService {

    private final BlogTypeMapper blogTypeMapper;

    private final BlogArticleStatusMapper blogArticleStatusMapper;

    private final BlogProgressMapper blogProgressMapper;

    private final BlogArticleTagMapper blogArticleTagMapper;

    @Override
    public List<BlogType> listType() {
        LambdaQueryWrapper<BlogType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BlogType::getId);
        return blogTypeMapper.selectList(wrapper);
    }

    @Override
    public List<BlogArticleStatus> listStatus() {
        LambdaQueryWrapper<BlogArticleStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BlogArticleStatus::getId);
        return blogArticleStatusMapper.selectList(wrapper);
    }

    @Override
    public List<BlogProgress> listProgress(Long typeId) {
        LambdaQueryWrapper<BlogProgress> wrapper = new LambdaQueryWrapper<>();
        // typeId 为空就查全部，前端按需自己分组
        wrapper.eq(typeId != null, BlogProgress::getTypeId, typeId);
        wrapper.orderByAsc(BlogProgress::getId);
        return blogProgressMapper.selectList(wrapper);
    }

    @Override
    public List<BlogArticleTag> listTag(String tagName) {
        LambdaQueryWrapper<BlogArticleTag> wrapper = new LambdaQueryWrapper<>();
        // 支持按名字模糊找标签，方便前端做输入联想
        wrapper.like(StrUtil.isNotBlank(tagName), BlogArticleTag::getTagName, tagName);
        wrapper.orderByAsc(BlogArticleTag::getId);
        return blogArticleTagMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogArticleTag addTag(String tagName) {
        if (StrUtil.isBlank(tagName)) {
            throw new BusinessException("标签名称不能为空");
        }
        String name = tagName.trim();

        // 表上有 uk_tag_name 唯一索引，先查一把，重名就把已有的返回去，不报错更友好
        LambdaQueryWrapper<BlogArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogArticleTag::getTagName, name);
        BlogArticleTag exist = blogArticleTagMapper.selectOne(wrapper);
        if (exist != null) {
            return exist;
        }

        BlogArticleTag tag = new BlogArticleTag();
        tag.setTagName(name);
        blogArticleTagMapper.insert(tag);
        log.info("新增标签成功，tagId={}, tagName={}", tag.getId(), name);
        return tag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long tagId) {
        if (tagId == null) {
            throw new BusinessException("标签ID不能为空");
        }
        BlogArticleTag tag = blogArticleTagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在或已被删除");
        }
        blogArticleTagMapper.deleteById(tagId);
        log.info("删除标签成功，tagId={}", tagId);
    }

    @Override
    public DictVO getAllDict(Long typeId) {
        DictVO vo = new DictVO();

        vo.setTypeList(listType().stream().map(item -> {
            DictVO.TypeItem target = new DictVO.TypeItem();
            target.setId(item.getId());
            target.setName(item.getName());
            target.setDescription(item.getDescription());
            return target;
        }).collect(Collectors.toList()));

        vo.setStatusList(listStatus().stream().map(item -> {
            DictVO.StatusItem target = new DictVO.StatusItem();
            target.setId(item.getId());
            target.setName(item.getName());
            target.setDescription(item.getDescription());
            return target;
        }).collect(Collectors.toList()));

        vo.setProgressList(listProgress(typeId).stream().map(item -> {
            DictVO.ProgressItem target = new DictVO.ProgressItem();
            target.setId(item.getId());
            target.setName(item.getName());
            target.setTypeId(item.getTypeId());
            target.setDescription(item.getDescription());
            target.setNodeCount(item.getNodeCount());
            return target;
        }).collect(Collectors.toList()));

        vo.setTagList(listTag(null).stream().map(item -> {
            DictVO.TagItem target = new DictVO.TagItem();
            target.setId(item.getId());
            target.setTagName(item.getTagName());
            return target;
        }).collect(Collectors.toList()));

        // 对接进度是枚举造的字典，不走库表
        vo.setCollabProgressList(java.util.Arrays.stream(CollabProgressEnum.values()).map(item -> {
            DictVO.CollabProgressItem target = new DictVO.CollabProgressItem();
            target.setCode(item.getCode());
            target.setName(item.getName());
            return target;
        }).collect(Collectors.toList()));

        return vo;
    }
}
