import type { LucideIcon } from "lucide-react";
import {
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
  Users,
  X,
} from "lucide-react";
import { useState, type ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function AdminLayout({
  title,
  headerRight,
  children,
}: {
  title: string;
  headerRight?: ReactNode;
  children: ReactNode;
}) {
  const { logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-[#F5F7FB] lg:grid lg:grid-cols-[240px_1fr]">
      <aside
        className={`fixed inset-y-0 left-0 z-30 flex w-[240px] flex-col bg-[#1E3A8A] text-white transition-transform lg:static lg:translate-x-0 ${
          menuOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between px-5 py-5">
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-500 text-sm font-bold">E</span>
            <p className="text-lg font-bold tracking-tight">EZFINANZ</p>
          </div>
          <button type="button" className="lg:hidden" onClick={() => setMenuOpen(false)}>
            <X className="h-5 w-5" />
          </button>
        </div>
        <p className="px-5 pb-2 text-[11px] font-semibold uppercase tracking-[0.16em] text-blue-200">Admin</p>
        <nav className="flex-1 space-y-1 px-3">
          <AdminLink to="/admin" icon={LayoutDashboard} label="Dashboard" end />
          <AdminLink to="/admin/applications" icon={FileText} label="Applications" />
          <AdminLink to="/admin/users" icon={Users} label="Users" />
          <AdminLink to="/admin/settings" icon={Settings} label="Settings" />
        </nav>
        <button type="button" onClick={logout} className="m-4 flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-blue-100 hover:bg-white/10">
          <LogOut className="h-4 w-4" />
          Logout
        </button>
      </aside>
      {menuOpen && <button type="button" className="fixed inset-0 z-20 bg-slate-900/40 lg:hidden" onClick={() => setMenuOpen(false)} />}
      <div>
        <header className="flex items-center justify-between px-4 py-5 sm:px-8">
          <div className="flex items-center gap-3">
            <button type="button" className="rounded-lg p-2 text-slate-600 lg:hidden" onClick={() => setMenuOpen(true)}>
              <Menu className="h-5 w-5" />
            </button>
            <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
          </div>
          {headerRight}
        </header>
        <main className="px-4 pb-10 sm:px-8">{children}</main>
      </div>
    </div>
  );
}

function AdminLink({
  to,
  icon: Icon,
  label,
  end,
}: {
  to: string;
  icon: LucideIcon;
  label: string;
  end?: boolean;
}) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${
          isActive ? "bg-blue-600 font-semibold text-white" : "text-blue-100 hover:bg-white/10"
        }`
      }
    >
      <Icon className="h-4 w-4" />
      {label}
    </NavLink>
  );
}

export function applicationId(userId: number): string {
  return `EZF${String(userId).padStart(9, "0")}`;
}

export function stageBadgeClass(stage: string): string {
  if (stage === "DISBURSED" || stage === "READY_FOR_DISBURSEMENT") {
    return "bg-emerald-50 text-emerald-700";
  }
  if (stage === "WAITING_FOR_ADMIN_REVIEW" || stage === "LIVE_SELFIE") {
    return "bg-orange-50 text-orange-600";
  }
  if (stage === "SELFIE_REJECTED" || stage === "NOT_ELIGIBLE") {
    return "bg-rose-50 text-rose-700";
  }
  if (stage === "KYC") {
    return "bg-slate-200 text-slate-700";
  }
  if (stage === "ELIGIBILITY" || stage === "EMI") {
    return "bg-sky-50 text-sky-700";
  }
  return "bg-blue-50 text-blue-700";
}
