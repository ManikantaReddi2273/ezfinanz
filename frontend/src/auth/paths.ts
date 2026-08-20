import type { User } from "../api/client";

export function isFullyVerified(user: User): boolean {
  return user.emailVerified && user.phoneVerified;
}

export function postLoginPath(user: User): string {
  if (user.role === "ADMIN") {
    return "/admin";
  }
  return "/customer";
}
