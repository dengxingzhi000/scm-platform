import type { MpPage } from "./page";

/** `com.scmcloud.tenant.domain.entity.Tenant`. */
export interface TenantDTO {
  id: string;
  tenantCode?: string | null;
  tenantName?: string | null;
  tenantNameEn?: string | null;
  tenantType?: number | null;
  companyName?: string | null;
  legalPerson?: string | null;
  registrationNo?: string | null;
  taxNo?: string | null;
  industry?: string | null;
  companySize?: number | null;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  address?: string | null;
  adminUserId?: string | null;
  adminUsername?: string | null;
  adminEmail?: string | null;
  status?: number | null;
  trialStartDate?: string | null;
  trialEndDate?: string | null;
  contractStartDate?: string | null;
  contractEndDate?: string | null;
  activatedAt?: string | null;
  suspendedAt?: string | null;
  logoUrl?: string | null;
  domain?: string | null;
  themeConfig?: string | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
  deleted?: boolean | null;
  remark?: string | null;
}

/** `com.scmcloud.tenant.api.command.TenantCreateCommand`. */
export interface TenantCreateCommand {
  tenantName: string;
  tenantNameEn?: string;
  tenantCode: string;
  tenantType?: number;
  companyName?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  address?: string;
  industry?: string;
  packageId?: string;
  adminUsername?: string;
  adminEmail?: string;
}

/** `com.scmcloud.tenant.service.provisioning.TenantProvisioningResult`. */
export interface TenantProvisioningResult {
  tenantId?: string | null;
  tenantCode?: string | null;
  adminUserId?: string | null;
  adminUsername?: string | null;
  adminRoleId?: string | null;
  adminRoleCode?: string | null;
  subscriptionId?: string | null;
  success?: boolean | null;
  errorMessage?: string | null;
}

/** `com.scmcloud.tenant.domain.entity.TenantPackage`. */
export interface TenantPackageDTO {
  id: string;
  packageCode?: string | null;
  packageName?: string | null;
  packageLevel?: number | null;
  priceMonthly?: number | null;
  priceYearly?: number | null;
  discountRate?: number | null;
  maxUsers?: number | null;
  maxWarehouses?: number | null;
  maxSkus?: number | null;
  maxOrdersPerDay?: number | null;
  maxStorageGb?: number | null;
  maxApiCallsPerDay?: number | null;
  features?: string | null;
  description?: string | null;
  highlights?: string | null;
  enabled?: boolean | null;
  isTrial?: boolean | null;
  trialDays?: number | null;
  sortOrder?: number | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
  deleted?: boolean | null;
  remark?: string | null;
}

/** `com.scmcloud.tenant.domain.entity.TenantSubscription`. */
export interface TenantSubscriptionDTO {
  id: string;
  tenantId?: string | null;
  packageId?: string | null;
  subscriptionType?: number | null;
  status?: number | null;
  startDate?: string | null;
  endDate?: string | null;
  autoRenew?: boolean | null;
  originalPrice?: number | null;
  actualPrice?: number | null;
  discountAmount?: number | null;
  paymentStatus?: number | null;
  paidAt?: string | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  remark?: string | null;
  deleted?: boolean | null;
}

/** `com.scmcloud.tenant.domain.entity.TenantResourceQuota`. */
export interface TenantResourceQuotaDTO {
  id: string;
  tenantId?: string | null;
  maxUsers?: number | null;
  currentUsers?: number | null;
  maxWarehouses?: number | null;
  currentWarehouses?: number | null;
  maxSkus?: number | null;
  currentSkus?: number | null;
  maxOrdersPerDay?: number | null;
  currentOrdersToday?: number | null;
  lastOrderResetDate?: string | null;
  maxStorageGb?: number | null;
  currentStorageGb?: number | null;
  maxApiCallsPerDay?: number | null;
  currentApiCallsToday?: number | null;
  lastApiResetDate?: string | null;
  customQuotas?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** `com.scmcloud.tenant.domain.entity.TenantFeature`. */
export interface TenantFeatureDTO {
  id: string;
  tenantId?: string | null;
  featureCode?: string | null;
  featureName?: string | null;
  enabled?: boolean | null;
  isBeta?: boolean | null;
  usageLimit?: number | null;
  currentUsage?: number | null;
  expireAt?: string | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
}

/** `com.scmcloud.tenant.domain.entity.TenantConfig`. */
export interface TenantConfigDTO {
  id: string;
  tenantId?: string | null;
  configCategory?: string | null;
  configKey?: string | null;
  configValue?: string | null;
  valueType?: string | null;
  description?: string | null;
  defaultValue?: string | null;
  isEncrypted?: boolean | null;
  isPublic?: boolean | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
}

/** `com.scmcloud.tenant.domain.entity.TenantOperationLog`. */
export interface TenantOperationLogDTO {
  id: string;
  tenantId?: string | null;
  operationType?: string | null;
  operationModule?: string | null;
  operationDesc?: string | null;
  operatorId?: string | null;
  operatorName?: string | null;
  operatorIp?: string | null;
  userAgent?: string | null;
  requestUrl?: string | null;
  requestMethod?: string | null;
  requestParams?: string | null;
  responseStatus?: number | null;
  executionTime?: number | null;
  createTime?: string | null;
}

export type TenantPage = MpPage<TenantDTO>;
export type TenantPackagePage = MpPage<TenantPackageDTO>;
export type TenantSubscriptionPage = MpPage<TenantSubscriptionDTO>;
export type TenantQuotaPage = MpPage<TenantResourceQuotaDTO>;
export type TenantFeaturePage = MpPage<TenantFeatureDTO>;
export type TenantConfigPage = MpPage<TenantConfigDTO>;
export type TenantOperationLogPage = MpPage<TenantOperationLogDTO>;

export interface TenantPageQuery {
  page: number;
  size: number;
  tenantName?: string;
  tenantType?: number;
  status?: number;
}

export interface TenantPackagePageQuery {
  page: number;
  size: number;
  packageName?: string;
  packageLevel?: number;
  enabled?: boolean;
}

export interface TenantSubscriptionPageQuery {
  page: number;
  size: number;
  tenantId?: string;
  status?: number;
}

export interface TenantQuotaPageQuery {
  page: number;
  size: number;
  tenantId?: string;
}

export interface TenantFeaturePageQuery {
  page: number;
  size: number;
  tenantId?: string;
  featureCode?: string;
  enabled?: boolean;
}

export interface TenantConfigPageQuery {
  page: number;
  size: number;
  tenantId?: string;
  configCategory?: string;
  configKey?: string;
}

export interface TenantOperationLogPageQuery {
  page: number;
  size: number;
  tenantId?: string;
  operationType?: string;
  operationModule?: string;
}
