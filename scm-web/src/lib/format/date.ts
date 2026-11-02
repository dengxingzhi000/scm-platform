import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/zh-cn";

dayjs.extend(relativeTime);

export const formatDateTime = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";

/**
 * Timestamps arrive as zone-less `LocalDateTime` strings, so dayjs reads them in the
 * browser zone — the same wall clock the containers run on (TZ=Asia/Shanghai).
 */
export const formatRelativeTime = (value?: string | null): string =>
  value ? dayjs(value).locale("zh-cn").fromNow() : "-";
