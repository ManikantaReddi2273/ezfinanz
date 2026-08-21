/**
 * Marketing landing; signed-in users are redirected to their role home.
 */
import { Navigate } from "react-router-dom";
import HeroSection from "@/components/ui/light-saas-hero-section";
import { useAuth } from "../auth/AuthContext";
import { postLoginPath } from "../auth/paths";

/** Public hero landing; redirects authenticated users away. */
export function LandingPage() {
  const { user, loading } = useAuth();

  if (loading) {
    return <p className="p-8 text-center text-slate-500">Loading…</p>;
  }
  if (user) {
    return <Navigate to={postLoginPath(user)} replace />;
  }

  return (
    <HeroSection
      color="#2563eb"
      speed={0.5}
      scale={1.2}
      opacity={0.15}
      mouseInteractive
    />
  );
}
