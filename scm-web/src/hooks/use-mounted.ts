"use client";

import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * False during SSR and the hydration render, true afterwards.
 *
 * A client component's server pass never has query data, while the client may already
 * have it by hydration time — that asymmetry makes React discard and rebuild the tree.
 * Gate async-derived attributes/children on this so the two renders agree.
 */
export function useMounted(): boolean {
  return useSyncExternalStore(subscribe, () => true, () => false);
}
