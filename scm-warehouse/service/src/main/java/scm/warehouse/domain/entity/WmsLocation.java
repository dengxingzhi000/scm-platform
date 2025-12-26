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
 * 库位表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_location")
@Schema(description = "库位表")
public class WmsLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("location_code")
    private String locationCode;

    @TableField("zone")
    private String zone;

    @TableField("shelf")
    private String shelf;

    @TableField("layer")
    private String layer;

    @TableField("position")
    private String position;

    @Schema(description = "库位类型:1-普通,2-冷藏,3-冷冻,4-危险品")
    @TableField("location_type")
    private Integer locationType;

    @TableField("max_capacity")
    private Integer maxCapacity;

    @TableField("current_capacity")
    private Integer currentCapacity;

    @Schema(description = "状态:0-锁定,1-可用,2-维护中")
    @TableField("status")
    private Integer status;

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
