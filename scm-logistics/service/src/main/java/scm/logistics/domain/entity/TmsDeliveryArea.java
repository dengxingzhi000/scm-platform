package scm.logistics.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 配送区域表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_delivery_area")
@Schema(description = "配送区域表")
public class TmsDeliveryArea implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("area_code")
    private String areaCode;

    @TableField("area_name")
    private String areaName;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("districts")
    private String districts;

    @TableField("carrier_id")
    private String carrierId;

    @Schema(description = "标准配送时长（小时）")
    @TableField("standard_duration")
    private Integer standardDuration;

    @TableField("delivery_type")
    private String deliveryType;

    @TableField("base_freight")
    private BigDecimal baseFreight;

    @TableField("additional_freight")
    private BigDecimal additionalFreight;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;

}
