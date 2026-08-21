/**
 * Site-wide footer for landing and in-app (dashboard) variants.
 */
import { Link } from "react-router-dom";
import { Mail, MapPin, Phone, ShieldCheck } from "lucide-react";

type SiteFooterProps = {
  variant?: "landing" | "dashboard";
};

/** Brand, links, and contact footer; `landing` adds marketing CTAs. */
export function SiteFooter({ variant = "landing" }: SiteFooterProps) {
  const year = new Date().getFullYear();
  const isLanding = variant === "landing";

  return (
    <footer
      className={
        isLanding
          ? "relative z-10 border-t border-blue-100/80 bg-gradient-to-b from-slate-50 to-white"
          : "mt-auto border-t border-slate-200 bg-white"
      }
    >
      <div className={`mx-auto grid gap-8 px-4 py-10 sm:px-8 ${isLanding ? "max-w-6xl md:grid-cols-4" : "max-w-5xl md:grid-cols-3"}`}>
        <div className={isLanding ? "md:col-span-1" : ""}>
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-sm font-bold text-white">
              E
            </span>
            <p className="text-lg font-bold tracking-tight text-slate-900">EZFINANZ</p>
          </div>
          <p className="mt-3 max-w-xs text-sm leading-6 text-slate-500">
            Personal loans made simple — verify, apply, and track your application in one place.
          </p>
          <p className="mt-3 flex items-center gap-1.5 text-xs font-medium text-emerald-700">
            <ShieldCheck className="h-3.5 w-3.5" />
            Secure · Encrypted · Role-based access
          </p>
        </div>

        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">Quick links</p>
          <ul className="mt-3 space-y-2 text-sm text-slate-600">
            {isLanding ? (
              <>
                <li>
                  <a href="#how-it-works" className="transition hover:text-blue-700">
                    How it works
                  </a>
                </li>
                <li>
                  <a href="#eligibility" className="transition hover:text-blue-700">
                    Eligibility
                  </a>
                </li>
                <li>
                  <a href="#security" className="transition hover:text-blue-700">
                    Security
                  </a>
                </li>
                <li>
                  <Link to="/login" className="transition hover:text-blue-700">
                    Sign in
                  </Link>
                </li>
                <li>
                  <Link to="/signup" className="transition hover:text-blue-700">
                    Create account
                  </Link>
                </li>
              </>
            ) : (
              <>
                <li>
                  <span className="text-slate-500">Dashboard · Application · Profile</span>
                </li>
                <li>
                  <span className="text-slate-500">Documents · Help & Support</span>
                </li>
                <li>
                  <a href="mailto:campusworks2273@gmail.com" className="transition hover:text-blue-700">
                    Contact support
                  </a>
                </li>
              </>
            )}
          </ul>
        </div>

        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">Contact</p>
          <ul className="mt-3 space-y-2.5 text-sm text-slate-600">
            <li className="flex items-start gap-2">
              <Mail className="mt-0.5 h-4 w-4 shrink-0 text-blue-600" />
              <a href="mailto:campusworks2273@gmail.com" className="transition hover:text-blue-700">
                campusworks2273@gmail.com
              </a>
            </li>
            <li className="flex items-start gap-2">
              <Phone className="mt-0.5 h-4 w-4 shrink-0 text-blue-600" />
              <span>Support via Help & Support in your dashboard</span>
            </li>
            <li className="flex items-start gap-2">
              <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-blue-600" />
              <span>India · Digital lending platform</span>
            </li>
          </ul>
        </div>

        {isLanding && (
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">Get started</p>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Apply in 8 guided steps. Track status anytime from your customer dashboard.
            </p>
            <Link
              to="/signup"
              className="mt-4 inline-flex rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700"
            >
              Start application
            </Link>
          </div>
        )}
      </div>

      <div className={`border-t ${isLanding ? "border-blue-100/70" : "border-slate-100"}`}>
        <div
          className={`mx-auto flex flex-col gap-2 px-4 py-4 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-8 ${
            isLanding ? "max-w-6xl" : "max-w-5xl"
          }`}
        >
          <p>© {year} EZFINANZ. All rights reserved.</p>
          <p className="sm:text-right">Personal loan application platform · Terms & privacy apply</p>
        </div>
      </div>
    </footer>
  );
}
