package com.qll.ucch.openapi.blog;

import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.TagSaveDTO;
import com.qll.ucch.models.po.BlogArticleTag;
import com.qll.ucch.service.IBlogDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典维护接口（管理员）。
 * 类型 / 状态 / 进度这三张表是初始化脚本写死的，一般不让人动，
 * 所以这里只开放标签的增删——实际用起来标签是最常变的。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/root/blog")
@RequiredArgsConstructor
@Tag(name = "字典维护（管理员）")
public class BlogRootController {

    private final IBlogDictService blogDictService;

    @PostMapping("/tag")
    @Operation(summary = "新增标签")
    public Result<BlogArticleTag> addTag(@Valid @RequestBody TagSaveDTO dto) {
        BlogArticleTag tag = blogDictService.addTag(dto.getTagName());
        return Result.success("标签已添加", tag);
    }

    @DeleteMapping("/tag/{id}")
    @Operation(summary = "删除标签")
    public Result<Void> deleteTag(@PathVariable("id") Long id) {
        blogDictService.deleteTag(id);
        return Result.success("标签已删除", null);
    }
}
