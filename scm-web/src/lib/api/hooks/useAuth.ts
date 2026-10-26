"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { authService } from "../services/auth";
import { useAuthStore } from "@/stores/useAuthStore";
import { toSessionUser } from "@/lib/auth/session";
import type { LoginRequest } from "../contracts/auth";

export function useLogin() {
  const login = useAuthStore((s) => s.login);
  const setPermissions = useAuthStore((s) => s.setPermissions);

  return useMutation({
    mutationFn: (data: LoginRequest) => authService.login(data),
    onSuccess: (response) => {
      if (!response.accessToken) {
        throw new Error(response.message || "Login did not return an access token");
      }
      login(
        toSessionUser(response),
        response.accessToken,
        response.refreshToken ?? ""
      );
      setPermissions(response.permissions ?? []);
    },
  });
}

export function useLogout() {
  const logout = useAuthStore((s) => s.logout);

  return useMutation({
    mutationFn: () => authService.logout(),
    onSettled: () => logout(),
  });
}

export function useCurrentUser() {
  return useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => authService.getUserInfo(),
    enabled: useAuthStore((s) => !!s.accessToken),
  });
}
