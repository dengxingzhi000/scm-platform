package scm.logistics.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 运单表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_waybill")
@Schema(description = "运单表")
public class TmsWaybill implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("waybill_no")
    private String waybillNo;

    @TableField("carrier_id")
    private String carrierId;

    @TableField("carrier_name")
    private String carrierName;

    @TableField("order_id")
    private String orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("sender_name")
    private String senderName;

    @TableField("sender_phone")
    private String senderPhone;

    @TableField("sender_address")
    private String senderAddress;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_address")
    private String receiverAddress;

    @TableField("goods_name")
    private String goodsName;

    @TableField("goods_weight")
    private BigDecimal goodsWeight;

    @TableField("goods_volume")
    private BigDecimal goodsVolume;

    @TableField("goods_value")
    private BigDecimal goodsValue;

    @TableField("freight_amount")
    private BigDecimal freightAmount;

    @TableField("insurance_amount")
    private BigDecimal insuranceAmount;

    @Schema(description = "状态:0-待揽件,1-已揽件,2-运输中,3-派送中,4-已签收,5-异常,6-退回")
    @TableField("status")
    private Integer status;

    @TableField("estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @TableField("actual_delivery")
    private LocalDateTime actualDelivery;

    @TableField("courier_id")
    private String courierId;

    @TableField("courier_name")
    private String courierName;

    @TableField("courier_phone")
    private String courierPhone;

    @Schema(description = "签收类型:1-本人签收,2-代签,3-快递柜")
    @TableField("sign_type")
    private Integer signType;

    @TableField("sign_person")
    private String signPerson;

    @TableField("sign_time")
    private LocalDateTime signTime;

    @TableField("sign_image")
    private String signImage;

    @TableField("exception_type")
    private String exceptionType;

    @TableField("exception_reason")
    private String exceptionReason;

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
