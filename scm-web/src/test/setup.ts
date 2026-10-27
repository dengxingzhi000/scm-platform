import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

/**
 * Vitest 没有开 globals:true,所以全局不存在 afterEach,
 * Testing Library 的自动 cleanup 不会注册(见 @testing-library/react 的
 * dist/index.js:仅在全局 afterEach/teardown 存在时自动 cleanup)。
 * 不清理的话,同一个文件里的用例会把 DOM 累积起来,
 * getByText 之类的单元素查询会命中上一个用例留下的节点。
 */
afterEach(() => {
  cleanup();
});
