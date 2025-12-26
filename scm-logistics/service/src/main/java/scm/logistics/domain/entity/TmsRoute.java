package scm.logistics.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 配送路线表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_route")
@Schema(description = "配送路线表")
public class TmsRoute implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("route_no")
    private String routeNo;

    @TableField("delivery_date")
    private LocalDate deliveryDate;

    @TableField("courier_id")
    private String courierId;

    @TableField("courier_name")
    private String courierName;

    @TableField("vehicle_no")
    private String vehicleNo;

    @TableField("vehicle_type")
    private String vehicleType;

    @TableField("start_location")
    private String startLocation;

    @TableField("end_location")
    private String endLocation;

    @Schema(description = "途经点（JSONB）- 路径优化后的顺序")
    @TableField("waypoints")
    private String waypoints;

    @TableField("waybill_ids")
    private String waybillIds;

    @TableField("waybill_count")
    private Integer waybillCount;

    @TableField("total_distance")
    private BigDecimal totalDistance;

    @TableField("estimated_duration")
    private Integer estimatedDuration;

    @TableField("actual_duration")
    private Integer actualDuration;

    @Schema(description = "路径优化算法:TSP-旅行商,GA-遗传,ACO-蚁群")
    @TableField("optimization_algorithm")
    private String optimizationAlgorithm;

    @TableField("optimization_score")
    private BigDecimal optimizationScore;

    @Schema(description = "状态:0-规划中,1-配送中,2-已完成,3-已取消")
    @TableField("status")
    private Integer status;

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
