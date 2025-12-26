package scm.warehouse.domain.entity;

import java.math.BigDecimal;
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
 * 波次拣货表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_wave_picking")
@Schema(description = "波次拣货表")
public class WmsWavePicking implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("wave_no")
    private String waveNo;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("outbound_ids")
    private String outboundIds;

    @TableField("order_count")
    private Integer orderCount;

    @TableField("total_items")
    private Integer totalItems;

    @TableField("picking_path")
    private String pickingPath;

    @TableField("total_distance")
    private Integer totalDistance;

    @Schema(description = "路径优化率")
    @TableField("optimization_rate")
    private BigDecimal optimizationRate;

    @Schema(description = "状态:0-待拣货,1-拣货中,2-已完成,3-已取消")
    @TableField("status")
    private Integer status;

    @TableField("picker_id")
    private String pickerId;

    @TableField("picker_name")
    private String pickerName;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("remark")
    private String remark;


}
