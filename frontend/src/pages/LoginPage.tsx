/**
 * Login page wrapping LoginForm in AuthLayout.
 */
import { Navigate } from "react-router-dom";
import { AuthLayout } from "../components/AuthLayout";
import { LoginForm } from "../components/LoginForm";
import { useAuth } from "../auth/AuthContext";
import { postLoginPath } from "../auth/paths";

/** Guest login screen; redirects if already signed in. */
export function LoginPage() {
  const { user, loading } = useAuth();

  if (loading) {
    return <p className="p-8 text-center text-slate-500">Loading…</p>;
  }
  if (user) {
    return <Navigate to={postLoginPath(user)} replace />;
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to continue your EZFINANZ application.">
      <LoginForm />
    </AuthLayout>
  );
}
