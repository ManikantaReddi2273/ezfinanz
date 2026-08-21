/**
 * Application step: income/credit eligibility assessment form and results.
 */
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { ApiError, authApi, eligibilityApi, type EligibilityAssessment, type IncomeType } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { rupee } from "../lib/money";
import { CreditScoreGauge } from "./CreditScoreGauge";
import { FlowCard, flowInput, flowPrimary } from "./FlowCard";

type EligibilityForm = {
  incomeType: IncomeType;
  incomeAmount: string;
  requestedLoanAmount: string;
  creditScore: string;
  outstandingDebts: string;
  employerName: string;
  designation: string;
};

/** Runs or displays eligibility; unlocks EMI when result is eligible/partial. */
export function EligibilityStep({ onContinue, readOnly }: { onContinue: () => void; readOnly?: boolean }) {
  const { updateUser } = useAuth();
  const [existing, setExisting] = useState<EligibilityAssessment | null>(null);
  const [showForm, setShowForm] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, reset, formState } = useForm<EligibilityForm>({
    defaultValues: {
      incomeType: "MONTHLY",
      incomeAmount: "",
      requestedLoanAmount: "",
      creditScore: "",
      outstandingDebts: "0",
      employerName: "",
      designation: "",
    },
  });

  useEffect(() => {
    eligibilityApi
      .get()
      .then((row) => {
        setExisting(row);
        setShowForm(false);
        reset({
          incomeType: row.incomeType,
          incomeAmount: String(row.incomeAmount),
          requestedLoanAmount: String(row.requestedLoanAmount),
          creditScore: String(row.creditScore),
          outstandingDebts: String(row.outstandingDebts),
          employerName: row.employerName,
          designation: row.designation,
        });
      })
      .catch((err) => {
        if (!(err instanceof ApiError) || err.status !== 404) {
          setError(err instanceof ApiError ? err.message : "Could not load eligibility.");
        }
      });
  }, [reset]);

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      const row = await eligibilityApi.assess({
        incomeType: values.incomeType,
        incomeAmount: Number(values.incomeAmount),
        requestedLoanAmount: Number(values.requestedLoanAmount),
        creditScore: Number(values.creditScore),
        outstandingDebts: Number(values.outstandingDebts || 0),
        employerName: values.employerName,
        designation: values.designation,
      });
      setExisting(row);
      setShowForm(false);
      updateUser(await authApi.me());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not check eligibility.");
    }
  });

  const passed = existing?.result === "ELIGIBLE" || existing?.result === "PARTIALLY_ELIGIBLE";

  if (existing && (readOnly || !showForm)) {
    return (
      <FlowCard>
        <CreditScoreGauge score={existing.creditScore} band={existing.creditBand} />
        <p className="mt-2 text-center text-sm font-semibold text-slate-900">Eligibility Result</p>
        <p className="mt-2 text-center text-sm text-slate-700">
          You are{" "}
          <span className="font-bold text-emerald-600">
            {existing.result === "ELIGIBLE" ? "Eligible" : existing.result === "PARTIALLY_ELIGIBLE" ? "Partially Eligible" : "Not Eligible"}
          </span>{" "}
          for the loan {passed ? "🥳" : ""}.
        </p>
        <p className="mt-1 text-center text-xs text-slate-500">
          Approved up to {rupee.format(existing.maxEligibleAmount)}. DTI {existing.dtiPercent}%.
        </p>
        <ul className="mt-3 space-y-1 text-center text-xs text-slate-500">
          {existing.reasons.slice(0, 3).map((reason) => (
            <li key={reason}>{reason}</li>
          ))}
        </ul>
        {passed && !readOnly ? (
          <button type="button" onClick={onContinue} className={`${flowPrimary} mt-5`}>
            Continue
          </button>
        ) : null}
        {!passed && !readOnly && (
          <button type="button" onClick={() => setShowForm(true)} className={`${flowPrimary} mt-5`}>
            Update details
          </button>
        )}
        {passed && !readOnly && (
          <button type="button" onClick={() => setShowForm(true)} className="mt-3 w-full text-sm font-semibold text-blue-700">
            Recheck eligibility
          </button>
        )}
      </FlowCard>
    );
  }

  return (
    <FlowCard wide>
      <form onSubmit={onSubmit} className="space-y-3">
        <label className="block text-sm font-medium text-slate-700">
          Income type
          <select className={flowInput} {...register("incomeType", { required: true })}>
            <option value="MONTHLY">Monthly</option>
            <option value="ANNUAL">Annual</option>
          </select>
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Income amount (₹)
          <input type="number" min={1} className={flowInput} {...register("incomeAmount", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Requested loan amount (₹)
          <input type="number" min={25000} max={1500000} className={flowInput} {...register("requestedLoanAmount", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          CIBIL / credit score
          <input type="number" min={300} max={900} className={flowInput} {...register("creditScore", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Current debts (₹)
          <input type="number" min={0} className={flowInput} {...register("outstandingDebts", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Employer name
          <input className={flowInput} {...register("employerName", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Designation
          <input className={flowInput} {...register("designation", { required: true })} />
        </label>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button type="submit" disabled={formState.isSubmitting} className={flowPrimary}>
          {formState.isSubmitting ? "Checking…" : "Check eligibility"}
        </button>
      </form>
    </FlowCard>
  );
}
