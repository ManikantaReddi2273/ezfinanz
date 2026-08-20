import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { postLoginPath } from "../auth/paths";
import type { Role } from "../api/client";

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
