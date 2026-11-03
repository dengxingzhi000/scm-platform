package com.scmcloud.common.dto.approval;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
/**
 *
 *
 * @author Deng
 * createData 2025/11/3 16:04
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ApprovalDTO {
    private UUID id;

    private UUID applicantId;

    private Integer approvalType;

    private UUID targetUserId;

    private String applyReason;

    private String businessJustification;

    private Integer approvalStatus;

    private LocalDateTime effectiveTime;

    private LocalDateTime expireTime;

    private LocalDateTime approvedTime;

    private String rejectReason;

    private List<UUID> roleIds;

    private List<UUID> permissionIds;
}
