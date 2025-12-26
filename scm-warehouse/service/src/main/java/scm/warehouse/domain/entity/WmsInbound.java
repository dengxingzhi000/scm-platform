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
 * 入库单表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_inbound")
@Schema(description = "入库单表")
public class WmsInbound implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("inbound_no")
    private String inboundNo;

    @TableField("warehouse_id")
    private String warehouseId;

    @Schema(description = "入库类型:1-采购入库,2-退货入库,3-调拨入库,4-其他入库")
    @TableField("inbound_type")
    private Integer inboundType;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private String sourceId;

    @TableField("source_no")
    private String sourceNo;

    @TableField("supplier_id")
    private String supplierId;

    @Schema(description = "状态:0-待入库,1-入库中,2-部分入库,3-已完成,4-已取消")
    @TableField("status")
    private Integer status;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("received_quantity")
    private Integer receivedQuantity;

    @TableField("expected_at")
    private LocalDateTime expectedAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

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
