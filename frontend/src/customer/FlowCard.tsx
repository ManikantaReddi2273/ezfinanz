import type { ReactNode } from "react";

export const flowInput =
  "mt-1.5 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-600 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-600";

export const flowPrimary =
  "w-full rounded-xl bg-blue-600 py-3 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60";

export const flowGhost =
  "w-full rounded-xl border border-blue-600 bg-white py-3 text-sm font-semibold text-blue-700 hover:bg-blue-50 disabled:opacity-60";

export function FlowCard({ children, wide }: { children: ReactNode; wide?: boolean }) {
  return (
    <div className={`mx-auto rounded-2xl bg-white p-6 shadow-md shadow-slate-200/70 ${wide ? "max-w-lg" : "max-w-[400px]"}`}>
      {children}
    </div>
  );
}
