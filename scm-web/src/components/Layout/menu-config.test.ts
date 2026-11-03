import { describe, expect, it } from "vitest";
import { appMenuItems, matchMenuKey, getParentGroupKey } from "./menu-config";

describe("supplier menu group", () => {
  it("exposes the three supplier pages under one group", () => {
    const group = appMenuItems.find((item) => (item as { key: string }).key === "group-supplier") as { children?: { key: string }[] };
    expect(group).toBeDefined();
    expect(group.children?.map((child) => child.key)).toEqual(["/supplier", "/supplier/audit", "/supplier/category"]);
  });

  it("matches the audit route to the supplier group", () => {
    expect(matchMenuKey("/zh-CN/supplier/audit")).toBe("/supplier/audit");
    expect(getParentGroupKey("/zh-CN/supplier/audit")).toBe("group-supplier");
  });
});
