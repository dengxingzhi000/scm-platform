package scm.warehouse.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 入库单明细表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_inbound_item")
@Schema(description = "入库单明细表")
public class WmsInboundItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("inbound_id")
    private String inboundId;

    @TableField("inbound_no")
    private String inboundNo;

    @TableField("sku_id")
    private String skuId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("location_id")
    private String locationId;

    @TableField("location_code")
    private String locationCode;

    @TableField("plan_quantity")
    private Integer planQuantity;

    @TableField("actual_quantity")
    private Integer actualQuantity;

    @Schema(description = "质量状态:1-合格,2-待检,3-不合格")
    @TableField("quality_status")
    private Integer qualityStatus;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("remark")
    private String remark;


}
