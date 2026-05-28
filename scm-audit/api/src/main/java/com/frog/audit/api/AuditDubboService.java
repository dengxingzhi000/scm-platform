package com.frog.audit.api;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计服务 Dubbo 接口
 *
 * <p>提供操作日志记录、日志查询等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface AuditDubboService {

    /**
     * 记录操作日志
     *
     * @param request 审计日志请求
     */
    void logOperation(AuditLogRequest request);

    /**
     * 查询审计日志
     *
     * @param request 查询请求
     * @return 查询结果
     */
    AuditQueryResult queryAuditLogs(AuditQueryRequest request);

    /**
     * 审计日志请求
     */
    class AuditLogRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private String module;
        private String operation;
        private Long operatorId;
        private String operatorName;
        private String ipAddress;
        private String method;
        private String params;
        private Integer result;
        private String errorMsg;

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public Long getOperatorId() {
            return operatorId;
        }

        public void setOperatorId(Long operatorId) {
            this.operatorId = operatorId;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getParams() {
            return params;
        }

        public void setParams(String params) {
            this.params = params;
        }

        public Integer getResult() {
            return result;
        }

        public void setResult(Integer result) {
            this.result = result;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }
    }

    /**
     * 审计日志查询请求
     */
    class AuditQueryRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private String module;
        private Long operatorId;
        private Integer result;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int page;
        private int size;

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public Long getOperatorId() {
            return operatorId;
        }

        public void setOperatorId(Long operatorId) {
            this.operatorId = operatorId;
        }

        public Integer getResult() {
            return result;
        }

        public void setResult(Integer result) {
            this.result = result;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    /**
     * 审计日志 VO
     */
    class AuditLogVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String module;
        private String operation;
        private Long operatorId;
        private String operatorName;
        private String ipAddress;
        private String method;
        private Integer result;
        private String errorMsg;
        private LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public Long getOperatorId() {
            return operatorId;
        }

        public void setOperatorId(Long operatorId) {
            this.operatorId = operatorId;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Integer getResult() {
            return result;
        }

        public void setResult(Integer result) {
            this.result = result;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 审计日志查询结果
     */
    class AuditQueryResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<AuditLogVO> items;
        private long total;
        private int page;
        private int size;

        public List<AuditLogVO> getItems() {
            return items;
        }

        public void setItems(List<AuditLogVO> items) {
            this.items = items;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }
}
