import { ChevronRight, CircleHelp, Headphones, Waypoints } from "lucide-react";
import { useEffect, useState } from "react";
import { customerApi, type CustomerDashboardData, type User } from "../api/client";
import { formatDateTime, rupee } from "../lib/money";
import { ApplicationProgress } from "./ApplicationProgress";
import { completedCount, defaultStep, LOAN_STEPS, isApplicationSubmitted, isReadyToSend, type StepId } from "./steps";

export function DashboardHome({
  user,
  onOpenStep,
  onOpenHelp,
}: {
  user: User;
  onOpenStep: (id: StepId) => void;
  onOpenHelp: () => void;
}) {
  const [data, setData] = useState<CustomerDashboardData | null>(null);
  const firstName = user.fullName?.split(" ")[0] || "there";
  const next = defaultStep(user);
  const nextMeta = LOAN_STEPS.find((item) => item.id === next);
  const done = completedCount(user);
  const status = loanStatus(user, data?.statusBadge);

  useEffect(() => {
    customerApi.dashboard().then(setData).catch(() => setData(null));
  }, [user.emiCompleted, user.eligibilityCompleted, user.applicationStage, user.selfieStatus, user.disbursed]);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-900 sm:text-3xl">Welcome back, {firstName} 👋</h2>
        <p className="mt-1 text-sm text-slate-500">Track your loan application journey</p>
      </div>

      <ApplicationProgress user={user} onSelect={onOpenStep} />

      <div className="grid gap-5 xl:grid-cols-[0.9fr_1.2fr_0.85fr]">
        <section className="hover-card-green hover-lift rounded-2xl border border-slate-100 bg-white p-5 shadow-sm transition-all duration-300">
          <h3 className="text-base font-semibold text-slate-900">Application Summary</h3>
          <dl className="mt-4 space-y-3 text-sm">
            <SummaryRow label="Application ID" value={data?.applicationId || `EZF${String(user.id).padStart(9, "0")}`} />
            <SummaryRow
              label="Requested Amount"
              value={data?.requestedAmount != null ? rupee.format(data.requestedAmount) : "—"}
            />
            <SummaryRow label="Tenure" value={data?.tenureMonths ? `${data.tenureMonths} Months` : "—"} />
            <SummaryRow
              label="Monthly EMI (Approx.)"
              value={data?.monthlyEmi != null ? rupee.format(data.monthlyEmi) : "—"}
            />
            <div className="flex items-center justify-between gap-3">
              <dt className="text-slate-500">Status</dt>
              <dd>
                <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${status.badgeClass}`}>
                  {status.label}
                </span>
              </dd>
            </div>
            <SummaryRow label="Last Updated" value={formatDateTime(data?.lastUpdated)} />
          </dl>
        </section>

        <section className="hover-card-blue hover-lift rounded-2xl border border-slate-100 bg-white p-5 shadow-sm transition-all duration-300">
          <h3 className="text-base font-semibold text-slate-900">Loan Status</h3>
          <div className={`mt-4 rounded-xl px-4 py-3 ${status.panelClass}`}>
            <p className="text-xs font-semibold uppercase tracking-wide">{status.toneLabel}</p>
            <p className="mt-1 text-xl font-bold">{status.label}</p>
          </div>
          <dl className="mt-5 space-y-3 text-sm">
            <SummaryRow label="Current step" value={nextMeta?.label || user.applicationStageLabel} />
            <SummaryRow label="Steps completed" value={`${done} of 8`} />
            {data?.monthlyEmi != null && (
              <SummaryRow label="Selected EMI" value={rupee.format(data.monthlyEmi)} />
            )}
            {user.eligibilityResult && (
              <SummaryRow label="Eligibility" value={user.eligibilityResult.replace(/_/g, " ")} />
            )}
            {user.selfieStatus && <SummaryRow label="Selfie review" value={user.selfieStatus} />}
          </dl>
          <p className="mt-4 text-sm text-slate-600">{status.detail}</p>
          <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-slate-100">
            <div className="h-full rounded-full bg-blue-600" style={{ width: `${(done / 8) * 100}%` }} />
          </div>
          {!user.disbursed && !isApplicationSubmitted(user) && (
            <button
              type="button"
              onClick={() => onOpenStep(isReadyToSend(user) ? "selfie" : next)}
              className={`mt-5 w-full rounded-xl py-3 text-sm font-semibold text-white transition-all duration-200 ${
                isReadyToSend(user)
                  ? "btn-hover-green bg-emerald-600 hover:bg-emerald-700"
                  : "btn-hover-blue bg-blue-600 hover:bg-blue-700"
              }`}
            >
              {isReadyToSend(user) ? "Send application" : status.cta}
            </button>
          )}
          {!user.disbursed && isApplicationSubmitted(user) && (
            <button
              type="button"
              onClick={() => onOpenStep(next)}
              className="btn-hover-blue mt-5 w-full rounded-xl bg-blue-600 py-3 text-sm font-semibold text-white transition-all duration-200 hover:bg-blue-700"
            >
              {status.cta}
            </button>
          )}
        </section>

        <section className="hover-card-orange hover-lift rounded-2xl border border-slate-100 bg-white p-5 shadow-sm transition-all duration-300">
          <h3 className="text-base font-semibold text-slate-900">Need Help?</h3>
          <ul className="mt-3 divide-y divide-slate-100">
            <HelpLink label="FAQs" icon={CircleHelp} onClick={onOpenHelp} hoverClass="hover-row-blue" />
            <HelpLink label="How it works?" icon={Waypoints} onClick={onOpenHelp} hoverClass="hover-row-green" />
            <HelpLink label="Contact Support" icon={Headphones} onClick={onOpenHelp} hoverClass="hover-row-orange" />
          </ul>
        </section>
      </div>
    </div>
  );
}

function loanStatus(user: User, badge?: string | null) {
  if (user.disbursed) {
    return {
      label: "Disbursed",
      toneLabel: "Loan status",
      detail: "The loan amount has been sent to your registered bank account.",
      cta: "View application",
      badgeClass: "bg-emerald-50 text-emerald-700",
      panelClass: "bg-emerald-50 text-emerald-900",
    };
  }
  if (user.selfieStatus === "APPROVED") {
    return {
      label: "Approved",
      toneLabel: "Loan status",
      detail: "Selfie is approved. Waiting for disbursement.",
      cta: "View application",
      badgeClass: "bg-emerald-50 text-emerald-700",
      panelClass: "bg-emerald-50 text-emerald-900",
    };
  }
  if (user.selfieStatus === "PENDING") {
    return {
      label: "Under Review",
      toneLabel: "Loan status",
      detail: "Your application has been sent and is with an administrator. You will be notified after review.",
      cta: "View application",
      badgeClass: "bg-amber-50 text-amber-700",
      panelClass: "bg-amber-50 text-amber-950",
    };
  }
  if (isReadyToSend(user)) {
    return {
      label: "Ready to Submit",
      toneLabel: "Loan status",
      detail: "All steps are complete. Open Selfie Verification and tap Send Application when you are ready.",
      cta: "Send application",
      badgeClass: "bg-emerald-50 text-emerald-700",
      panelClass: "bg-emerald-50 text-emerald-900",
    };
  }
  if (user.selfieStatus === "REJECTED") {
    return {
      label: "Application Rejected",
      toneLabel: "Loan status",
      detail: "Your application was rejected. Review the admin message, update any required details, capture a new selfie, and send the application again.",
      cta: "Update & resubmit",
      badgeClass: "bg-rose-50 text-rose-700",
      panelClass: "bg-rose-50 text-rose-900",
    };
  }
  if (user.eligibilityResult === "NOT_ELIGIBLE") {
    return {
      label: "Not Eligible",
      toneLabel: "Loan status",
      detail: "This application did not meet eligibility rules. You can review the eligibility step for reasons.",
      cta: "View eligibility",
      badgeClass: "bg-rose-50 text-rose-700",
      panelClass: "bg-rose-50 text-rose-900",
    };
  }
  return {
    label: badge || `${user.applicationStageLabel} Pending`,
    toneLabel: "Loan status",
    detail: `Continue from ${LOAN_STEPS.find((item) => item.id === defaultStep(user))?.label ?? "the next step"} to move this application forward.`,
    cta: "Continue application",
    badgeClass: "bg-orange-50 text-orange-600",
    panelClass: "bg-blue-50 text-blue-950",
  };
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium text-slate-900">{value}</dd>
    </div>
  );
}

function HelpLink({
  label,
  icon: Icon,
  onClick,
  hoverClass,
}: {
  label: string;
  icon: typeof CircleHelp;
  onClick: () => void;
  hoverClass: string;
}) {
  return (
    <li>
      <button
        type="button"
        onClick={onClick}
        className={`${hoverClass} flex w-full items-center justify-between rounded-lg px-2 py-3 text-sm text-slate-700 transition-all duration-200`}
      >
        <span className="flex items-center gap-2">
          <Icon className="h-4 w-4 text-blue-600 transition-colors duration-200" />
          {label}
        </span>
        <ChevronRight className="h-4 w-4 text-slate-400 transition-transform duration-200 group-hover:translate-x-0.5" />
      </button>
    </li>
  );
}
