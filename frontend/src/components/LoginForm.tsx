import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { ApiError, authApi } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { postLoginPath } from "../auth/paths";
import { Eye, EyeOff } from "lucide-react";
import { GoogleSignInButton } from "./GoogleSignInButton";

type EmailForm = { email: string; password: string };
type PhoneForm = { phone: string };

export function LoginForm() {
  const [tab, setTab] = useState<"email" | "phone">("email");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const { setSession } = useAuth();
  const emailForm = useForm<EmailForm>();
  const phoneForm = useForm<PhoneForm>();

  const onEmailLogin = emailForm.handleSubmit(async (values) => {
    setError(null);
    try {
      const result = await authApi.loginEmail(values);
      setSession(result.token, result.user);
      navigate(postLoginPath(result.user), { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.code === "EMAIL_NOT_VERIFIED") {
        navigate("/verify-otp", {
          state: { channel: "email", target: values.email, reason: "verify-before-login" },
        });
        return;
      }
      setError(err instanceof ApiError ? err.message : "Could not log in.");
    }
  });

  const onPhoneSend = phoneForm.handleSubmit(async (values) => {
    setError(null);
    try {
      await authApi.sendPhoneOtp(values.phone);
      navigate("/verify-otp", { state: { channel: "phone", target: values.phone } });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not send OTP.");
    }
  });

  const inputClass =
    "mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm outline-none focus:border-blue-600 focus:bg-white focus:ring-2 focus:ring-blue-600/15";

  return (
    <div>
      <div className="flex gap-2 text-xs font-medium">
        <button
          type="button"
          className={tab === "email" ? "text-blue-700" : "text-slate-400"}
          onClick={() => setTab("email")}
        >
          Email
        </button>
        <span className="text-slate-300">·</span>
        <button
          type="button"
          className={tab === "phone" ? "text-blue-700" : "text-slate-400"}
          onClick={() => setTab("phone")}
        >
          Phone OTP
        </button>
      </div>

      {tab === "email" ? (
        <form className="mt-4 space-y-4" onSubmit={onEmailLogin}>
          <label className="block text-sm font-medium text-slate-700">
            Email or phone number
            <input type="email" className={inputClass} placeholder="you@email.com" {...emailForm.register("email", { required: true })} />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Password
            <span className="relative mt-1.5 block">
              <input
                type={showPassword ? "text" : "password"}
                className={`${inputClass} mt-0 pr-10`}
                {...emailForm.register("password", { required: true })}
              />
              <button
                type="button"
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"
                onClick={() => setShowPassword((value) => !value)}
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </span>
          </label>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={emailForm.formState.isSubmitting}
            className="w-full rounded-xl bg-blue-600 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
          >
            {emailForm.formState.isSubmitting ? "Signing in…" : "Login"}
          </button>
        </form>
      ) : (
        <form className="mt-4 space-y-4" onSubmit={onPhoneSend}>
          <label className="block text-sm font-medium text-slate-700">
            Phone number
            <input type="tel" placeholder="9876543210" className={inputClass} {...phoneForm.register("phone", { required: true })} />
          </label>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={phoneForm.formState.isSubmitting}
            className="w-full rounded-xl bg-blue-600 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
          >
            {phoneForm.formState.isSubmitting ? "Sending…" : "Continue with phone"}
          </button>
        </form>
      )}

      {tab === "email" && (
        <button
          type="button"
          onClick={() => setTab("phone")}
          className="mt-3 w-full rounded-xl border border-slate-200 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          Continue with Phone
        </button>
      )}
      <GoogleSignInButton />
      <p className="mt-5 text-center text-sm text-slate-600">
        Don’t have an account?{" "}
        <Link to="/signup" className="font-semibold text-blue-700">
          Sign up
        </Link>
      </p>
    </div>
  );
}
