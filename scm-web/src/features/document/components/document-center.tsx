"use client";

import { useState } from "react";

import { PageHeader } from "@/components/ui/page-header";
import { cn } from "@/lib/utils";

import { DocumentFileList } from "./document-file-list";
import { DocumentList } from "./document-list";
import { TemplateList } from "./template-list";

type TabKey = "templates" | "documents" | "files";

const TABS: Array<{ key: TabKey; label: string; hint: string }> = [
  { key: "templates", label: "模板管理", hint: "模板定义、发布版本与渲染" },
  { key: "documents", label: "文档管理", hint: "渲染产物与业务文档" },
  { key: "files", label: "文档文件", hint: "模板源文件与渲染产物文件" },
];

export function DocumentCenter() {
  const [tab, setTab] = useState<TabKey>("templates");
  const active = TABS.find((item) => item.key === tab) ?? TABS[0];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="文档中心"
        title="文档模板与生成"
        description={active.hint}
      />

      <div className="flex flex-wrap items-center gap-1">
        {TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={() => setTab(item.key)}
            className={cn(
              "rounded-md px-3 py-1.5 text-[12px] transition-colors",
              tab === item.key
                ? "bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary-text)]"
                : "text-muted-foreground hover:bg-sunken",
            )}
          >
            {item.label}
          </button>
        ))}
      </div>

      {tab === "templates" && <TemplateList />}
      {tab === "documents" && <DocumentList />}
      {tab === "files" && <DocumentFileList />}
    </div>
  );
}
