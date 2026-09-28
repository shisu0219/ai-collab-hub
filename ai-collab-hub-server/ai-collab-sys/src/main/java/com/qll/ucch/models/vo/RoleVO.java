package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色简易出参，下拉框用
 */
@Data
@Schema(description = "角色信息")
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色标识符")
    private String code;

    @Schema(description = "角色描述")
    private String description;
}
