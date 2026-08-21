/**
 * Help & Support page: FAQs plus support ticket list/create.
 */
import { ChevronRight, CircleHelp, Headphones, MessageCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiError, supportApi, type SupportTicket } from "../api/client";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { formatDateTime } from "../lib/money";

const FAQS = [
  {
    q: "What are the steps to apply for a loan?",
    a: "Complete 8 guided steps: sign up, verify email and phone, KYC, eligibility check, EMI selection, bank account, declaration, and selfie verification. Use Send Application on the selfie step when everything is ready.",
  },
  {
    q: "Can I edit my application before submitting?",
    a: "Yes. Until you tap Send Application, you can go back to any completed step and update your details. If you change an earlier step, later steps may need to be reviewed again.",
  },
  {
    q: "How long does approval take?",
    a: "After you send your application, an administrator reviews your selfie and application details. Once approved, the loan moves to disbursement.",
  },
  {
    q: "What documents do I need for KYC?",
    a: "Provide your identity details (PAN, Aadhaar, or another accepted ID), address information, and optionally upload a photo of your ID document during the KYC step.",
  },
  {
    q: "How is loan eligibility decided?",
    a: "Eligibility is based on your income, requested loan amount, credit score (CIBIL), outstanding debts, and debt-to-income ratio. You will see Eligible, Partially Eligible, or Not Eligible after the check.",
  },
  {
    q: "Can I change my EMI amount or tenure?",
    a: "Yes, before sending your application. Open EMI Selection, choose a loan amount and tenure, and save the updated terms. Changing EMI may require you to review bank and declaration steps again.",
  },
  {
    q: "What happens after I confirm my selfie?",
    a: "Your photo is saved as a draft. Review all steps, then tap Send Application to submit for admin review. You can retake the selfie before sending if needed.",
  },
  {
    q: "What if my selfie is rejected?",
    a: "You will see the rejection reason in your dashboard and receive an email with the admin message. Update any required details, capture a new selfie, and send your application again for review.",
  },
];

/** FAQ accordion and ticket form; optional `onContact` opens the chat widget. */
export function HelpPage({ onContact }: { onContact?: () => void }) {
  const [subject, setSubject] = useState("Application help");
  const [message, setMessage] = useState("");
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [openFaq, setOpenFaq] = useState<string | null>(FAQS[0].q);
  const [sendOpen, setSendOpen] = useState(false);

  const load = () => {
    supportApi.list().then(setTickets).catch(() => undefined);
  };

  useEffect(() => {
    load();
  }, []);

  const send = async () => {
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await supportApi.send(subject, message);
      setMessage("");
      setSuccess("Your message was sent to our support team. We will get back to you soon.");
      load();
      onContact?.();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not send the message.");
    } finally {
      setSaving(false);
      setSendOpen(false);
    }
  };

  return (
    <div className="space-y-6">
      <ConfirmDialog
        open={sendOpen}
        title="Send message?"
        message="Send this support message to the EZFINANZ team?"
        confirmLabel="Send message"
        tone="blue"
        busy={saving}
        onConfirm={() => void send()}
        onCancel={() => setSendOpen(false)}
      />

      <section className="rounded-2xl border border-blue-100 bg-blue-50 px-5 py-4 text-sm text-blue-900">
        <p className="flex items-center gap-2 font-semibold">
          <MessageCircle className="h-4 w-4" />
          Need a quick answer?
        </p>
        <p className="mt-1 text-blue-800/90">
          Use the blue chat button in the bottom-right corner to talk with the EZFINANZ Assistant anytime while you browse your dashboard.
        </p>
      </section>

      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <section className="hover-card-orange hover-lift rounded-2xl border border-slate-100 bg-white p-6 shadow-sm transition-all duration-300">
          <h2 className="text-lg font-semibold text-slate-900">Frequently Asked Questions</h2>
          <p className="mt-1 text-sm text-slate-500">Common questions about your personal loan application</p>
          <ul className="mt-4 divide-y divide-slate-100">
            {FAQS.map((item) => (
              <li key={item.q}>
                <button
                  type="button"
                  onClick={() => setOpenFaq(openFaq === item.q ? null : item.q)}
                  className="hover-row-blue flex w-full items-center justify-between rounded-lg px-2 py-3 text-left text-sm font-medium text-slate-800 transition-all duration-200"
                >
                  <span className="flex items-center gap-2 pr-3">
                    <CircleHelp className="h-4 w-4 shrink-0 text-blue-600" />
                    {item.q}
                  </span>
                  <ChevronRight className={`h-4 w-4 shrink-0 text-slate-400 transition-transform ${openFaq === item.q ? "rotate-90" : ""}`} />
                </button>
                {openFaq === item.q && <p className="pb-3 pl-8 pr-2 text-sm leading-6 text-slate-600">{item.a}</p>}
              </li>
            ))}
          </ul>
        </section>
        <section className="hover-card-blue hover-lift rounded-2xl border border-slate-100 bg-white p-6 shadow-sm transition-all duration-300">
          <h3 className="flex items-center gap-2 text-base font-semibold text-slate-900">
            <Headphones className="h-4 w-4 text-blue-600" />
            Contact Support
          </h3>
          <p className="mt-1 text-sm text-slate-500">
            Send a message about your application. Our team will receive it by email.
          </p>
          <input
            value={subject}
            onChange={(event) => setSubject(event.target.value)}
            className="mt-4 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-600"
            placeholder="Subject"
          />
          <textarea
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            rows={4}
            placeholder="Describe your question about eligibility, EMI, KYC, selfie review, or application status…"
            className="mt-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-600"
          />
          {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
          {success && <p className="mt-2 text-sm text-emerald-700">{success}</p>}
          <button
            type="button"
            disabled={saving || message.trim().length < 4}
            onClick={() => setSendOpen(true)}
            className="btn-hover-blue mt-4 w-full rounded-xl bg-blue-600 py-2.5 text-sm font-semibold text-white transition-all duration-200 disabled:opacity-60"
          >
            {saving ? "Sending…" : "Send message"}
          </button>
          {tickets.length > 0 && (
            <div className="mt-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Your recent messages</p>
              <ul className="mt-2 space-y-3 text-sm">
                {tickets.map((ticket) => (
                  <li key={ticket.id} className="rounded-lg bg-slate-50 px-3 py-2">
                    <p className="font-medium text-slate-800">{ticket.subject}</p>
                    <p className="text-slate-600">{ticket.message}</p>
                    <p className="mt-1 text-xs text-slate-400">{formatDateTime(ticket.createdAt)}</p>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
