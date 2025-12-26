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
 * 出库单表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_outbound")
@Schema(description = "出库单表")
public class WmsOutbound implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("outbound_no")
    private String outboundNo;

    @TableField("warehouse_id")
    private String warehouseId;

    @Schema(description = "出库类型:1-销售出库,2-调拨出库,3-报损出库,4-其他出库")
    @TableField("outbound_type")
    private Integer outboundType;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private String sourceId;

    @TableField("source_no")
    private String sourceNo;

    @Schema(description = "优先级:1-普通,2-紧急,3-特急")
    @TableField("priority")
    private Integer priority;

    @Schema(description = "状态:0-待拣货,1-拣货中,2-已拣货,3-已出库,4-已取消")
    @TableField("status")
    private Integer status;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("picked_quantity")
    private Integer pickedQuantity;

    @Schema(description = "拣货路径优化（JSONB）")
    @TableField("picking_path")
    private String pickingPath;

    @TableField("total_distance")
    private Integer totalDistance;

    @TableField("expected_at")
    private LocalDateTime expectedAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("picker_id")
    private String pickerId;

    @TableField("picker_name")
    private String pickerName;

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
