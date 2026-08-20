const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export function googleSignInUrl(): string {
  return `${API_URL}/api/auth/google/start`;
}
