"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { authApi } from "../endpoints";
import { useAuthStore } from "@/stores/useAuthStore";
import type { LoginRequest, LoginResponse } from "../types";

export function useLogin() {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (res: unknown) => {
      const loginRes = res as LoginResponse;
      setAuth(loginRes.accessToken, loginRes.user);
    },
  });
}

export function useLogout() {
  const logout = useAuthStore((s) => s.logout);

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => logout(),
  });
}

export function useCurrentUser() {
  return useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => authApi.me(),
    enabled: useAuthStore((s) => !!s.token),
  });
}
