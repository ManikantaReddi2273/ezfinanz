/**
 * Standalone email/phone verification page for customers who are not fully verified.
 */
import { useState } from "react";
import { Navigate } from "react-router-dom";
import { ApiError, authApi } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { isFullyVerified } from "../auth/paths";

/** Step-2 contact verification UI; redirects when already verified or admin. */
export function VerifyContactsPage() {
  const { user, logout } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (user.role === "ADMIN") {
    return <Navigate to="/admin" replace />;
  }
  if (isFullyVerified(user)) {
    return <Navigate to="/customer" replace />;
  }

  return (
    <div className="min-h-screen bg-cream">
      <header className="bg-navy text-cream px-6 py-4 flex items-center justify-between">
        <p className="text-lg font-semibold">EZFINANZ</p>
        <button type="button" onClick={logout} className="text-sm text-cream/80 hover:text-white">
          Sign out
        </button>
      </header>
      <main className="mx-auto max-w-2xl p-6">
        <p className="text-sm uppercase tracking-[0.2em] text-teal">Step 2 of 8</p>
        <h1 className="mt-2 text-2xl font-semibold text-navy">Verify email and phone</h1>
        <p className="mt-2 text-stone-600">
          Both must be verified before you can continue to KYC and the loan application.
        </p>

        <div className="mt-8 space-y-4">
          <EmailVerifyCard />
          <PhoneVerifyCard />
        </div>
      </main>
    </div>
  );
}

function EmailVerifyCard() {
  const { user, updateUser } = useAuth();
  const [email, setEmail] = useState(user?.email ?? "");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!user) {
    return null;
  }
  if (user.emailVerified) {
    return (
      <section className="rounded-xl border border-teal/30 bg-white p-5">
        <p className="text-sm font-medium text-teal">Email verified</p>
        <p className="mt-1 text-navy">{user.email}</p>
      </section>
    );
  }

  const send = async () => {
    setError(null);
    setBusy(true);
    try {
      await authApi.sendEmailVerification(email || undefined);
      const me = await authApi.me();
      updateUser(me);
      setOtpSent(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not send the email code.");
    } finally {
      setBusy(false);
    }
  };

  const confirm = async () => {
    setError(null);
    setBusy(true);
    try {
      const next = await authApi.confirmEmailVerification(otp);
      updateUser(next);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not verify the email code.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="rounded-xl border border-stone-200 bg-white p-5">
      <p className="text-sm font-medium text-navy">Email verification</p>
      <p className="mt-1 text-sm text-stone-600">We will send a 6-digit code to this address.</p>
      <label className="mt-4 block">
        <span className="text-sm text-navy">Email</span>
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2.5 outline-none focus:border-teal"
          placeholder="you@example.com"
        />
      </label>
      <button
        type="button"
        onClick={send}
        disabled={busy}
        className="mt-3 rounded-lg bg-navy px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
      >
        {otpSent ? "Resend email code" : "Send email code"}
      </button>
      {otpSent && (
        <div className="mt-4">
          <input
            inputMode="numeric"
            maxLength={6}
            value={otp}
            onChange={(event) => setOtp(event.target.value)}
            className="w-full rounded-lg border border-stone-300 px-3 py-2.5 tracking-[0.4em] text-center text-lg outline-none focus:border-teal"
            placeholder="000000"
          />
          <button
            type="button"
            onClick={confirm}
            disabled={busy || otp.length !== 6}
            className="mt-3 w-full rounded-lg bg-teal py-2.5 font-medium text-white disabled:opacity-60"
          >
            Verify email
          </button>
        </div>
      )}
      {error && <p className="mt-3 text-sm text-red-700">{error}</p>}
    </section>
  );
}

function PhoneVerifyCard() {
  const { user, updateUser } = useAuth();
  const [phone, setPhone] = useState(user?.phone ?? "");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!user) {
    return null;
  }
  if (user.phoneVerified) {
    return (
      <section className="rounded-xl border border-teal/30 bg-white p-5">
        <p className="text-sm font-medium text-teal">Phone verified</p>
        <p className="mt-1 text-navy">{user.phone}</p>
      </section>
    );
  }

  const send = async () => {
    setError(null);
    setBusy(true);
    try {
      await authApi.sendPhoneVerification(phone || undefined);
      const me = await authApi.me();
      updateUser(me);
      setOtpSent(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not send the SMS code.");
    } finally {
      setBusy(false);
    }
  };

  const confirm = async () => {
    setError(null);
    setBusy(true);
    try {
      const next = await authApi.confirmPhoneVerification(otp);
      updateUser(next);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not verify the SMS code.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="rounded-xl border border-stone-200 bg-white p-5">
      <p className="text-sm font-medium text-navy">Phone verification</p>
      <p className="mt-1 text-sm text-stone-600">We will send a 6-digit OTP by SMS.</p>
      <label className="mt-4 block">
        <span className="text-sm text-navy">Phone number</span>
        <input
          type="tel"
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2.5 outline-none focus:border-teal"
          placeholder="9876543210"
        />
      </label>
      <button
        type="button"
        onClick={send}
        disabled={busy}
        className="mt-3 rounded-lg bg-navy px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
      >
        {otpSent ? "Resend SMS code" : "Send SMS code"}
      </button>
      {otpSent && (
        <div className="mt-4">
          <input
            inputMode="numeric"
            maxLength={6}
            value={otp}
            onChange={(event) => setOtp(event.target.value)}
            className="w-full rounded-lg border border-stone-300 px-3 py-2.5 tracking-[0.4em] text-center text-lg outline-none focus:border-teal"
            placeholder="000000"
          />
          <button
            type="button"
            onClick={confirm}
            disabled={busy || otp.length !== 6}
            className="mt-3 w-full rounded-lg bg-teal py-2.5 font-medium text-white disabled:opacity-60"
          >
            Verify phone
          </button>
        </div>
      )}
      {error && <p className="mt-3 text-sm text-red-700">{error}</p>}
    </section>
  );
}
