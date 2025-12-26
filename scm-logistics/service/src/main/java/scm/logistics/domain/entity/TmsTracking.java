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
 * 物流轨迹表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_tracking")
@Schema(description = "物流轨迹表")
public class TmsTracking implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("waybill_id")
    private String waybillId;

    @TableField("waybill_no")
    private String waybillNo;

    @TableField("track_time")
    private LocalDateTime trackTime;

    @TableField("location")
    private String location;

    @TableField("description")
    private String description;

    @TableField("longitude")
    private BigDecimal longitude;

    @TableField("latitude")
    private BigDecimal latitude;

    @TableField("track_status")
    private String trackStatus;

    @TableField("operator")
    private String operator;

    @Schema(description = "来源:API-接口推送,MANUAL-手动录入")
    @TableField("source")
    private String source;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("extra_data")
    private String extraData;

}
