/**
 * The raw MyBatis-Plus `Page` serialization used by scm-order and scm-inventory.
 * Note the naming mismatch: requests send `pageNum`/`size`, responses echo `current`/`size`.
 */
export interface MpPage<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages?: number;
}

export interface UiPage<T> {
  records: T[];
  total: number;
  current: number;
  pageSize: number;
}

export function toUiPage<T, R>(
  page: MpPage<T> | null | undefined,
  map: (item: T) => R
): UiPage<R> {
  return {
    records: (page?.records ?? []).map(map),
    total: page?.total ?? 0,
    current: page?.current ?? 1,
    pageSize: page?.size ?? 0,
  };
}
