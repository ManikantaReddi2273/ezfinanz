import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { ApiError, authApi } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { postLoginPath } from "../auth/paths";
import { AuthLayout } from "../components/AuthLayout";

type OtpState = {
  channel: "email" | "phone";
  target: string;
  reason?: string;
};

type OtpForm = { otp: string };

export function VerifyOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { setSession } = useAuth();
  const state = location.state as OtpState | null;
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(
    state?.reason === "verify-before-login" ? "Verify your email to finish signing in." : null,
  );
  const { register, handleSubmit, formState } = useForm<OtpForm>();

  if (!state?.channel || !state.target) {
    return <Navigate to="/login" replace />;
  }

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      const result =
        state.channel === "email"
          ? await authApi.verifyEmailOtp(state.target, values.otp)
          : await authApi.verifyPhoneOtp(state.target, values.otp);
      setSession(result.token, result.user);
      navigate(postLoginPath(result.user), { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Verification failed.");
    }
  });

  const resend = async () => {
    setError(null);
    try {
      if (state.channel === "email") {
        await authApi.resendEmailOtp(state.target);
      } else {
        await authApi.sendPhoneOtp(state.target);
      }
      setInfo("A new code was sent.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not resend the code.");
    }
  };

  return (
    <AuthLayout
      title="Enter verification code"
      subtitle={`We sent a 6-digit code to ${state.target}.`}
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <label className="block">
          <span className="text-sm font-medium text-navy">Code</span>
          <input
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2.5 tracking-[0.4em] text-center text-lg outline-none focus:border-teal"
            {...register("otp", {
              required: "Enter the 6-digit code",
              pattern: { value: /^\d{6}$/, message: "Enter 6 digits" },
            })}
          />
        </label>
        {info && <p className="text-sm text-teal">{info}</p>}
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button
          type="submit"
          disabled={formState.isSubmitting}
          className="w-full rounded-lg bg-teal py-2.5 font-medium text-white hover:bg-teal-light disabled:opacity-60"
        >
          {formState.isSubmitting ? "Verifying…" : "Verify"}
        </button>
      </form>
      <button type="button" onClick={resend} className="mt-4 text-sm font-medium text-navy">
        Resend code
      </button>
      <p className="mt-6 text-sm text-stone-600">
        <Link to="/login" className="font-medium text-teal">
          Back to sign in
        </Link>
      </p>
    </AuthLayout>
  );
}
