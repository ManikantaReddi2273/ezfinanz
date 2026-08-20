import { FileText } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiError, authApi, declarationApi, type DeclarationStatus } from "../api/client";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { useAuth } from "../auth/AuthContext";
import { FlowCard, flowPrimary } from "./FlowCard";

export function DeclarationStep({ onContinue, readOnly }: { onContinue: () => void; readOnly?: boolean }) {
  const { updateUser } = useAuth();
  const [status, setStatus] = useState<DeclarationStatus | null>(null);
  const [accurate, setAccurate] = useState(false);
  const [authorise, setAuthorise] = useState(false);
  const [terms, setTerms] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  useEffect(() => {
    declarationApi
      .get()
      .then((row) => {
        setStatus(row);
        if (row.accepted) {
          setAccurate(true);
          setAuthorise(true);
          setTerms(true);
        }
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : "Could not load the declaration.");
      });
  }, []);

  const accepted = accurate && authorise && terms;

  const confirm = async () => {
    if (!accepted) {
      setError("Please tick all three boxes to continue.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const row = await declarationApi.accept();
      setStatus(row);
      updateUser(await authApi.me());
      onContinue();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the declaration.");
    } finally {
      setSaving(false);
      setConfirmOpen(false);
    }
  };

  return (
    <FlowCard>
      <ConfirmDialog
        open={confirmOpen}
        title="Accept declaration?"
        message="Confirm that your information is accurate and you agree to the terms and conditions."
        confirmLabel="Agree & Continue"
        tone="blue"
        busy={saving}
        onConfirm={() => void confirm()}
        onCancel={() => setConfirmOpen(false)}
      />
      <div className="flex justify-center gap-2">
        <FileText className="h-12 w-10 text-blue-600" />
        <FileText className="h-12 w-10 text-blue-500" />
      </div>
      <div className="mt-5 space-y-3 text-sm text-slate-700">
        <label className="flex items-start gap-2">
          <input type="checkbox" className="mt-1 accent-blue-600" checked={accurate} disabled={readOnly} onChange={(event) => setAccurate(event.target.checked)} />
          <span>I confirm that all the above information is true and accurate.</span>
        </label>
        <label className="flex items-start gap-2">
          <input type="checkbox" className="mt-1 accent-blue-600" checked={authorise} disabled={readOnly} onChange={(event) => setAuthorise(event.target.checked)} />
          <span>I authorise EZFINANZ to verify my identity, credit, bank, and PAN details.</span>
        </label>
        <label className="flex items-start gap-2">
          <input type="checkbox" className="mt-1 accent-blue-600" checked={terms} disabled={readOnly} onChange={(event) => setTerms(event.target.checked)} />
          <span>
            I agree to the <span className="font-semibold text-blue-700">terms and conditions</span> and{" "}
            <span className="font-semibold text-blue-700">privacy policy</span>.
          </span>
        </label>
      </div>
      {status?.accepted && <p className="mt-3 text-xs text-emerald-700">Already accepted{status.acceptedAt ? ` on ${new Date(status.acceptedAt).toLocaleString()}` : ""}.</p>}
      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      {!readOnly && (
        <button type="button" disabled={saving || !accepted} onClick={() => setConfirmOpen(true)} className={`${flowPrimary} mt-5`}>
          {saving ? "Saving…" : "Agree & Continue"}
        </button>
      )}
    </FlowCard>
  );
}
