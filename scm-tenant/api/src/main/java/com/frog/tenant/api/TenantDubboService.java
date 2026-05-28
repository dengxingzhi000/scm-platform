package com.frog.tenant.api;

import java.io.Serializable;
import java.util.Map;

/**
 * 租户服务 Dubbo 接口
 *
 * <p>提供租户查询、租户配置、功能开关等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface TenantDubboService {

    /**
     * 根据租户 ID 查询租户信息
     *
     * @param tenantId 租户 ID
     * @return 租户信息，不存在时返回 null
     */
    TenantVO getTenantById(String tenantId);

    /**
     * 获取租户配置
     *
     * @param tenantId 租户 ID
     * @return 租户配置信息，不存在时返回 null
     */
    TenantConfigVO getTenantConfig(String tenantId);

    /**
     * 检查租户是否启用某功能
     *
     * @param tenantId 租户 ID
     * @param featureCode 功能编码
     * @return true-已启用，false-未启用
     */
    boolean checkFeatureEnabled(String tenantId, String featureCode);

    /**
     * 租户信息
     */
    class TenantVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String tenantId;
        private String tenantName;
        private String contactPerson;
        private String contactPhone;
        private String contactEmail;
        private Integer status;
        private String industry;
        private String region;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(String tenantName) {
            this.tenantName = tenantName;
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

        public String getContactEmail() {
            return contactEmail;
        }

        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getIndustry() {
            return industry;
        }

        public void setIndustry(String industry) {
            this.industry = industry;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    /**
     * 租户配置信息
     */
    class TenantConfigVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String tenantId;
        private String theme;
        private String language;
        private String timezone;
        private Integer maxUsers;
        private Map<String, String> features;
        private Map<String, String> extraConfig;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public Integer getMaxUsers() {
            return maxUsers;
        }

        public void setMaxUsers(Integer maxUsers) {
            this.maxUsers = maxUsers;
        }

        public Map<String, String> getFeatures() {
            return features;
        }

        public void setFeatures(Map<String, String> features) {
            this.features = features;
        }

        public Map<String, String> getExtraConfig() {
            return extraConfig;
        }

        public void setExtraConfig(Map<String, String> extraConfig) {
            this.extraConfig = extraConfig;
        }
    }
}
