import { create } from "zustand";
import { persist } from "zustand/middleware";

type ThemeMode = "light" | "dark";

type UIState = {
  sidebarCollapsed: boolean;
  theme: ThemeMode;
  locale: string;
  toggleSidebar: () => void;
  setTheme: (theme: ThemeMode) => void;
  setLocale: (locale: string) => void;
};

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      sidebarCollapsed: false,
      theme: "light",
      locale: "zh-CN",
      toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      setTheme: (theme) => set({ theme }),
      setLocale: (locale) => set({ locale }),
    }),
    { name: "scm-ui" }
  )
);
