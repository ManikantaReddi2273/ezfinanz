/**
 * Route guards: require auth, enforce role, or keep guests off authenticated pages.
 */
import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { postLoginPath } from "../auth/paths";
import type { Role } from "../api/client";

/** Renders child routes only when a user session exists; otherwise redirects to login. */
export function ProtectedRoute() {
  const { user, loading } = useAuth();
  if (loading) {
    return <p className="p-8 text-center text-navy">Loading…</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}

/** Restricts nested routes to a specific role; other roles go to their home path. */
export function RoleRoute({ role }: { role: Role }) {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (user.role !== role) {
    return <Navigate to={postLoginPath(user)} replace />;
  }
  return <Outlet />;
}

/** For login/signup: redirects authenticated users away to their post-login path. */
export function GuestRoute() {
  const { user, loading } = useAuth();
  if (loading) {
    return <p className="p-8 text-center text-navy">Loading…</p>;
  }
  if (user) {
    return <Navigate to={postLoginPath(user)} replace />;
  }
  return <Outlet />;
}
