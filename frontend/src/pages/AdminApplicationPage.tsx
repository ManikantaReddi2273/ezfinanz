/**
 * Admin detail view for one application: tabs, selfie review, and disbursement.
 */
import { ArrowLeft, Check, Download, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, adminApi, type AdminApplicationDetail } from "../api/client";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { formatDateTime, rupee, rupeeExact } from "../lib/money";
import { AdminLayout, applicationId, stageBadgeClass } from "./AdminLayout";

type Tab = "summary" | "kyc" | "eligibility" | "emi" | "bank" | "declaration" | "documents";
type PendingAction = "approve" | "reject" | "disburse" | null;

const TABS: { id: Tab; label: string }[] = [
  { id: "summary", label: "Summary" },
  { id: "kyc", label: "KYC Details" },
  { id: "eligibility", label: "Eligibility" },
  { id: "emi", label: "EMI & Terms" },
  { id: "bank", label: "Bank Account" },
  { id: "declaration", label: "Declaration" },
  { id: "documents", label: "Documents & Selfie" },
];

/** Loads and reviews a single applicant by `:userId` route param. */
export function AdminApplicationPage() {
  const { userId } = useParams();
  const id = Number(userId);
  const [detail, setDetail] = useState<AdminApplicationDetail | null>(null);
  const [selfieUrl, setSelfieUrl] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [tab, setTab] = useState<Tab>("documents");
  const [zoom, setZoom] = useState(false);
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);

  const load = async () => {
    const row = await adminApi.get(id);
    setDetail(row);
    if (row.selfie) {
      try {
        const blob = await adminApi.selfieBlob(id);
        setSelfieUrl(URL.createObjectURL(blob));
      } catch {
        setSelfieUrl(null);
      }
    }
  };

  useEffect(() => {
    if (!Number.isFinite(id)) {
      return;
    }
    load().catch((err) => setError(err instanceof ApiError ? err.message : "Could not load application."));
  }, [id]);

  const run = async (action: () => Promise<AdminApplicationDetail>) => {
    setBusy(true);
    setError(null);
    try {
      const row = await action();
      setDetail(row);
      if (row.selfie) {
        const blob = await adminApi.selfieBlob(id);
        setSelfieUrl(URL.createObjectURL(blob));
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Action failed.");
    } finally {
      setBusy(false);
      setPendingAction(null);
    }
  };

  if (!detail) {
    return (
      <AdminLayout title="Application Detail">
        <p className="text-sm text-slate-600">{error || "Loading application…"}</p>
      </AdminLayout>
    );
  }

  const pending = detail.selfie?.reviewStatus === "PENDING";
  const approved = detail.selfie?.reviewStatus === "APPROVED" && !detail.selfie.disbursed;
  const stageLabel =
    detail.currentStage === "WAITING_FOR_ADMIN_REVIEW" || detail.currentStage === "LIVE_SELFIE"
      ? "Selfie Pending"
      : detail.currentStageLabel;

  const downloadSelfie = async () => {
    const blob = await adminApi.selfieBlob(id);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${applicationId(id)}-selfie.jpg`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <AdminLayout title="Application Detail (Admin View)">
      <ConfirmDialog
        open={pendingAction === "approve"}
        title="Approve application?"
        message={`Approve this application and notify ${detail.email || "the applicant"} by email?`}
        confirmLabel="Approve"
        tone="green"
        busy={busy}
        onConfirm={() => void run(() => adminApi.approveSelfie(id, message.trim() || undefined))}
        onCancel={() => setPendingAction(null)}
      />
      <ConfirmDialog
        open={pendingAction === "reject"}
        title="Reject application?"
        message={`Reject this application and notify ${detail.email || "the applicant"} by email?`}
        confirmLabel="Reject"
        tone="red"
        busy={busy}
        onConfirm={() => void run(() => adminApi.rejectSelfie(id, message.trim() || undefined))}
        onCancel={() => setPendingAction(null)}
      />
      <ConfirmDialog
        open={pendingAction === "disburse"}
        title="Confirm disbursement?"
        message="Mark this loan as disbursed to the applicant's registered bank account?"
        confirmLabel="Confirm disbursement"
        tone="blue"
        busy={busy}
        onConfirm={() => void run(() => adminApi.disburse(id))}
        onCancel={() => setPendingAction(null)}
      />

      <Link to="/admin/applications" className="mb-4 inline-flex items-center gap-1 text-sm font-semibold text-blue-700">
        <ArrowLeft className="h-4 w-4" /> Back to Applications
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">{detail.applicantName || "Applicant"}</h2>
          <p className="mt-1 text-sm text-slate-500">
            Application ID: {applicationId(detail.userId)}{" "}
            <span className={`ml-2 rounded-md px-2.5 py-1 text-xs font-semibold ${stageBadgeClass(detail.currentStage)}`}>
              {stageLabel}
            </span>
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {pending && (
            <>
              <button
                type="button"
                disabled={busy}
                onClick={() => setPendingAction("approve")}
                className="rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-60"
              >
                Approve Application
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={() => setPendingAction("reject")}
                className="rounded-lg border border-rose-400 px-4 py-2.5 text-sm font-semibold text-rose-600 hover:bg-rose-50 disabled:opacity-60"
              >
                Reject Application
              </button>
            </>
          )}
          {approved && (
            <button
              type="button"
              disabled={busy}
              onClick={() => setPendingAction("disburse")}
              className="rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
            >
              Confirm disbursement
            </button>
          )}
        </div>
      </div>

      {pending && (
        <div className="mt-4 max-w-xl">
          <textarea
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            placeholder="Optional message to the applicant (sent by email on approve or reject)"
            rows={3}
            className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-600"
          />
        </div>
      )}
      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

      <div className="mt-6 flex gap-5 overflow-x-auto border-b border-slate-200 text-sm">
        {TABS.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => setTab(item.id)}
            className={`whitespace-nowrap pb-3 font-medium ${
              tab === item.id ? "border-b-2 border-blue-600 text-blue-700" : "text-slate-500"
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {tab === "summary" && (
          <Card>
            <dl className="grid gap-3 text-sm sm:grid-cols-2">
              <Row label="Email" value={`${detail.email || "—"} (${detail.emailVerified ? "verified" : "not verified"})`} />
              <Row label="Phone" value={`${detail.phone || "—"} (${detail.phoneVerified ? "verified" : "not verified"})`} />
              <Row label="Stage" value={detail.currentStageLabel} />
              <Row label="Loan amount" value={detail.emi ? rupee.format(detail.emi.principal) : "—"} />
              <Row label="Tenure" value={detail.emi ? `${detail.emi.tenureMonths} months` : "—"} />
              <Row label="Selfie" value={detail.selfie?.reviewStatus || "Not submitted"} />
            </dl>
          </Card>
        )}

        {tab === "kyc" && (
          <Card title="KYC Details">
            {detail.kyc ? (
              <dl className="grid gap-3 text-sm sm:grid-cols-2">
                <Row label="Full name" value={detail.kyc.fullName} />
                <Row label="Date of birth" value={`${detail.kyc.dateOfBirth} (age ${detail.kyc.age})`} />
                <Row label="Gender" value={detail.kyc.gender} />
                <Row label="ID" value={`${detail.kyc.idType} · ${detail.kyc.idNumber}`} />
                <Row
                  label="Address"
                  value={`${detail.kyc.addressLine}, ${detail.kyc.city}, ${detail.kyc.state} ${detail.kyc.pincode}`}
                />
              </dl>
            ) : (
              <p className="text-sm text-slate-500">KYC not submitted.</p>
            )}
            {detail.kyc?.hasDocument && (
              <button
                type="button"
                className="mt-4 text-sm font-semibold text-blue-700"
                onClick={async () => {
                  const blob = await adminApi.kycDocumentBlob(id);
                  window.open(URL.createObjectURL(blob), "_blank", "noopener,noreferrer");
                }}
              >
                View ID document
              </button>
            )}
          </Card>
        )}

        {tab === "eligibility" && (
          <Card title="Eligibility">
            {detail.eligibility ? (
              <dl className="grid gap-3 text-sm sm:grid-cols-2">
                <Row label="Result" value={detail.eligibility.result.replace(/_/g, " ")} />
                <Row label="Credit score" value={`${detail.eligibility.creditScore} (${detail.eligibility.creditBand})`} />
                <Row label="DTI" value={`${detail.eligibility.dtiPercent}%`} />
                <Row label="Requested" value={rupee.format(detail.eligibility.requestedLoanAmount)} />
                <Row label="Max eligible" value={rupee.format(detail.eligibility.maxEligibleAmount)} />
                <Row label="Employer" value={`${detail.eligibility.employerName} · ${detail.eligibility.designation}`} />
              </dl>
            ) : (
              <p className="text-sm text-slate-500">Eligibility not checked.</p>
            )}
          </Card>
        )}

        {tab === "emi" && (
          <Card title="EMI & Terms">
            {detail.emi ? (
              <dl className="grid gap-3 text-sm sm:grid-cols-2">
                <Row label="Amount" value={rupee.format(detail.emi.principal)} />
                <Row label="Tenure" value={`${detail.emi.tenureMonths} months`} />
                <Row label="Rate" value={`${detail.emi.annualInterestPercent}%`} />
                <Row label="Monthly EMI" value={rupeeExact.format(detail.emi.monthlyEmi)} />
                <Row label="IRR" value={`${detail.emi.irrPercent}%`} />
                <Row label="Net disbursement" value={rupeeExact.format(detail.emi.netDisbursement)} />
              </dl>
            ) : (
              <p className="text-sm text-slate-500">EMI terms not selected.</p>
            )}
          </Card>
        )}

        {tab === "bank" && (
          <Card title="Bank Account">
            {detail.bankAccount ? (
              <dl className="grid gap-3 text-sm sm:grid-cols-2">
                <Row label="Holder" value={detail.bankAccount.accountHolderName} />
                <Row label="Bank" value={detail.bankAccount.bankName} />
                <Row label="Account" value={detail.bankAccount.accountNumberMasked} />
                <Row label="IFSC" value={detail.bankAccount.ifscCode} />
              </dl>
            ) : (
              <p className="text-sm text-slate-500">Bank account not added.</p>
            )}
          </Card>
        )}

        {tab === "declaration" && (
          <Card title="Declaration">
            {detail.declaration?.accepted ? (
              <p className="text-sm text-slate-700">
                Accepted · version {detail.declaration.termsVersion}
                {detail.declaration.acceptedAt ? ` · ${formatDateTime(detail.declaration.acceptedAt)}` : ""}
              </p>
            ) : (
              <p className="text-sm text-slate-500">Declaration not accepted.</p>
            )}
          </Card>
        )}

        {tab === "documents" && (
          <div className="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
            <Card title="Selfie / Photo">
              {selfieUrl ? (
                <img src={selfieUrl} alt="Applicant selfie" className="w-full rounded-xl object-cover" />
              ) : (
                <p className="text-sm text-slate-500">Selfie not submitted.</p>
              )}
              {selfieUrl && (
                <div className="mt-4 grid grid-cols-2 gap-3">
                  <button type="button" onClick={() => void downloadSelfie()} className="flex items-center justify-center gap-2 rounded-lg border border-slate-200 py-2 text-sm font-medium text-slate-700">
                    <Download className="h-4 w-4" /> Download
                  </button>
                  <button type="button" onClick={() => setZoom(true)} className="flex items-center justify-center gap-2 rounded-lg border border-slate-200 py-2 text-sm font-medium text-slate-700">
                    <Search className="h-4 w-4" /> Zoom
                  </button>
                </div>
              )}
              {detail.kyc?.hasDocument && (
                <button
                  type="button"
                  className="mt-3 text-sm font-semibold text-blue-700"
                  onClick={async () => {
                    const blob = await adminApi.kycDocumentBlob(id);
                    window.open(URL.createObjectURL(blob), "_blank", "noopener,noreferrer");
                  }}
                >
                  Open KYC ID document
                </button>
              )}
              {detail.selfie?.disbursed && <p className="mt-3 font-medium text-emerald-800">Disbursement confirmed.</p>}
            </Card>
            <div className="space-y-5">
              <Card>
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-900">Customer Declaration</h3>
                  <span
                    className={`rounded-md px-2 py-1 text-xs font-semibold ${
                      detail.declaration?.accepted ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"
                    }`}
                  >
                    {detail.declaration?.accepted ? "Confirmed" : "Pending"}
                  </span>
                </div>
                <div className="mt-4 flex items-center gap-3">
                  {selfieUrl && <img src={selfieUrl} alt="" className="h-10 w-10 rounded-full object-cover" />}
                  <div className="text-sm">
                    <p className="font-semibold text-slate-900">{detail.applicantName}</p>
                    <p className="text-slate-500">
                      {detail.declaration?.accepted
                        ? `Customer has accepted the declaration${detail.declaration.acceptedAt ? ` on ${formatDateTime(detail.declaration.acceptedAt)}` : ""}.`
                        : "Customer has not accepted the declaration yet."}
                    </p>
                  </div>
                </div>
              </Card>
              <Card title="Verification Logs">
                <ol className="space-y-4">
                  <LogItem
                    title="Application Submitted"
                    time={detail.kyc?.submittedAt}
                    tone="blue"
                    done
                  />
                  <LogItem
                    title="Selfie Captured"
                    time={detail.selfie?.submittedAt}
                    tone="green"
                    done={Boolean(detail.selfie)}
                  />
                  <LogItem
                    title="Waiting for Admin Review"
                    time={detail.selfie?.submittedAt}
                    tone="orange"
                    done={detail.selfie?.reviewStatus === "PENDING"}
                  />
                </ol>
              </Card>
            </div>
          </div>
        )}
      </div>

      {zoom && selfieUrl && (
        <button type="button" className="fixed inset-0 z-40 bg-slate-900/70 p-8" onClick={() => setZoom(false)}>
          <img src={selfieUrl} alt="Zoomed selfie" className="mx-auto max-h-full max-w-full rounded-xl object-contain" />
        </button>
      )}
    </AdminLayout>
  );
}

function Card({ title, children }: { title?: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl bg-white p-5 shadow-sm">
      {title && <h3 className="mb-4 text-sm font-semibold text-slate-900">{title}</h3>}
      {children}
    </section>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-slate-500">{label}</dt>
      <dd className="mt-0.5 font-medium text-slate-900">{value}</dd>
    </div>
  );
}

function LogItem({
  title,
  time,
  tone,
  done,
}: {
  title: string;
  time?: string | null;
  tone: "blue" | "green" | "orange";
  done?: boolean;
}) {
  const color = tone === "green" ? "text-emerald-600" : tone === "orange" ? "text-orange-500" : "text-blue-600";
  const ring = tone === "green" ? "bg-emerald-500" : tone === "orange" ? "border-2 border-orange-400 bg-white" : "bg-blue-600";
  return (
    <li className="flex gap-3">
      <span className={`mt-0.5 flex h-5 w-5 items-center justify-center rounded-full ${ring}`}>
        {tone === "green" && done && <Check className="h-3 w-3 text-white" />}
      </span>
      <div>
        <p className="text-sm font-medium text-slate-800">{title}</p>
        <p className={`text-xs ${color}`}>{formatDateTime(time)}</p>
      </div>
    </li>
  );
}
