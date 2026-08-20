import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { authApi, storeToken } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { postLoginPath } from "../auth/paths";

export function GoogleCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setSession } = useAuth();
  const [failed, setFailed] = useState(false);
  const [message, setMessage] = useState("Signing you in with Google…");

  useEffect(() => {
    const token = searchParams.get("token");
    const error = searchParams.get("error");

    if (error) {
      setFailed(true);
      setMessage(error);
      return;
    }
    if (!token) {
      setFailed(true);
      setMessage("Google sign-in did not return a session token.");
      return;
    }

    storeToken(token);
    authApi
      .me()
      .then((user) => {
        setSession(token, user);
        navigate(postLoginPath(user), { replace: true });
      })
      .catch(() => {
        setFailed(true);
        setMessage("Signed in with Google, but the session could not be loaded. Try again.");
      });
  }, [navigate, searchParams, setSession]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <p className="text-sm text-slate-600">{message}</p>
        {failed && (
          <Link to="/login" className="mt-5 inline-block text-sm font-semibold text-blue-700">
            Back to login
          </Link>
        )}
      </div>
    </div>
  );
}
