import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));

vi.mock("@/lib/api/client", () => ({
  default: { get: h.get, post: h.post, put: h.put, delete: h.delete },
}));

import { supplierApiService } from "./supplier";

/**
 * The scm-supplier controllers disagree on where the parameters live: most
 * mutations read them from the query string, only `PUT /{id}/audit`,
 * `PUT /{id}/categories`, `POST /`, `PUT /{id}` and the sub-resource
 * writes take a JSON body. These tests pin that split down, because getting it
 * wrong surfaces as an HTTP 400 at runtime.
 */
describe("supplier api service", () => {
  beforeEach(() => {
    h.get.mockReset().mockResolvedValue({ code: 200, data: [] });
    h.post.mockReset().mockResolvedValue({ code: 200, data: [] });
    h.put.mockReset().mockResolvedValue({ code: 200, data: [] });
    h.delete.mockReset().mockResolvedValue({ code: 200, data: [] });
  });

  it("sends the supplier filters as query params", () => {
    supplierApiService.list({
      page: 2,
      size: 10,
      keyword: "华强",
      supplierType: 1,
      cooperationStatus: 1,
      enabled: true,
      auditStatus: 2,
      supplierLevel: 1,
      riskLevel: 2,
      blacklisted: false,
      categoryId: "c-1",
    });

    expect(h.get).toHaveBeenCalledWith("/scm-supplier/api/v1/suppliers", {
      params: {
        page: 2,
        size: 10,
        keyword: "华强",
        supplierType: 1,
        cooperationStatus: 1,
        enabled: true,
        auditStatus: 2,
        supplierLevel: 1,
        riskLevel: 2,
        blacklisted: false,
        categoryId: "c-1",
      },
    });
  });

  it("puts the blacklist reason in a query param, not a body", () => {
    supplierApiService.blacklist("s-1", "资质过期", {
      operatorId: "u-1",
      operatorName: "张三",
    });

    const [url, body, config] = h.put.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/suppliers/s-1/blacklist");
    expect(body).toBeUndefined();
    expect(config.params).toMatchObject({
      reason: "资质过期",
      operatorId: "u-1",
      operatorName: "张三",
    });
  });

  it("puts the risk level and its reason in query params", () => {
    supplierApiService.changeRiskLevel("s-1", 3, "交付延迟", { operatorId: "u-1" });

    const [url, body, config] = h.put.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/suppliers/s-1/risk-level");
    expect(body).toBeUndefined();
    expect(config.params).toMatchObject({
      level: 3,
      reason: "交付延迟",
      operatorId: "u-1",
    });
  });

  it("puts the cooperation status in a query param", () => {
    supplierApiService.changeCooperationStatus("s-1", 1, { operatorId: "u-1" });

    const [url, body, config] = h.put.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/suppliers/s-1/cooperation-status");
    expect(body).toBeUndefined();
    expect(config.params.status).toBe(1);
  });

  it("sends the audit decision as a json body", () => {
    supplierApiService.audit("s-1", {
      action: "APPROVE",
      operatorId: "u-1",
      operatorName: "张三",
      remark: "资料齐全",
    });

    const [url, body] = h.put.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/suppliers/s-1/audit");
    expect(body).toEqual({
      action: "APPROVE",
      operatorId: "u-1",
      operatorName: "张三",
      remark: "资料齐全",
    });
  });

  it("sends the category ids as a bare array", () => {
    supplierApiService.assignCategories("s-1", ["c-1", "c-2"]);

    const [url, body] = h.put.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/suppliers/s-1/categories");
    expect(Array.isArray(body)).toBe(true);
    expect(body).toEqual(["c-1", "c-2"]);
  });

  it("sends the submit-audit operator as query params with no body", () => {
    supplierApiService.submitAudit("s-1", { operatorId: "u-1", operatorName: "张三" });

    const [url, body, config] = h.post.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/suppliers/s-1/submit-audit");
    expect(body).toBeUndefined();
    expect(config.params).toMatchObject({
      operatorId: "u-1",
      operatorName: "张三",
    });
  });

  it("sends the certificate verification decision as query params", () => {
    supplierApiService.certificates.verify("s-1", "cert-1", {
      approved: true,
      operatorId: "u-1",
      remark: "已核验",
    });

    const [url, body, config] = h.put.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/suppliers/s-1/certificates/cert-1/verify");
    expect(body).toBeUndefined();
    expect(config.params.approved).toBe(true);
  });

  it("sends the operator as query params when deleting", () => {
    supplierApiService.remove("s-1", { operatorId: "u-1", operatorName: "张三" });

    const [url, config] = h.delete.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/suppliers/s-1");
    expect(config.params).toMatchObject({
      operatorId: "u-1",
      operatorName: "张三",
    });
  });

  it("keeps the legacy list and getById signatures", () => {
    supplierApiService.list({ page: 1, size: 20 });
    expect(h.get.mock.calls[0][0]).toBe("/scm-supplier/api/v1/suppliers");

    supplierApiService.getById("s-1");
    expect(h.get.mock.calls[1][0]).toBe("/scm-supplier/api/v1/suppliers/s-1");
  });

  it("reads the certificate expiry window from query params", () => {
    supplierApiService.certificateExpiry.expiring({ page: 1, size: 10, days: 60 });

    const [url, config] = h.get.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/supplier-certificates/expiring");
    expect(config.params.days).toBe(60);
  });

  it("reads the evaluation page from query params", () => {
    supplierApiService.evaluations.page({ page: 1, size: 10, supplierId: "s-1" });

    const [url, config] = h.get.mock.calls[0];
    expect(url).toBe("/scm-supplier/api/v1/supplier-evaluations");
    expect(config.params.supplierId).toBe("s-1");
  });
});
