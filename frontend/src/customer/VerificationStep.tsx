import { useEffect, useState } from "react";
import { ApiError, authApi } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { CheckCircle2, Mail, Smartphone } from "lucide-react";
import { FlowCard, flowGhost, flowInput, flowPrimary } from "./FlowCard";
import { OtpBoxes } from "./OtpBoxes";

export function VerificationStep({ onContinue, readOnly }: { onContinue: () => void; readOnly?: boolean }) {
  const { user } = useAuth();
  if (!user) {
    return null;
  }
  if (user.emailVerified && user.phoneVerified) {
    return (
      <FlowCard>
        <div className="text-center">
          <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-500" />
          <p className="mt-3 text-lg font-semibold text-slate-900">Email and phone verified</p>
          <p className="mt-3 text-sm text-slate-600">{user.email}</p>
          <p className="text-sm text-slate-600">{user.phone}</p>
          {!readOnly && (
            <button type="button" onClick={onContinue} className={`${flowPrimary} mt-6`}>
              Continue
            </button>
          )}
        </div>
      </FlowCard>
    );
  }
  if (!user.emailVerified) {
    return <EmailVerifyCard />;
  }
  return <PhoneVerifyCard />;
}

function EmailVerifyCard() {
  const { user, updateUser } = useAuth();
  const [email, setEmail] = useState(user?.email ?? "");
  const [otp, setOtp] = useState("");
  const [changing, setChanging] = useState(!user?.email);
  const [otpSent, setOtpSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const send = async () => {
    setError(null);
    setBusy(true);
    try {
      await authApi.sendEmailVerification(email || undefined);
      updateUser(await authApi.me());
      setOtpSent(true);
      setChanging(false);
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
      updateUser(await authApi.confirmEmailVerification(otp));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not verify the email code.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <FlowCard>
      <div className="flex justify-center">
        <span className="flex h-16 w-16 items-center justify-center rounded-full bg-blue-50">
          <Mail className="h-8 w-8 text-blue-600" />
        </span>
      </div>
      <p className="mt-4 text-center text-sm leading-6 text-slate-600">
        We have sent a verification code to{" "}
        <span className="font-semibold text-slate-900">{user?.email || email || "your email"}</span>. Please
        check your inbox and enter the 6-digit code.
      </p>
      {changing && (
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="you@example.com"
          className={`${flowInput} mt-4`}
        />
      )}
      {otpSent && !changing && (
        <div className="mt-5">
          <OtpBoxes value={otp} onChange={setOtp} />
          <button type="button" disabled={busy || otp.length !== 6} onClick={() => void confirm()} className={`${flowPrimary} mt-5`}>
            {busy ? "Verifying…" : "Verify email"}
          </button>
        </div>
      )}
      <button type="button" disabled={busy} onClick={() => void send()} className={`${flowGhost} mt-3`}>
        {otpSent ? "Resend Email" : "Send Email"}
      </button>
      <button type="button" className="mt-3 w-full text-sm font-semibold text-blue-700" onClick={() => setChanging(true)}>
        Change Email
      </button>
      {error && <p className="mt-3 text-center text-sm text-red-600">{error}</p>}
    </FlowCard>
  );
}

function PhoneVerifyCard() {
  const { user, updateUser } = useAuth();
  const [phone, setPhone] = useState(user?.phone ?? "");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (seconds <= 0) {
      return;
    }
    const handle = window.setTimeout(() => setSeconds((value) => value - 1), 1000);
    return () => window.clearTimeout(handle);
  }, [seconds]);

  const send = async () => {
    setError(null);
    setBusy(true);
    try {
      await authApi.sendPhoneVerification(phone || undefined);
      updateUser(await authApi.me());
      setOtpSent(true);
      setSeconds(30);
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
      updateUser(await authApi.confirmPhoneVerification(otp));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not verify the SMS code.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <FlowCard>
      <div className="flex justify-center">
        <span className="flex h-16 w-16 items-center justify-center rounded-full bg-blue-50">
          <Smartphone className="h-8 w-8 text-blue-600" />
        </span>
      </div>
      <p className="mt-4 text-center text-sm leading-6 text-slate-600">
        Enter the OTP sent to <span className="font-semibold text-slate-900">{user?.phone || phone || "your phone"}</span>.
      </p>
      {!otpSent && (
        <input
          type="tel"
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          placeholder="9876543210"
          className={`${flowInput} mt-4`}
        />
      )}
      {otpSent && (
        <div className="mt-5">
          <OtpBoxes value={otp} onChange={setOtp} />
        </div>
      )}
      {otpSent && seconds > 0 ? (
        <p className="mt-3 text-center text-sm text-blue-700">Resend OTP in 0:{String(seconds).padStart(2, "0")}</p>
      ) : (
        <button type="button" disabled={busy} onClick={() => void send()} className="mt-3 w-full text-sm font-semibold text-blue-700">
          {otpSent ? "Resend OTP" : "Send OTP"}
        </button>
      )}
      <button
        type="button"
        disabled={busy || (otpSent && otp.length !== 6)}
        onClick={() => (otpSent ? void confirm() : void send())}
        className={`${flowPrimary} mt-4`}
      >
        {busy ? "Please wait…" : otpSent ? "Verify & Continue" : "Send OTP"}
      </button>
      {error && <p className="mt-3 text-center text-sm text-red-600">{error}</p>}
    </FlowCard>
  );
}
