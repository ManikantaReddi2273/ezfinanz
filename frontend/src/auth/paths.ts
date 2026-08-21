/**
 * Post-auth navigation helpers: verification checks and role-based home paths.
 */
import type { User } from "../api/client";

/** True when both email and phone are verified. */
export function isFullyVerified(user: User): boolean {
  return user.emailVerified && user.phoneVerified;
}

/** Default landing path after login for the user's role. */
export function postLoginPath(user: User): string {
  if (user.role === "ADMIN") {
    return "/admin";
  }
  return "/customer";
}
