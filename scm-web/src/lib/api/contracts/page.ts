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

/**
 * Spring Data's `Page` serialization, used by the scm-product search endpoints
 * (`/products/search*`). Shape differs from both `PageResult` and `MpPage`.
 */
export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
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

export function springPageToUiPage<T, R>(
  page: SpringPage<T> | null | undefined,
  map: (item: T) => R
): UiPage<R> {
  return {
    records: (page?.content ?? []).map(map),
    total: page?.totalElements ?? 0,
    // Spring's `number` is zero-based, so the UI page index starts at 1.
    current: (page?.number ?? 0) + 1,
    pageSize: page?.size ?? 0,
  };
}
