/**
 * Google OAuth helpers — builds the backend start URL for sign-in redirects.
 */
const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

/** Backend URL that begins the Google OAuth redirect flow. */
export function googleSignInUrl(): string {
  return `${API_URL}/api/auth/google/start`;
}
