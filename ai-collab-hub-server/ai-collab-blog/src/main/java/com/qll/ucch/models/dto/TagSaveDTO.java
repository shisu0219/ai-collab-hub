package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标签新增入参（管理员 / 发布者都能加）。
 *
 * @author qll
 */
@Data
@Schema(description = "标签新增入参")
public class TagSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签名称")
    @NotBlank(message = "标签名称不能为空")
    private String tagName;
}
