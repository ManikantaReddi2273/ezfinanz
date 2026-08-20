import { useState } from "react";
import { ApiError, authApi } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export function ProfilePage() {
  const { user, updateUser } = useAuth();
  const [fullName, setFullName] = useState(user?.fullName ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  if (!user) {
    return null;
  }

  const save = async () => {
    setSaving(true);
    setError(null);
    setSaved(false);
    try {
      updateUser(await authApi.updateProfile(fullName));
      setSaved(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not update profile.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="max-w-xl rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">Profile</h2>
      <p className="mt-1 text-sm text-slate-500">This name appears on your application and for admin review.</p>
      <label className="mt-5 block text-sm font-medium text-slate-700">
        Full name
        <input
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          className="mt-1.5 w-full rounded-lg border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-blue-600"
        />
      </label>
      <dl className="mt-5 space-y-2 text-sm">
        <div className="flex justify-between gap-3">
          <dt className="text-slate-500">Email</dt>
          <dd className="font-medium">{user.email || "—"} {user.emailVerified ? "(verified)" : "(not verified)"}</dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-slate-500">Phone</dt>
          <dd className="font-medium">{user.phone || "—"} {user.phoneVerified ? "(verified)" : "(not verified)"}</dd>
        </div>
      </dl>
      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      {saved && <p className="mt-3 text-sm text-emerald-700">Profile saved.</p>}
      <button
        type="button"
        disabled={saving || fullName.trim().length < 2}
        onClick={() => void save()}
        className="mt-5 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
      >
        {saving ? "Saving…" : "Save changes"}
      </button>
    </section>
  );
}
