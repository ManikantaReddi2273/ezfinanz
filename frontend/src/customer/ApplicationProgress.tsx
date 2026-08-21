/**
 * Horizontal 8-step progress tracker; clicks open reachable steps via `onSelect`.
 */
import { Check } from "lucide-react";
import type { User } from "../api/client";
import { LOAN_STEPS, stepStatus, type StepId } from "./steps";

/** Visual application progress bar used on home and apply views. */
export function ApplicationProgress({
  user,
  onSelect,
}: {
  user: User;
  onSelect: (id: StepId) => void;
}) {
  return (
    <section className="hover-card-blue rounded-2xl border border-slate-100 bg-white p-5 shadow-sm transition-all duration-300 sm:p-6">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-base font-semibold text-slate-900">Application Progress</h2>
      </div>
      <ol className="flex items-start">
        {LOAN_STEPS.map((item, index) => {
          const status = stepStatus(user, item.id);
          const prevStatus = index > 0 ? stepStatus(user, LOAN_STEPS[index - 1].id) : null;
          const segmentDone =
            prevStatus === "complete" && status === "complete";
          const locked = status === "locked";

          return (
            <li key={item.id} className="relative flex min-w-[88px] flex-1 flex-col items-center">
              {index > 0 && (
                <span
                  className={`absolute right-1/2 top-[18px] h-[3px] w-full -translate-y-1/2 rounded-full transition-colors duration-300 ${
                    segmentDone ? "bg-emerald-500" : "bg-slate-200"
                  }`}
                  aria-hidden
                />
              )}

              <button
                type="button"
                onClick={() => onSelect(item.id)}
                disabled={locked}
                className={`group relative z-10 flex flex-col items-center transition-transform duration-200 ${
                  locked ? "cursor-not-allowed" : "hover:scale-105"
                }`}
              >
                <span
                  className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-semibold shadow-sm transition-all duration-200 ${
                    status === "complete"
                      ? "bg-emerald-500 text-white group-hover:bg-emerald-600 group-hover:shadow-emerald-200 group-hover:shadow-md"
                      : status === "current"
                        ? "border-2 border-blue-600 bg-white text-blue-600 ring-4 ring-blue-50 group-hover:border-blue-700 group-hover:ring-blue-100"
                        : "border-2 border-slate-200 bg-white text-slate-400"
                  }`}
                >
                  {status === "complete" ? <Check className="h-4 w-4" strokeWidth={3} /> : item.number}
                </span>
                <span
                  className={`mt-2.5 max-w-[88px] text-center text-[11px] font-medium leading-4 transition-colors duration-200 ${
                    status === "current"
                      ? "text-blue-700"
                      : status === "complete"
                        ? "text-emerald-700 group-hover:text-emerald-800"
                        : "text-slate-500"
                  }`}
                >
                  {item.label}
                </span>
              </button>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
