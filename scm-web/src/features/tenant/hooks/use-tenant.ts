"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";

import { toApiError } from "@/lib/api/envelope";
import { tenantApiService } from "@/lib/api/services/tenant";
import type {
  TenantConfigDTO,
  TenantConfigPageQuery,
  TenantCreateCommand,
  TenantDTO,
  TenantFeatureDTO,
  TenantFeaturePageQuery,
  TenantOperationLogPageQuery,
  TenantPackageDTO,
  TenantPackagePageQuery,
  TenantPageQuery,
  TenantResourceQuotaDTO,
  TenantSubscriptionPageQuery,
} from "@/lib/api/contracts/tenant";

const fail = (error: unknown) => message.error(toApiError(error).message);

export const tenantKeys = {
  all: ["tenant"] as const,
  list: (query: TenantPageQuery) => ["tenant", "list", query] as const,
  subscriptions: (tenantId: string) =>
    ["tenant", "subscriptions", tenantId] as const,
  quotas: (tenantId: string) => ["tenant", "quotas", tenantId] as const,
  features: (tenantId: string) => ["tenant", "features", tenantId] as const,
  configs: (tenantId: string) => ["tenant", "configs", tenantId] as const,
  logs: (tenantId: string) => ["tenant", "logs", tenantId] as const,
  packages: (query: TenantPackagePageQuery) =>
    ["tenant", "packages", query] as const,
  activePackages: ["tenant", "packages", "active"] as const,
};

export function useTenantList(query: TenantPageQuery) {
  return useQuery({
    queryKey: tenantKeys.list(query),
    queryFn: () => tenantApiService.page(query),
  });
}

export function useActivePackages() {
  return useQuery({
    queryKey: tenantKeys.activePackages,
    queryFn: () => tenantApiService.listActivePackages(),
  });
}

export function useProvisionTenant() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (command: TenantCreateCommand) =>
      tenantApiService.provision(command),
    onSuccess: (result) => {
      if (result.success === false) {
        message.error(result.errorMessage || "租户开通失败");
        return;
      }
      message.success("租户已开通");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useUpdateTenant() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entity: Partial<TenantDTO>) => tenantApiService.update(entity),
    onSuccess: () => {
      message.success("租户已更新");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useToggleTenant() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enable }: { id: string; enable: boolean }) =>
      enable ? tenantApiService.enable(id) : tenantApiService.disable(id),
    onSuccess: (_data, variables) => {
      message.success(variables.enable ? "租户已启用" : "租户已停用");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useDeleteTenant() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => tenantApiService.remove(id),
    onSuccess: () => {
      message.success("租户已删除");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function usePackageList(query: TenantPackagePageQuery) {
  return useQuery({
    queryKey: tenantKeys.packages(query),
    queryFn: () => tenantApiService.pagePackages(query),
  });
}

export function useSavePackage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entity: Partial<TenantPackageDTO>) =>
      entity.id
        ? tenantApiService.updatePackage(entity)
        : tenantApiService.createPackage(entity),
    onSuccess: () => {
      message.success("套餐已保存");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useDeletePackage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => tenantApiService.removePackage(id),
    onSuccess: () => {
      message.success("套餐已删除");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useTenantSubscriptions(tenantId: string, enabled = true) {
  return useQuery({
    queryKey: tenantKeys.subscriptions(tenantId),
    queryFn: () => tenantApiService.listTenantSubscriptions(tenantId),
    enabled: enabled && !!tenantId,
  });
}

export function useSubscribePackage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      tenantId,
      packageId,
    }: {
      tenantId: string;
      packageId: string;
    }) => tenantApiService.subscribe(tenantId, packageId),
    onSuccess: () => {
      message.success("套餐已订阅");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useUnsubscribe() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => tenantApiService.unsubscribe(id),
    onSuccess: () => {
      message.success("订阅已取消");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useTenantQuotas(tenantId: string, enabled = true) {
  return useQuery({
    queryKey: tenantKeys.quotas(tenantId),
    queryFn: () => tenantApiService.listTenantQuotas(tenantId),
    enabled: enabled && !!tenantId,
  });
}

export function useSaveQuota() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entity: Partial<TenantResourceQuotaDTO>) =>
      entity.id
        ? tenantApiService.updateQuota(entity)
        : tenantApiService.createQuota(entity),
    onSuccess: () => {
      message.success("配额已保存");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useTenantFeatures(tenantId: string, enabled = true) {
  return useQuery({
    queryKey: tenantKeys.features(tenantId),
    queryFn: () => tenantApiService.listTenantFeatures(tenantId),
    enabled: enabled && !!tenantId,
  });
}

export function useSaveFeature() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entity: Partial<TenantFeatureDTO>) =>
      entity.id
        ? tenantApiService.updateFeature(entity)
        : tenantApiService.createFeature(entity),
    onSuccess: () => {
      message.success("功能开关已保存");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useDeleteFeature() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => tenantApiService.removeFeature(id),
    onSuccess: () => {
      message.success("功能开关已删除");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useTenantConfigs(tenantId: string, enabled = true) {
  const query: TenantConfigPageQuery = { page: 1, size: 100, tenantId };
  return useQuery({
    queryKey: tenantKeys.configs(tenantId),
    queryFn: () => tenantApiService.pageConfigs(query),
    enabled: enabled && !!tenantId,
  });
}

export function useSaveConfig() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entity: Partial<TenantConfigDTO>) =>
      entity.id
        ? tenantApiService.updateConfig(entity)
        : tenantApiService.createConfig(entity),
    onSuccess: () => {
      message.success("配置已保存");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useDeleteConfig() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => tenantApiService.removeConfig(id),
    onSuccess: () => {
      message.success("配置已删除");
      qc.invalidateQueries({ queryKey: ["tenant"] });
    },
    onError: fail,
  });
}

export function useTenantLogs(tenantId: string, enabled = true) {
  return useQuery({
    queryKey: tenantKeys.logs(tenantId),
    queryFn: () => tenantApiService.listTenantOperationLogs(tenantId),
    enabled: enabled && !!tenantId,
  });
}

export function useOperationLogPage(query: TenantOperationLogPageQuery) {
  return useQuery({
    queryKey: ["tenant", "logs", "page", query],
    queryFn: () => tenantApiService.pageOperationLogs(query),
  });
}

export function useSubscriptionPage(query: TenantSubscriptionPageQuery) {
  return useQuery({
    queryKey: ["tenant", "subscriptions", "page", query],
    queryFn: () => tenantApiService.pageSubscriptions(query),
  });
}

export function useFeaturePage(query: TenantFeaturePageQuery) {
  return useQuery({
    queryKey: ["tenant", "features", "page", query],
    queryFn: () => tenantApiService.pageFeatures(query),
  });
}
