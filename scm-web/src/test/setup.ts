import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

/**
 * jsdom 没有实现 ResizeObserver。Radix UI 的 Checkbox 内部走
 * @radix-ui/react-use-size,它在 useLayoutEffect 里 `new ResizeObserver(...)`,
 * 于是渲染 Checkbox 的测试会直接抛 "ResizeObserver is not defined"。
 *
 * 这里补一个只会 observe/unobserve 的空实现 —— 组件测试关心的是交互与可访问性,
 * 不需要真实的尺寸回调。
 */
if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = class ResizeObserverStub {
    constructor(callback: ResizeObserverCallback) {
      if (typeof callback !== "function") {
        throw new TypeError("ResizeObserver callback must be a function");
      }
    }
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver;
}

/**
 * jsdom 也没有实现 Pointer Capture 与 scrollIntoView。Radix UI 的 Select 在
 * 处理指针事件时会调 `target.hasPointerCapture(...)`,选中选项时还会调
 * `candidate.scrollIntoView(...)`,于是点开下拉会直接抛
 * "target.hasPointerCapture is not a function" / "candidate?.scrollIntoView is not a function"。
 *
 * 这里补几个空实现 —— 组件测试关心的是交互与可访问性,
 * 不需要真实的指针捕获状态与滚动行为。
 */
if (!Element.prototype.hasPointerCapture) {
  Element.prototype.hasPointerCapture = () => false;
}
if (!Element.prototype.setPointerCapture) {
  Element.prototype.setPointerCapture = () => {};
}
if (!Element.prototype.releasePointerCapture) {
  Element.prototype.releasePointerCapture = () => {};
}
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {};
}

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
