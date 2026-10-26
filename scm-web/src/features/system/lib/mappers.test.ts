import { describe, expect, it } from "vitest";

import {
  normalizeRoleCode,
  toPageResult,
  toPermission,
  toRole,
  toUser,
  toUserPayload,
} from "./mappers";

describe("system mappers", () => {
  it("maps backend user fields and status values", () => {
    const user = toUser({
      id: "u1",
      username: "admin",
      realName: "管理员",
      email: "admin@scm.com",
      status: 2,
      roleNames: ["ROLE_ADMIN"],
      createTime: "2026-01-01 10:00:00",
    });

    expect(user.displayName).toBe("管理员");
    expect(user.status).toBe("locked");
    expect(user.roles).toEqual(["ROLE_ADMIN"]);
    expect(user.createdAt).toBe("2026-01-01 10:00:00");
  });

  it("maps the backend page envelope", () => {
    const page = toPageResult(
      {
        total: 1,
        pageNum: 2,
        pageSize: 10,
        pages: 1,
        records: [{ id: "u1", username: "admin", realName: "Admin" }],
      },
      toUser
    );

    expect(page.current).toBe(2);
    expect(page.pageSize).toBe(10);
    expect(page.total).toBe(1);
    expect(page.records[0].displayName).toBe("Admin");
  });

  it("normalizes role codes to the backend pattern", () => {
    expect(normalizeRoleCode("warehouse_admin")).toBe("ROLE_WAREHOUSE_ADMIN");
    expect(normalizeRoleCode("ROLE_ADMIN")).toBe("ROLE_ADMIN");
    expect(normalizeRoleCode("role-123")).toBe("ROLE_ROLE");
  });

  it("maps roles and their permission ids", () => {
    const role = toRole({
      id: "r1",
      roleCode: "ROLE_ADMIN",
      roleName: "管理员",
      roleLevel: 1,
      dataScope: 1,
      permissionIds: ["p1", "p2"],
      status: 1,
    });

    expect(role.name).toBe("管理员");
    expect(role.code).toBe("ROLE_ADMIN");
    expect(role.permissionIds).toEqual(["p1", "p2"]);
    expect(role.status).toBe("active");
  });

  it("maps permission types and builds update payloads", () => {
    const permission = toPermission({
      id: "p1",
      permissionCode: "system:user:list",
      permissionName: "用户列表",
      permissionType: 3,
      routePath: "/system/user",
      status: 1,
    });

    expect(permission.type).toBe("button");
    expect(permission.path).toBe("/system/user");

    const payload = toUserPayload({
      username: "admin",
      displayName: "管理员",
      status: "disabled",
    });

    expect(payload.realName).toBe("管理员");
    expect(payload.status).toBe(0);
  });
});
