package scm.purchase.domain.entity;

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
 * 询价单表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_rfq")
@Schema(description = "询价单表")
public class PurRfq implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String rfqNo;

    private String rfqTitle;

    private String requestId;

    private String requestNo;

    @Schema(description = "询价类型:1-公开询价,2-邀请询价,3-竞价采购")
    private Integer rfqType;

    @Schema(description = "询价方式:1-电话询价,2-邮件询价,3-平台询价")
    private Integer inquiryMethod;

    private LocalDateTime deadline;

    private String quotationRequirement;

    private String paymentTerms;

    private String deliveryTerms;

    @Schema(description = "状态:0-草稿,1-已发布,2-报价中,3-已截止,4-已关闭")
    private Integer status;

    private String initiatorId;

    private String initiatorName;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;


}
