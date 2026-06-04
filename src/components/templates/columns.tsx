import type { ColumnsType } from "antd/es/table";

export function createDateColumn<T>(key: keyof T, title = "创建时间"): ColumnsType<T>[number] {
  return {
    title,
    dataIndex: key as string,
    key: key as string,
    width: 180,
    render: (val: string) => (val ? new Date(val).toLocaleString("zh-CN") : "-"),
  };
}

export function createStatusColumn<T>(
  key: keyof T,
  title = "状态",
  statusMap?: Record<string, { color: string; text: string }>
): ColumnsType<T>[number] {
  return {
    title,
    dataIndex: key as string,
    key: key as string,
    width: 120,
    render: (val: string) => {
      const status = statusMap?.[val];
      return (
        <span style={{ color: status?.color || "#000" }}>
          {status?.text || val}
        </span>
      );
    },
  };
}

export function createActionsColumn<T>(
  actions: (record: T) => React.ReactNode
): ColumnsType<T>[number] {
  return {
    title: "操作",
    key: "action",
    width: 200,
    render: (_: unknown, record: T) => actions(record),
  };
}
