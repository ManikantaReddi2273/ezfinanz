import type { ReactNode } from "react";
import { Link } from "react-router-dom";

export function AuthLayout({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <div className="min-h-screen bg-slate-50 lg:grid lg:grid-cols-2">
      <aside className="hidden lg:flex flex-col justify-between border-r border-slate-200 bg-white p-12">
        <Link to="/" className="text-xl font-semibold tracking-tight text-blue-700">
          EZFINANZ
        </Link>
        <div>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-700">Personal loans</p>
          <h1 className="mt-3 text-4xl font-semibold leading-tight text-slate-900">
            Apply with a clear,
            <br />
            guided process.
          </h1>
          <p className="mt-5 max-w-md text-sm leading-6 text-slate-600">
            Customers and admins use the same sign-in. After login you land on a dashboard that tracks
            every step of the application.
          </p>
        </div>
        <p className="text-sm text-slate-400">Secure access · Role-based routing</p>
      </aside>
      <main className="flex items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
          <Link to="/" className="lg:hidden mb-6 inline-block text-lg font-semibold text-slate-900">
            EZFINANZ
          </Link>
          <h2 className="text-2xl font-semibold text-slate-900">{title}</h2>
          <p className="mt-2 text-sm text-slate-600">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </main>
    </div>
  );
}
