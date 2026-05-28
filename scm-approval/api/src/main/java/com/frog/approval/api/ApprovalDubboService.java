package com.frog.approval.api;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    /**
     * 审批请求
     */
    class ApprovalRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private String businessType;
        private Long businessId;
        private Long applicantId;
        private String title;
        private String content;
        private String priority;

        public String getBusinessType() {
            return businessType;
        }

        public void setBusinessType(String businessType) {
            this.businessType = businessType;
        }

        public Long getBusinessId() {
            return businessId;
        }

        public void setBusinessId(Long businessId) {
            this.businessId = businessId;
        }

        public Long getApplicantId() {
            return applicantId;
        }

        public void setApplicantId(Long applicantId) {
            this.applicantId = applicantId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }

    /**
     * 审批信息
     */
    class ApprovalVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String approvalNo;
        private String businessType;
        private Long businessId;
        private Long applicantId;
        private String title;
        private String content;
        private String status;
        private Long approverId;
        private String rejectReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getApprovalNo() {
            return approvalNo;
        }

        public void setApprovalNo(String approvalNo) {
            this.approvalNo = approvalNo;
        }

        public String getBusinessType() {
            return businessType;
        }

        public void setBusinessType(String businessType) {
            this.businessType = businessType;
        }

        public Long getBusinessId() {
            return businessId;
        }

        public void setBusinessId(Long businessId) {
            this.businessId = businessId;
        }

        public Long getApplicantId() {
            return applicantId;
        }

        public void setApplicantId(Long applicantId) {
            this.applicantId = applicantId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getApproverId() {
            return approverId;
        }

        public void setApproverId(Long approverId) {
            this.approverId = approverId;
        }

        public String getRejectReason() {
            return rejectReason;
        }

        public void setRejectReason(String rejectReason) {
            this.rejectReason = rejectReason;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
