import { Pencil } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { ApiError, authApi, emiApi, type EmiQuote } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { rupee, rupeeExact } from "../lib/money";
import { flowPrimary } from "./FlowCard";

export function EmiTermsPanel({ onContinue, readOnly }: { onContinue: () => void; readOnly?: boolean }) {
  const { updateUser } = useAuth();
  const [quote, setQuote] = useState<EmiQuote | null>(null);
  const [amount, setAmount] = useState<number | null>(null);
  const [tenure, setTenure] = useState(24);
  const [editingAmount, setEditingAmount] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [ready, setReady] = useState(false);
  const skipQuote = useRef(true);

  useEffect(() => {
    let cancelled = false;
    emiApi
      .get()
      .then((row) => {
        if (cancelled) {
          return;
        }
        setQuote(row);
        setAmount(row.principal);
        setTenure(row.tenureMonths);
        setSaved(true);
        skipQuote.current = true;
        setReady(true);
      })
      .catch(async (err) => {
        if (cancelled) {
          return;
        }
        if (!(err instanceof ApiError) || err.status !== 404) {
          setError(err instanceof ApiError ? err.message : "Could not load EMI terms.");
          return;
        }
        try {
          const initial = await emiApi.quote(undefined, 24);
          if (!cancelled) {
            setQuote(initial);
            setAmount(initial.principal);
            setTenure(initial.tenureMonths);
            skipQuote.current = true;
            setReady(true);
          }
        } catch (quoteErr) {
          if (!cancelled) {
            setError(quoteErr instanceof ApiError ? quoteErr.message : "Could not calculate EMI.");
          }
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!ready || amount == null) {
      return;
    }
    if (skipQuote.current) {
      skipQuote.current = false;
      return;
    }
    const handle = window.setTimeout(() => {
      emiApi
        .quote(amount, tenure)
        .then((row) => {
          setQuote(row);
          setError(null);
          setSaved(false);
        })
        .catch((err) => {
          setError(err instanceof ApiError ? err.message : "Could not calculate EMI.");
        });
    }, 220);
    return () => window.clearTimeout(handle);
  }, [amount, tenure, ready]);

  const min = quote?.minAmount ?? 25000;
  const max = quote?.maxAmount ?? 25000;
  const currentAmount = amount ?? min;

  const confirm = async () => {
    if (amount == null) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const row = await emiApi.save(amount, tenure);
      setQuote(row);
      setSaved(true);
      updateUser(await authApi.me());
      onContinue();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save EMI terms.");
    } finally {
      setSaving(false);
    }
  };

  if (!quote) {
    return <p className="text-sm text-slate-500">{error || "Loading EMI terms…"}</p>;
  }

  const otherCharges = quote.otherCharges + quote.documentationFee + quote.stampDuty;

  return (
    <div>
      <dl className="space-y-2 text-sm">
        <div className="flex items-center justify-between gap-3">
          <dt className="text-slate-500">Loan Amount</dt>
          <dd className="flex items-center gap-2 font-semibold text-slate-900">
            {editingAmount && !readOnly ? (
              <input
                type="number"
                min={min}
                max={max}
                step={1000}
                value={currentAmount}
                onChange={(event) => setAmount(Number(event.target.value))}
                onBlur={() => setEditingAmount(false)}
                className="w-28 rounded border border-blue-200 px-2 py-1 text-right text-sm"
              />
            ) : (
              <>
                {rupee.format(quote.principal)}
                {!readOnly && (
                  <button type="button" onClick={() => setEditingAmount(true)} className="text-blue-600">
                    <Pencil className="h-3.5 w-3.5" />
                  </button>
                )}
              </>
            )}
          </dd>
        </div>
        <Row label="Interest Rate (p.a)" value={`${quote.annualInterestPercent}%`} />
        <Row label="Processing Fee" value={rupeeExact.format(quote.processingFee)} />
        <Row label="GST (18%)" value={rupeeExact.format(quote.gstOnProcessingFee)} />
        <Row label="Other Charges" value={rupeeExact.format(otherCharges)} />
      </dl>
      <div className="mt-4 flex justify-center gap-2">
        {[12, 18, 24, 36].map((months) => (
          <button
            key={months}
            type="button"
            disabled={readOnly}
            onClick={() => setTenure(months)}
            className={`rounded-lg px-3 py-1.5 text-sm font-semibold ${
              tenure === months ? "bg-blue-600 text-white" : "border border-slate-200 bg-white text-slate-600"
            } disabled:cursor-not-allowed`}
          >
            {months}
          </button>
        ))}
      </div>
      <p className="mt-1 text-center text-[11px] text-slate-400">months</p>
      <dl className="mt-4 space-y-2 border-t border-slate-100 pt-3 text-sm">
        <Row label="Monthly EMI" value={rupee.format(quote.monthlyEmi)} strong />
        <Row label="Total Interest" value={rupeeExact.format(quote.totalInterest)} />
        <Row label="Total Repayment" value={rupeeExact.format(quote.totalRepayment)} />
        <Row label="Total Charges" value={rupeeExact.format(quote.totalCharges)} />
        <Row label="Net Disbursement" value={rupeeExact.format(quote.netDisbursement)} strong />
        <Row label="IRR (Approx)" value={`${quote.irrPercent}%`} />
      </dl>
      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      {!readOnly && (
        <button type="button" disabled={saving} onClick={() => (saved ? onContinue() : void confirm())} className={`${flowPrimary} mt-5`}>
          {saving ? "Saving…" : "Continue"}
        </button>
      )}
    </div>
  );
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <dt className="text-slate-500">{label}</dt>
      <dd className={strong ? "font-semibold text-slate-900" : "font-medium text-slate-800"}>{value}</dd>
    </div>
  );
}
