import { ChevronRight, CircleHelp, Headphones, Waypoints } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiError, supportApi, type SupportTicket } from "../api/client";
import { formatDateTime } from "../lib/money";

const FAQS = [
  {
    q: "How long does approval take?",
    a: "After you submit a live selfie, an administrator reviews it. Disbursement happens only after approval.",
  },
  {
    q: "Where are my documents stored?",
    a: "KYC files are saved under backend/uploads/kyc and selfies under backend/uploads/selfie, linked to your user id.",
  },
  {
    q: "Can I change EMI tenure?",
    a: "Yes, until the loan is disbursed. Open Dashboard or My Application, pick a tenure, and continue to save it.",
  },
];

export function HelpPage({ onContact }: { onContact?: () => void }) {
  const [subject, setSubject] = useState("Application help");
  const [message, setMessage] = useState("");
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [openFaq, setOpenFaq] = useState<string | null>(FAQS[0].q);

  const load = () => {
    supportApi.list().then(setTickets).catch(() => undefined);
  };

  useEffect(() => {
    load();
  }, []);

  const send = async () => {
    setSaving(true);
    setError(null);
    try {
      await supportApi.send(subject, message);
      setMessage("");
      load();
      onContact?.();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not send the message.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
      <section className="rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Need Help?</h2>
        <button type="button" onClick={() => setOpenFaq(openFaq === FAQS[0].q ? null : FAQS[0].q)} className="sr-only">
          faqs
        </button>
        <ul className="mt-4 divide-y divide-slate-100">
          {FAQS.map((item) => (
            <li key={item.q}>
              <button
                type="button"
                onClick={() => setOpenFaq(openFaq === item.q ? null : item.q)}
                className="flex w-full items-center justify-between py-3 text-left text-sm font-medium text-slate-800"
              >
                <span className="flex items-center gap-2">
                  <CircleHelp className="h-4 w-4 text-blue-600" />
                  {item.q}
                </span>
                <ChevronRight className={`h-4 w-4 text-slate-400 ${openFaq === item.q ? "rotate-90" : ""}`} />
              </button>
              {openFaq === item.q && <p className="pb-3 pl-6 text-sm text-slate-600">{item.a}</p>}
            </li>
          ))}
          <li className="flex items-center justify-between py-3 text-sm font-medium text-slate-800">
            <span className="flex items-center gap-2">
              <Waypoints className="h-4 w-4 text-blue-600" />
              How it works?
            </span>
          </li>
          <li className="pt-2 text-sm text-slate-600">
            Complete 8 steps: account, verify contacts, KYC, eligibility, EMI, bank, declaration, then selfie review.
          </li>
        </ul>
      </section>
      <section className="rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
        <h3 className="flex items-center gap-2 text-base font-semibold text-slate-900">
          <Headphones className="h-4 w-4 text-blue-600" />
          Contact Support
        </h3>
        <input
          value={subject}
          onChange={(event) => setSubject(event.target.value)}
          className="mt-4 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-600"
        />
        <textarea
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          rows={4}
          placeholder="How can we help?"
          className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-600"
        />
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
        <button
          type="button"
          disabled={saving || message.trim().length < 4}
          onClick={() => void send()}
          className="mt-4 w-full rounded-xl bg-blue-600 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
        >
          {saving ? "Sending…" : "Send message"}
        </button>
        {tickets.length > 0 && (
          <ul className="mt-5 space-y-3 text-sm">
            {tickets.map((ticket) => (
              <li key={ticket.id} className="rounded-lg bg-slate-50 px-3 py-2">
                <p className="font-medium text-slate-800">{ticket.subject}</p>
                <p className="text-slate-600">{ticket.message}</p>
                <p className="mt-1 text-xs text-slate-400">{formatDateTime(ticket.createdAt)}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
