package scm.product.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 商品属性模板表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prod_attribute_template")
@Schema(description = "商品属性模板表")
public class ProdAttributeTemplate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    @Schema(description = "模板 ID")
    private String id;


    @TableField("template_name")
    @Schema(description = "模板名称")
    private String templateName;

    @TableField("category_id")
    @Schema(description = "分类 ID")
    private String categoryId;

    @TableField("attributes")
    @Schema(description = "属性定义 JSON数组")
    private String attributes;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("create_by")
    @Schema(description = "创建人")
    private String createBy;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField("update_by")
    @Schema(description = "更新人")
    private String updateBy;

    @TableField("deleted")
    @Schema(description = "是否删除")
    private Boolean deleted;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

}
