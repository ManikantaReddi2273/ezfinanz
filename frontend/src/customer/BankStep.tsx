import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { ApiError, authApi, bankApi, type BankAccount } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { FlowCard, flowGhost, flowInput, flowPrimary } from "./FlowCard";

type BankForm = {
  accountHolderName: string;
  accountNumber: string;
  confirmAccountNumber: string;
  ifscCode: string;
  bankName: string;
};

export function BankStep({ onContinue, readOnly }: { onContinue: () => void; readOnly?: boolean }) {
  const { user, updateUser } = useAuth();
  const [existing, setExisting] = useState<BankAccount | null>(null);
  const [verified, setVerified] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, reset, watch, getValues, formState } = useForm<BankForm>({
    defaultValues: {
      accountHolderName: user?.fullName ?? "",
      accountNumber: "",
      confirmAccountNumber: "",
      ifscCode: "",
      bankName: "",
    },
  });

  useEffect(() => {
    bankApi
      .get()
      .then((row) => {
        setExisting(row);
        setVerified(true);
        reset({
          accountHolderName: row.accountHolderName,
          accountNumber: row.accountNumber,
          confirmAccountNumber: row.accountNumber,
          ifscCode: row.ifscCode,
          bankName: row.bankName,
        });
      })
      .catch((err) => {
        if (!(err instanceof ApiError) || err.status !== 404) {
          setError(err instanceof ApiError ? err.message : "Could not load bank details.");
        }
      });
  }, [reset]);

  const verifyAccount = () => {
    const values = getValues();
    if (!/^[0-9]{9,18}$/.test(values.accountNumber)) {
      setError("Enter a valid account number (9–18 digits).");
      setVerified(false);
      return;
    }
    if (values.accountNumber !== values.confirmAccountNumber) {
      setError("Account number and confirmation do not match.");
      setVerified(false);
      return;
    }
    if (!/^[A-Z]{4}0[A-Z0-9]{6}$/i.test(values.ifscCode)) {
      setError("Enter a valid IFSC, for example HDFC0001234.");
      setVerified(false);
      return;
    }
    if (!values.accountHolderName.trim() || !values.bankName.trim()) {
      setError("Fill account holder name and bank name.");
      setVerified(false);
      return;
    }
    setError(null);
    setVerified(true);
  };

  const onSubmit = handleSubmit(async (values) => {
    if (values.accountNumber !== values.confirmAccountNumber) {
      setError("Account number and confirmation do not match.");
      return;
    }
    setError(null);
    try {
      const row = await bankApi.save({
        accountHolderName: values.accountHolderName,
        accountNumber: values.accountNumber,
        ifscCode: values.ifscCode.toUpperCase(),
        bankName: values.bankName,
      });
      setExisting(row);
      updateUser(await authApi.me());
      onContinue();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save bank account.");
    }
  });

  return (
    <FlowCard>
      <form onSubmit={onSubmit} className="space-y-3">
        <label className="block text-sm font-medium text-slate-700">
          Account Holder Name
          <input className={flowInput} disabled={readOnly} {...register("accountHolderName", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Account Number
          <input className={flowInput} disabled={readOnly} inputMode="numeric" autoComplete="off" {...register("accountNumber", { required: true, pattern: /^[0-9]{9,18}$/ })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Re-enter Account Number
          <input className={flowInput} disabled={readOnly} inputMode="numeric" autoComplete="off" {...register("confirmAccountNumber", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          IFSC Code
          <input className={`${flowInput} uppercase`} disabled={readOnly} maxLength={11} {...register("ifscCode", { required: true, setValueAs: (value) => String(value).toUpperCase() })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Bank Name
          <input className={flowInput} disabled={readOnly} {...register("bankName", { required: true })} />
        </label>
        {watch("accountNumber") && watch("confirmAccountNumber") && watch("accountNumber") !== watch("confirmAccountNumber") && (
          <p className="text-sm text-red-600">Account numbers do not match.</p>
        )}
        {verified && (
          <p className="text-sm text-emerald-700">
            {existing ? "Saved account details look valid." : "Account details look valid."}
          </p>
        )}
        {error && <p className="text-sm text-red-600">{error}</p>}
        {!readOnly && (
          <>
            <button type="button" onClick={verifyAccount} className={flowGhost}>
              Verify Account
            </button>
            <button type="submit" disabled={formState.isSubmitting} className={flowPrimary}>
              {formState.isSubmitting ? "Saving…" : "Save & Continue"}
            </button>
          </>
        )}
      </form>
    </FlowCard>
  );
}
