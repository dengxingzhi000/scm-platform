import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  TenantConfigDTO,
  TenantConfigPage,
  TenantConfigPageQuery,
  TenantCreateCommand,
  TenantDTO,
  TenantFeatureDTO,
  TenantFeaturePage,
  TenantFeaturePageQuery,
  TenantOperationLogDTO,
  TenantOperationLogPage,
  TenantOperationLogPageQuery,
  TenantPackageDTO,
  TenantPackagePage,
  TenantPackagePageQuery,
  TenantPage,
  TenantPageQuery,
  TenantProvisioningResult,
  TenantQuotaPage,
  TenantQuotaPageQuery,
  TenantResourceQuotaDTO,
  TenantSubscriptionDTO,
  TenantSubscriptionPage,
  TenantSubscriptionPageQuery,
} from "../contracts/tenant";

/** scm-tenant 的控制器直接挂在服务根上（没有 `/api/v1` 前缀）。 */
const TENANT_BASE = "/scm-tenant";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, body?: unknown, params?: object) =>
  unwrap<T>(apiClient.post(url, body, { params }));

const put = <T>(url: string, body?: unknown, params?: object) =>
  unwrap<T>(apiClient.put(url, body, { params }));

const del = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.delete(url, { params }));

export const tenantApiService = {
  page: (query: TenantPageQuery) =>
    get<TenantPage>(`${TENANT_BASE}/tenant/page`, query),
  listActive: () => get<TenantDTO[]>(`${TENANT_BASE}/tenant/active`),
  getById: (id: string) => get<TenantDTO>(`${TENANT_BASE}/tenant/${id}`),
  create: (entity: Partial<TenantDTO>) =>
    post<TenantDTO>(`${TENANT_BASE}/tenant`, entity),
  update: (entity: Partial<TenantDTO>) =>
    put<TenantDTO>(`${TENANT_BASE}/tenant`, entity),
  remove: (id: string) => del<boolean>(`${TENANT_BASE}/tenant/${id}`),
  enable: (id: string) => put<boolean>(`${TENANT_BASE}/tenant/${id}/enable`),
  disable: (id: string) => put<boolean>(`${TENANT_BASE}/tenant/${id}/disable`),
  provision: (command: TenantCreateCommand) =>
    post<TenantProvisioningResult>(`${TENANT_BASE}/tenant/provisioning`, command),

  pagePackages: (query: TenantPackagePageQuery) =>
    get<TenantPackagePage>(`${TENANT_BASE}/tenant-package/page`, query),
  listActivePackages: () =>
    get<TenantPackageDTO[]>(`${TENANT_BASE}/tenant-package/active`),
  createPackage: (entity: Partial<TenantPackageDTO>) =>
    post<TenantPackageDTO>(`${TENANT_BASE}/tenant-package`, entity),
  updatePackage: (entity: Partial<TenantPackageDTO>) =>
    put<TenantPackageDTO>(`${TENANT_BASE}/tenant-package`, entity),
  removePackage: (id: string) =>
    del<boolean>(`${TENANT_BASE}/tenant-package/${id}`),

  pageSubscriptions: (query: TenantSubscriptionPageQuery) =>
    get<TenantSubscriptionPage>(`${TENANT_BASE}/tenant-subscription/page`, query),
  listTenantSubscriptions: (tenantId: string) =>
    get<TenantSubscriptionDTO[]>(
      `${TENANT_BASE}/tenant-subscription/tenant/${tenantId}`,
    ),
  subscribe: (tenantId: string, packageId: string) =>
    post<boolean>(`${TENANT_BASE}/tenant-subscription/subscribe`, undefined, {
      tenantId,
      packageId,
    }),
  unsubscribe: (id: string) =>
    post<boolean>(`${TENANT_BASE}/tenant-subscription/${id}/unsubscribe`),
  removeSubscription: (id: string) =>
    del<boolean>(`${TENANT_BASE}/tenant-subscription/${id}`),

  pageQuotas: (query: TenantQuotaPageQuery) =>
    get<TenantQuotaPage>(`${TENANT_BASE}/tenant-resource-quota/page`, query),
  listTenantQuotas: (tenantId: string) =>
    get<TenantResourceQuotaDTO[]>(
      `${TENANT_BASE}/tenant-resource-quota/tenant/${tenantId}`,
    ),
  createQuota: (entity: Partial<TenantResourceQuotaDTO>) =>
    post<TenantResourceQuotaDTO>(`${TENANT_BASE}/tenant-resource-quota`, entity),
  updateQuota: (entity: Partial<TenantResourceQuotaDTO>) =>
    put<TenantResourceQuotaDTO>(`${TENANT_BASE}/tenant-resource-quota`, entity),
  removeQuota: (id: string) =>
    del<boolean>(`${TENANT_BASE}/tenant-resource-quota/${id}`),

  pageFeatures: (query: TenantFeaturePageQuery) =>
    get<TenantFeaturePage>(`${TENANT_BASE}/tenant-feature/page`, query),
  listTenantFeatures: (tenantId: string) =>
    get<TenantFeatureDTO[]>(`${TENANT_BASE}/tenant-feature/tenant/${tenantId}`),
  createFeature: (entity: Partial<TenantFeatureDTO>) =>
    post<TenantFeatureDTO>(`${TENANT_BASE}/tenant-feature`, entity),
  updateFeature: (entity: Partial<TenantFeatureDTO>) =>
    put<TenantFeatureDTO>(`${TENANT_BASE}/tenant-feature`, entity),
  removeFeature: (id: string) =>
    del<boolean>(`${TENANT_BASE}/tenant-feature/${id}`),

  pageConfigs: (query: TenantConfigPageQuery) =>
    get<TenantConfigPage>(`${TENANT_BASE}/tenant-config/page`, query),
  createConfig: (entity: Partial<TenantConfigDTO>) =>
    post<TenantConfigDTO>(`${TENANT_BASE}/tenant-config`, entity),
  updateConfig: (entity: Partial<TenantConfigDTO>) =>
    put<TenantConfigDTO>(`${TENANT_BASE}/tenant-config`, entity),
  removeConfig: (id: string) =>
    del<boolean>(`${TENANT_BASE}/tenant-config/${id}`),

  pageOperationLogs: (query: TenantOperationLogPageQuery) =>
    get<TenantOperationLogPage>(
      `${TENANT_BASE}/tenant-operation-log/page`,
      query,
    ),
  listTenantOperationLogs: (tenantId: string) =>
    get<TenantOperationLogDTO[]>(
      `${TENANT_BASE}/tenant-operation-log/tenant/${tenantId}`,
    ),
};
