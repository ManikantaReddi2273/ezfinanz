/**
 * Application step: capture KYC identity, address, and optional ID document.
 */
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { ApiError, authApi, kycApi, type Gender, type IdType, type KycProfile } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { FileText } from "lucide-react";
import { FlowCard, flowInput, flowPrimary } from "./FlowCard";

type KycForm = {
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  addressLine: string;
  city: string;
  state: string;
  pincode: string;
  idType: IdType;
  idNumber: string;
};

/** KYC form step; `readOnly` shows saved profile without edits. */
export function KycStep({ onContinue, readOnly }: { onContinue?: () => void; readOnly?: boolean }) {
  const { user, updateUser } = useAuth();
  const [existing, setExisting] = useState<KycProfile | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, reset, formState } = useForm<KycForm>({
    defaultValues: {
      fullName: user?.fullName ?? "",
      dateOfBirth: "",
      gender: "MALE",
      addressLine: "",
      city: "",
      state: "",
      pincode: "",
      idType: "PAN",
      idNumber: "",
    },
  });

  useEffect(() => {
    kycApi
      .get()
      .then((profile) => {
        setExisting(profile);
        reset({
          fullName: profile.fullName,
          dateOfBirth: profile.dateOfBirth,
          gender: profile.gender,
          addressLine: profile.addressLine,
          city: profile.city,
          state: profile.state,
          pincode: profile.pincode,
          idType: profile.idType,
          idNumber: profile.idNumber,
        });
      })
      .catch((err) => {
        if (!(err instanceof ApiError) || err.status !== 404) {
          setError(err instanceof ApiError ? err.message : "Could not load KYC.");
        }
      });
  }, [reset]);

  const onSubmit = handleSubmit(async (values) => {
    if (readOnly) {
      return;
    }
    setError(null);
    const form = new FormData();
    Object.entries(values).forEach(([key, value]) => form.append(key, value));
    if (file) {
      form.append("document", file);
    }
    try {
      const profile = await kycApi.save(form);
      setExisting(profile);
      updateUser(await authApi.me());
      onContinue?.();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save KYC.");
    }
  });

  const fileLabel = file?.name || existing?.documentFileName;

  return (
    <FlowCard wide>
      <form onSubmit={onSubmit} className="space-y-3">
        <label className="block text-sm font-medium text-slate-700">
          Full Name
          <input className={flowInput} disabled={readOnly} {...register("fullName", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Date of Birth
          <input type="date" className={flowInput} disabled={readOnly} {...register("dateOfBirth", { required: true })} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Gender
          <select className={flowInput} disabled={readOnly} {...register("gender", { required: true })}>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
            <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
          </select>
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Address
          <textarea rows={3} className={flowInput} disabled={readOnly} {...register("addressLine", { required: true })} />
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm font-medium text-slate-700">
            City
            <input className={flowInput} disabled={readOnly} {...register("city", { required: true })} />
          </label>
          <label className="text-sm font-medium text-slate-700">
            State
            <input className={flowInput} disabled={readOnly} {...register("state", { required: true })} />
          </label>
        </div>
        <label className="block text-sm font-medium text-slate-700">
          Pincode
          <input className={flowInput} maxLength={6} disabled={readOnly} {...register("pincode", { required: true })} />
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm font-medium text-slate-700">
            ID type
            <select className={flowInput} disabled={readOnly} {...register("idType", { required: true })}>
              <option value="PAN">PAN</option>
              <option value="AADHAAR">Aadhaar</option>
              <option value="PASSPORT">Passport</option>
              <option value="DRIVING_LICENSE">Driving licence</option>
              <option value="VOTER_ID">Voter ID</option>
            </select>
          </label>
          <label className="text-sm font-medium text-slate-700">
            ID number
            <input className={flowInput} disabled={readOnly} {...register("idNumber", { required: true })} />
          </label>
        </div>
        <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
          <p className="text-sm font-medium text-slate-700">Upload ID proof</p>
          <div className="mt-2 flex items-center justify-between gap-3">
            <span className="flex min-w-0 items-center gap-2 text-sm text-slate-600">
              <FileText className="h-4 w-4 shrink-0 text-blue-600" />
              <span className="truncate">{fileLabel || "No file selected"}</span>
            </span>
            {!readOnly && (
              <label className="cursor-pointer text-sm font-semibold text-blue-700">
                {fileLabel ? "Change File" : "Choose file"}
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp,application/pdf"
                  className="hidden"
                  onChange={(event) => setFile(event.target.files?.[0] ?? null)}
                />
              </label>
            )}
          </div>
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        {!readOnly && (
          <button type="submit" disabled={formState.isSubmitting} className={flowPrimary}>
            {formState.isSubmitting ? "Saving…" : "Save & Continue"}
          </button>
        )}
      </form>
    </FlowCard>
  );
}
