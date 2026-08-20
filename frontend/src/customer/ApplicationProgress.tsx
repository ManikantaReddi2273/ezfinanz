import { Check } from "lucide-react";
import type { User } from "../api/client";
import { LOAN_STEPS, stepStatus, type StepId } from "./steps";

export function ApplicationProgress({
  user,
  onSelect,
}: {
  user: User;
  onSelect: (id: StepId) => void;
}) {
  return (
    <section className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm sm:p-6">
      <div className="mb-5 flex items-center justify-between">
        <h2 className="text-base font-semibold text-slate-900">Application Progress</h2>
      </div>
      <ol className="flex gap-0 overflow-x-auto pb-1">
        {LOAN_STEPS.map((item, index) => {
          const status = stepStatus(user, item.id);
          const nextStatus = LOAN_STEPS[index + 1] ? stepStatus(user, LOAN_STEPS[index + 1].id) : null;
          const connectorDone = status === "complete" && nextStatus === "complete";
          return (
            <li key={item.id} className="flex min-w-[96px] flex-1 items-start">
              <button type="button" onClick={() => onSelect(item.id)} className="flex w-full flex-col items-center">
                <span
                  className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-semibold ${
                    status === "complete"
                      ? "bg-emerald-500 text-white"
                      : status === "current"
                        ? "bg-blue-600 text-white"
                        : "bg-slate-200 text-slate-500"
                  }`}
                >
                  {status === "complete" ? <Check className="h-4 w-4" strokeWidth={3} /> : item.number}
                </span>
                <span className="mt-2 max-w-[88px] text-center text-[11px] font-medium leading-4 text-slate-600">
                  {item.label}
                </span>
              </button>
              {index < LOAN_STEPS.length - 1 && (
                <span
                  className={`mt-[17px] h-0.5 min-w-[12px] flex-1 ${connectorDone ? "bg-emerald-500" : "bg-slate-200"}`}
                  aria-hidden
                />
              )}
            </li>
          );
        })}
      </ol>
    </section>
  );
}
