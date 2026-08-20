import { Bot, MessageCircle, Send, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { ApiError, supportApi } from "../api/client";

type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
  sources?: string[];
};

const WELCOME: ChatMessage = {
  id: "welcome",
  role: "assistant",
  text: "Hi there 👋 I'm the EZFINANZ Assistant. Ask me about application steps, eligibility, EMI, KYC, selfie review, or resubmitting after rejection.",
};

export function SupportChatWidget() {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME]);
  const endRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    endRef.current?.scrollIntoView({ behavior: "smooth" });
    window.setTimeout(() => inputRef.current?.focus(), 80);
  }, [open, messages, busy]);

  const send = async () => {
    const text = input.trim();
    if (text.length < 1 || busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setInput("");
    setMessages((prev) => [...prev, { id: `u-${Date.now()}`, role: "user", text }]);
    try {
      const row = await supportApi.chat(text);
      setMessages((prev) => [
        ...prev,
        {
          id: `a-${Date.now()}`,
          role: "assistant",
          text: row.reply,
          sources: row.sources,
        },
      ]);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not reach the assistant.");
      setMessages((prev) => [
        ...prev,
        {
          id: `e-${Date.now()}`,
          role: "assistant",
          text: "Sorry — I could not reply just now. Please try again, or use Contact Support in Help & Support.",
        },
      ]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="pointer-events-none fixed bottom-5 right-5 z-50 flex flex-col items-end gap-3 sm:bottom-6 sm:right-6">
      {open && (
        <div className="pointer-events-auto flex h-[min(560px,calc(100vh-7rem))] w-[min(380px,calc(100vw-1.5rem))] flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl">
          <header className="flex items-center justify-between gap-3 bg-blue-600 px-4 py-3 text-white">
            <div className="flex min-w-0 items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white/15">
                <Bot className="h-5 w-5" />
              </span>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold">EZFINANZ Assistant</p>
                <p className="truncate text-xs text-blue-100">The team can also help via Contact Support</p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="rounded-full p-1.5 text-white/90 transition hover:bg-white/15"
              aria-label="Close chat"
            >
              <X className="h-5 w-5" />
            </button>
          </header>

          <div className="flex-1 space-y-3 overflow-y-auto bg-slate-50 px-3 py-4">
            {messages.map((item) => (
              <div key={item.id} className={`flex ${item.role === "user" ? "justify-end" : "justify-start"}`}>
                <div
                  className={`max-w-[85%] rounded-2xl px-3.5 py-2.5 text-sm leading-6 ${
                    item.role === "user"
                      ? "rounded-br-md bg-blue-600 text-white"
                      : "rounded-bl-md bg-white text-slate-700 shadow-sm"
                  }`}
                >
                  <p className="whitespace-pre-wrap">{item.text}</p>
                  {item.sources && item.sources.length > 0 && (
                    <p className="mt-2 text-[11px] text-slate-400">Sources: {item.sources.join(", ")}</p>
                  )}
                  {item.role === "assistant" && (
                    <p className="mt-1.5 text-[11px] text-slate-400">EZFINANZ Assistant · AI</p>
                  )}
                </div>
              </div>
            ))}
            {busy && <p className="px-1 text-xs text-slate-500">Assistant is typing…</p>}
            <div ref={endRef} />
          </div>

          <div className="border-t border-slate-200 bg-white p-3">
            {error && <p className="mb-2 text-xs text-red-600">{error}</p>}
            <div className="flex items-end gap-2 rounded-2xl border-2 border-slate-900/80 bg-white p-2">
              <input
                ref={inputRef}
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" && !event.shiftKey) {
                    event.preventDefault();
                    void send();
                  }
                }}
                placeholder="Ask a question…"
                className="min-w-0 flex-1 bg-transparent px-2 py-2 text-sm text-slate-800 outline-none placeholder:text-slate-400"
              />
              <button
                type="button"
                disabled={busy || input.trim().length < 1}
                onClick={() => void send()}
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-600 text-white transition hover:bg-blue-700 disabled:bg-slate-300"
                aria-label="Send message"
              >
                <Send className="h-4 w-4" />
              </button>
            </div>
            <p className="mt-2 text-center text-[10px] text-slate-400">
              Answers use EZFINANZ help docs. For account-specific issues, use Contact Support.
            </p>
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="pointer-events-auto relative flex h-14 w-14 items-center justify-center rounded-full bg-blue-600 text-white shadow-lg transition hover:bg-blue-700 hover:shadow-xl"
        aria-label={open ? "Close support chat" : "Open support chat"}
      >
        {open ? <X className="h-6 w-6" /> : <MessageCircle className="h-6 w-6" />}
        {!open && <span className="absolute -right-0.5 -top-0.5 h-3 w-3 rounded-full bg-rose-500 ring-2 ring-white" />}
      </button>
    </div>
  );
}
