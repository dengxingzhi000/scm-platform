package com.frog.supplier.api;

import java.io.Serializable;
import java.util.List;

/**
 * 供应商服务 Dubbo 接口
 *
 * <p>提供供应商查询、评估等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface SupplierDubboService {

    /**
     * 根据 ID 查询供应商
     *
     * @param id 供应商 ID
     * @return 供应商信息，不存在时返回 null
     */
    SupplierVO getSupplierById(Long id);

    /**
     * 查询所有启用的供应商
     *
     * @return 供应商列表
     */
    List<SupplierVO> listActiveSuppliers();

    /**
     * 评估供应商
     *
     * @param supplierId 供应商 ID
     * @param request 评估请求
     * @return 评估结果
     */
    EvaluationResult evaluateSupplier(Long supplierId, EvaluationRequest request);

    /**
     * 供应商信息
     */
    class SupplierVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String name;
        private String code;
        private String contactPerson;
        private String contactPhone;
        private String email;
        private String address;
        private Integer status;
        private Integer level;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getContactPerson() {
            return contactPerson;
        }

        public void setContactPerson(String contactPerson) {
            this.contactPerson = contactPerson;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }
    }

    /**
     * 供应商评估请求
     */
    class EvaluationRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer qualityScore;
        private Integer deliveryScore;
        private Integer serviceScore;
        private String comment;
        private Long evaluatorId;

        public Integer getQualityScore() {
            return qualityScore;
        }

        public void setQualityScore(Integer qualityScore) {
            this.qualityScore = qualityScore;
        }

        public Integer getDeliveryScore() {
            return deliveryScore;
        }

        public void setDeliveryScore(Integer deliveryScore) {
            this.deliveryScore = deliveryScore;
        }

        public Integer getServiceScore() {
            return serviceScore;
        }

        public void setServiceScore(Integer serviceScore) {
            this.serviceScore = serviceScore;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Long getEvaluatorId() {
            return evaluatorId;
        }

        public void setEvaluatorId(Long evaluatorId) {
            this.evaluatorId = evaluatorId;
        }
    }

    /**
     * 评估结果
     */
    class EvaluationResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long evaluationId;
        private Integer totalScore;
        private Integer level;
        private String message;

        public Long getEvaluationId() {
            return evaluationId;
        }

        public void setEvaluationId(Long evaluationId) {
            this.evaluationId = evaluationId;
        }

        public Integer getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(Integer totalScore) {
            this.totalScore = totalScore;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
