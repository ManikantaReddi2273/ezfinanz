/**
 * Shared card shell and button/input class names for application step forms.
 */
import type { ReactNode } from "react";

/** Standard text input classes for step forms. */
export const flowInput =
  "mt-1.5 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-600 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-600";

/** Primary action button classes for step forms. */
export const flowPrimary =
  "btn-hover-blue w-full rounded-xl bg-blue-600 py-3 text-sm font-semibold text-white transition-all duration-200 hover:bg-blue-700 disabled:opacity-60 disabled:hover:transform-none disabled:hover:shadow-none";

/** Secondary/outline button classes for step forms. */
export const flowGhost =
  "hover-lift w-full rounded-xl border border-blue-600 bg-white py-3 text-sm font-semibold text-blue-700 transition-all duration-200 hover:border-blue-700 hover:bg-blue-50 hover:shadow-md hover:shadow-blue-100 disabled:opacity-60";

/** Centered white card wrapping a single application step's content. */
export function FlowCard({ children, wide }: { children: ReactNode; wide?: boolean }) {
  return (
    <div
      className={`hover-card-blue hover-lift mx-auto rounded-2xl border border-transparent bg-white p-6 shadow-md shadow-slate-200/70 ${wide ? "max-w-lg" : "max-w-[400px]"}`}
    >
      {children}
    </div>
  );
}
