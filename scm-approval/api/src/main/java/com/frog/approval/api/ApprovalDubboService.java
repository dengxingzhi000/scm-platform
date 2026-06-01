package com.frog.approval.api;

import com.frog.approval.api.dto.ApprovalVO;
import com.frog.approval.api.request.ApprovalRequest;

/**
 * 审批服务 Dubbo 接口
 *
 * <p>提供审批提交、审批通过/驳回、状态查询等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface ApprovalDubboService {

    /**
     * 提交审批
     *
     * @param request 审批请求
     * @return 审批信息
     */
    ApprovalVO submitApproval(ApprovalRequest request);

    /**
     * 审批通过
     *
     * @param approvalId 审批 ID
     * @param userId 审批人 ID
     */
    void approve(Long approvalId, Long userId);

    /**
     * 审批驳回
     *
     * @param approvalId 审批 ID
     * @param userId 审批人 ID
     * @param reason 驳回原因
     */
    void reject(Long approvalId, Long userId, String reason);

    /**
     * 查询审批状态
     *
     * @param approvalId 审批 ID
     * @return 审批信息，不存在时返回 null
     */
    ApprovalVO getApprovalStatus(Long approvalId);
}
