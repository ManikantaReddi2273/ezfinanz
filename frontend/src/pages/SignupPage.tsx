import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { ApiError, authApi } from "../api/client";
import { AuthLayout } from "../components/AuthLayout";
import { GoogleSignInButton } from "../components/GoogleSignInButton";

type SignupForm = {
  fullName: string;
  email: string;
  password: string;
  confirmPassword: string;
};

export function SignupPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, watch, formState } = useForm<SignupForm>();
  const password = watch("password");

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      await authApi.signupEmail({
        email: values.email,
        password: values.password,
        fullName: values.fullName || undefined,
      });
      navigate("/verify-otp", { state: { channel: "email", target: values.email } });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not create the account.");
    }
  });

  return (
    <AuthLayout title="Create your account" subtitle="Verify your email to continue. Phone login is also available.">
      <form className="space-y-4" onSubmit={onSubmit}>
        <label className="block">
          <span className="text-sm font-medium text-navy">Full name</span>
          <input
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2.5 outline-none focus:border-teal"
            {...register("fullName")}
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-navy">Email</span>
          <input
            type="email"
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2.5 outline-none focus:border-teal"
            {...register("email", { required: "Email is required" })}
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-navy">Password</span>
          <input
            type="password"
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2.5 outline-none focus:border-teal"
            {...register("password", {
              required: "Password is required",
              minLength: { value: 8, message: "At least 8 characters" },
            })}
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-navy">Confirm password</span>
          <input
            type="password"
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2.5 outline-none focus:border-teal"
            {...register("confirmPassword", {
              required: "Confirm your password",
              validate: (value) => value === password || "Passwords do not match",
            })}
          />
        </label>
        {formState.errors.confirmPassword && (
          <p className="text-sm text-red-700">{formState.errors.confirmPassword.message}</p>
        )}
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button
          type="submit"
          disabled={formState.isSubmitting}
          className="w-full rounded-lg bg-teal-700 py-2.5 font-medium text-white hover:bg-teal-800 disabled:opacity-60"
        >
          {formState.isSubmitting ? "Creating account…" : "Sign up"}
        </button>
      </form>
      <div className="relative my-5">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-stone-200" />
        </div>
        <p className="relative mx-auto w-fit bg-white px-3 text-xs text-stone-400">or</p>
      </div>
      <GoogleSignInButton />
      <p className="mt-6 text-sm text-stone-600">
        Already have an account?{" "}
        <Link to="/login" className="font-medium text-teal">
          Sign in
        </Link>
      </p>
    </AuthLayout>
  );
}
