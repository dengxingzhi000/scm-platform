import type { LoginResponse, UserInfo } from "@/lib/api/contracts/auth";
import type { User } from "@/stores/useAuthStore";

export function toSessionUser(response: LoginResponse): User {
  return {
    id: response.userId,
    username: response.username,
    displayName: response.realName || response.username,
    roles: response.roles ?? [],
  };
}

export function mergeUserInfo(
  current: User | null,
  info: UserInfo
): User {
  return {
    id: info.userId,
    username: info.username,
    displayName: info.realName || current?.displayName || info.username,
    email: info.email ?? current?.email,
    avatar: info.avatar ?? current?.avatar,
    roles: info.roles ?? current?.roles,
  };
}
