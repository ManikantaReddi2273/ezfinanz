/**
 * Customer app shell: sidebar nav, application steps, profile/docs/help, support chat.
 */
import type { LucideIcon } from "lucide-react";
import {
  Bell,
  FileText,
  FolderOpen,
  HelpCircle,
  LayoutDashboard,
  LogOut,
  Menu,
  UserRound,
  X,
} from "lucide-react";
import { useEffect, useState } from "react";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { SiteFooter } from "../components/SiteFooter";
import { customerApi, type DashboardNotice } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { ApplicationProgress } from "./ApplicationProgress";
import { BankStep } from "./BankStep";
import { DashboardHome } from "./DashboardHome";
import { DeclarationStep } from "./DeclarationStep";
import { DocumentsPage } from "./DocumentsPage";
import { EligibilityStep } from "./EligibilityStep";
import { EmiStep } from "./EmiStep";
import { HelpPage } from "./HelpPage";
import { KycStep } from "./KycStep";
import { ProfilePage } from "./ProfilePage";
import { SelfieStep } from "./SelfieStep";
import { SupportChatWidget } from "./SupportChatWidget";
import { defaultStep, isApplicationRejected, isStepReadOnly, lockMessage, canNavigateToStep, isApplicationSubmitted, isReadyToSend, stepStatus, type StepId } from "./steps";
import { VerificationStep } from "./VerificationStep";

type Page = "home" | "apply" | "profile" | "documents" | "help";

/** Main authenticated customer experience with step wizard and secondary pages. */
export function CustomerDashboard() {
  const { user, logout } = useAuth();
  const [page, setPage] = useState<Page>("home");
  const [step, setStep] = useState<StepId>("verify");
  const [menuOpen, setMenuOpen] = useState(false);
  const [lockNote, setLockNote] = useState<string | null>(null);
  const [notices, setNotices] = useState<DashboardNotice[]>([]);
  const [showNotices, setShowNotices] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);

  useEffect(() => {
    if (!user) {
      return;
    }
    setStep(defaultStep(user));
    customerApi.dashboard().then((row) => setNotices(row.notices)).catch(() => undefined);
  }, [
    user?.id,
    user?.emailVerified,
    user?.phoneVerified,
    user?.kycCompleted,
    user?.eligibilityPassed,
    user?.emiCompleted,
    user?.bankCompleted,
    user?.declarationCompleted,
    user?.selfieSubmitted,
    user?.selfieStatus,
    user?.disbursed,
  ]);

  if (!user) {
    return null;
  }

  const initials = (user.fullName || user.email || "U")
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  const titles: Record<Page, string> = {
    home: "Dashboard",
    apply: "My Application",
    profile: "Profile",
    documents: "Documents",
    help: "Help & Support",
  };

  const goToStep = (id: StepId) => {
    setLockNote(null);
    setStep(id);
    setPage("apply");
    setMenuOpen(false);
  };

  const selectStep = (id: StepId) => {
    setLockNote(null);
    if (canNavigateToStep(user, id)) {
      goToStep(id);
      return;
    }
    const status = stepStatus(user, id);
    if (status === "locked") {
      setLockNote(lockMessage(user));
      goToStep(defaultStep(user));
      return;
    }
    goToStep(id);
  };

  const openPage = (next: Page) => {
    setPage(next);
    setMenuOpen(false);
    setShowNotices(false);
  };

  const noticeCount = notices.filter((item) => item.id !== "ok").length;

  return (
    <div className="min-h-screen bg-[#F5F7FB] lg:grid lg:grid-cols-[248px_1fr]">
      <aside
        className={`fixed inset-y-0 left-0 z-30 flex w-[248px] flex-col border-r border-slate-100 bg-white transition-transform lg:static lg:translate-x-0 ${
          menuOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between px-5 py-5">
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-sm font-bold text-white">
              E
            </span>
            <p className="text-lg font-bold tracking-tight text-slate-900">EZFINANZ</p>
          </div>
          <button type="button" className="lg:hidden" onClick={() => setMenuOpen(false)}>
            <X className="h-5 w-5 text-slate-500" />
          </button>
        </div>
        <nav className="flex-1 space-y-1 px-3">
          <SideLink icon={LayoutDashboard} label="Dashboard" active={page === "home"} hoverClass="hover-nav-blue" onClick={() => openPage("home")} />
          <SideLink icon={FileText} label="My Application" active={page === "apply"} hoverClass="hover-nav-green" onClick={() => { setStep(defaultStep(user)); openPage("apply"); }} />
          <SideLink icon={UserRound} label="Profile" active={page === "profile"} hoverClass="hover-nav-violet" onClick={() => openPage("profile")} />
          <SideLink icon={FolderOpen} label="Documents" active={page === "documents"} hoverClass="hover-nav-orange" onClick={() => openPage("documents")} />
          <SideLink icon={HelpCircle} label="Help & Support" active={page === "help"} hoverClass="hover-nav-rose" onClick={() => openPage("help")} />
        </nav>
        <div className="p-4">
          <div className="hover-card-blue hover-lift rounded-2xl border border-transparent bg-blue-50 p-4 transition-all duration-300">
            <p className="text-sm font-semibold text-slate-900">Need Help?</p>
            <p className="mt-1 text-xs text-slate-500">Talk to our support team</p>
            <button
              type="button"
              onClick={() => openPage("help")}
              className="btn-hover-blue mt-3 w-full rounded-lg bg-white py-2 text-xs font-semibold text-blue-700 shadow-sm transition-all duration-200 hover:bg-blue-50 hover:text-blue-800"
            >
              Contact Support
            </button>
          </div>
          <button type="button" onClick={() => setLogoutOpen(true)} className="hover-nav-rose mt-3 flex w-full items-center gap-2 rounded-lg px-2 py-2 text-sm text-slate-500 transition-all duration-200">
            <LogOut className="h-4 w-4" />
            Logout
          </button>
        </div>
      </aside>

      {menuOpen && <button type="button" className="fixed inset-0 z-20 bg-slate-900/30 lg:hidden" onClick={() => setMenuOpen(false)} />}

      <div className="flex min-h-screen flex-col">
        <header className="flex items-center justify-between px-4 py-5 sm:px-8">
          <div className="flex items-center gap-3">
            <button type="button" className="rounded-lg p-2 text-slate-600 lg:hidden" onClick={() => setMenuOpen(true)}>
              <Menu className="h-5 w-5" />
            </button>
            <h1 className="text-2xl font-bold text-slate-900">{titles[page]}</h1>
          </div>
          <div className="flex items-center gap-3">
            <div className="relative">
              <button type="button" className="relative rounded-full p-2 text-slate-500 transition-all duration-200 hover:bg-blue-50 hover:text-blue-600" onClick={() => setShowNotices((open) => !open)}>
                <Bell className="h-5 w-5" />
                {noticeCount > 0 && (
                  <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
                    {noticeCount}
                  </span>
                )}
              </button>
              {showNotices && (
                <div className="absolute right-0 z-20 mt-2 w-72 rounded-xl border border-slate-100 bg-white p-2 shadow-lg">
                  {notices.map((notice) => (
                    <button
                      key={notice.id}
                      type="button"
                      className="hover-row-blue block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-700 transition-all duration-200"
                      onClick={() => {
                        setShowNotices(false);
                        if (notice.target === "verify") {
                          goToStep("verify");
                        } else if (notice.target === "emi") {
                          goToStep("emi");
                        } else if (notice.target === "selfie") {
                          goToStep("selfie");
                        } else {
                          openPage("home");
                        }
                      }}
                    >
                      {notice.title}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="flex items-center gap-2">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">
                {initials}
              </span>
              <span className="hidden text-sm font-medium text-slate-700 sm:inline">{user.fullName || user.email}</span>
            </div>
          </div>
        </header>

        <main className="flex-1 px-4 pb-10 sm:px-8">
          {page === "home" && (
            <DashboardHome user={user} onOpenStep={selectStep} onOpenHelp={() => openPage("help")} />
          )}
          {page === "apply" && (
            <>
              <p className="mb-4 text-lg font-semibold text-slate-900">Application Flow</p>
              <div className="mb-6">
                <ApplicationProgress user={user} onSelect={selectStep} />
              </div>
              {lockNote && (
                <p className="mb-4 rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700">{lockNote}</p>
              )}
              {!isApplicationSubmitted(user) && (
                <p
                  className={`mb-4 rounded-lg px-4 py-3 text-sm ${
                    isApplicationRejected(user)
                      ? "border border-rose-100 bg-rose-50 text-rose-900"
                      : "border border-blue-100 bg-blue-50 text-blue-900"
                  }`}
                >
                  {isApplicationRejected(user)
                    ? "Your application was rejected. You can edit any completed step, capture a new selfie, and resubmit for review."
                    : (
                      <>
                        You can go back and edit any completed step before sending your application from Selfie Verification.
                        {isReadyToSend(user) ? " Your application is ready to send." : ""}
                      </>
                    )}
                </p>
              )}
              {step === "account" && (
                <section className="mx-auto max-w-[400px] rounded-2xl bg-white p-6 shadow-md">
                  <p className="text-center font-semibold text-slate-900">Continue your application</p>
                  <button type="button" onClick={() => goToStep(defaultStep(user))} className="mt-4 w-full rounded-xl bg-blue-600 py-3 text-sm font-semibold text-white">
                    Continue
                  </button>
                </section>
              )}
              {step === "verify" && (
                <VerificationStep readOnly={isStepReadOnly(user, "verify")} onContinue={() => goToStep("kyc")} />
              )}
              {step === "kyc" && <KycStep readOnly={isStepReadOnly(user, "kyc")} onContinue={() => goToStep("eligibility")} />}
              {step === "eligibility" && (
                <EligibilityStep readOnly={isStepReadOnly(user, "eligibility")} onContinue={() => goToStep("emi")} />
              )}
              {step === "emi" && <EmiStep readOnly={isStepReadOnly(user, "emi")} onContinue={() => goToStep("bank")} />}
              {step === "bank" && <BankStep readOnly={isStepReadOnly(user, "bank")} onContinue={() => goToStep("declaration")} />}
              {step === "declaration" && (
                <DeclarationStep readOnly={isStepReadOnly(user, "declaration")} onContinue={() => goToStep("selfie")} />
              )}
              {step === "selfie" && <SelfieStep readOnly={isStepReadOnly(user, "selfie")} />}
            </>
          )}
          {page === "profile" && <ProfilePage />}
          {page === "documents" && <DocumentsPage />}
          {page === "help" && <HelpPage />}
        </main>
        <SiteFooter variant="dashboard" />
      </div>
      <SupportChatWidget />
      <ConfirmDialog
        open={logoutOpen}
        title="Log out?"
        message="You will need to sign in again to continue your loan application."
        confirmLabel="Log out"
        tone="red"
        onConfirm={() => {
          setLogoutOpen(false);
          logout();
        }}
        onCancel={() => setLogoutOpen(false)}
      />
    </div>
  );
}

function SideLink({
  icon: Icon,
  label,
  active,
  hoverClass,
  onClick,
}: {
  icon: LucideIcon;
  label: string;
  active: boolean;
  hoverClass: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition-all duration-200 ${
        active ? "bg-blue-50 font-semibold text-blue-700" : `text-slate-500 ${hoverClass}`
      }`}
    >
      <Icon className={`h-4 w-4 transition-colors duration-200 ${active ? "text-blue-600" : "text-slate-400"}`} />
      {label}
    </button>
  );
}
