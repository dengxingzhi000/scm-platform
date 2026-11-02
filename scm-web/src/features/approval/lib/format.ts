import dayjs from "dayjs";

export const formatDateTime = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
