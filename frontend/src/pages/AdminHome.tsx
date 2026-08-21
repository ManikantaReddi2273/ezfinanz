/**
 * Admin console home: dashboard stats, applications table, users, knowledge, settings.
 * Section is chosen from the current `/admin/*` pathname.
 */
import { ArrowDownRight, ArrowUpRight, CheckCircle2, ClipboardList, IdCard, Search, ThumbsDown } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ApiError, adminApi, type AdminAccount, type AdminApplicationSummary, type KnowledgeDocument } from "../api/client";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { formatDateTime, rupee } from "../lib/money";
import { AdminLayout, applicationId, stageBadgeClass } from "./AdminLayout";

function weekDelta(rows: AdminApplicationSummary[], match: (row: AdminApplicationSummary) => boolean): number {
  const now = Date.now();
  const week = 7 * 24 * 60 * 60 * 1000;
  const thisWeek = rows.filter((row) => match(row) && now - new Date(row.submittedAt).getTime() < week).length;
  const lastWeek = rows.filter((row) => {
    const age = now - new Date(row.submittedAt).getTime();
    return match(row) && age >= week && age < week * 2;
  }).length;
  if (lastWeek === 0) {
    return thisWeek > 0 ? 100 : 0;
  }
  return Math.round(((thisWeek - lastWeek) / lastWeek) * 100);
}

/** Multi-section admin hub (applications list, staff, knowledge uploads, settings). */
export function AdminHome() {
  const location = useLocation();
  const [rows, setRows] = useState<AdminApplicationSummary[]>([]);
  const [headerQuery, setHeaderQuery] = useState("");
  const [tableQuery, setTableQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [error, setError] = useState<string | null>(null);
  const section = location.pathname.includes("/users")
    ? "users"
    : location.pathname.includes("/knowledge")
      ? "knowledge"
      : location.pathname.includes("/settings")
        ? "settings"
        : location.pathname.endsWith("/applications")
          ? "applications"
          : "dashboard";

  useEffect(() => {
    adminApi
      .list()
      .then(setRows)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Could not load applications."));
  }, []);

  const stats = useMemo(() => {
    const pending = (row: AdminApplicationSummary) =>
      row.currentStage === "WAITING_FOR_ADMIN_REVIEW" || row.currentStage === "LIVE_SELFIE";
    const approved = (row: AdminApplicationSummary) =>
      row.currentStage === "READY_FOR_DISBURSEMENT" || row.currentStage === "DISBURSED";
    const rejected = (row: AdminApplicationSummary) =>
      row.currentStage === "SELFIE_REJECTED" || row.currentStage === "NOT_ELIGIBLE";
    return {
      total: rows.length,
      pending: rows.filter(pending).length,
      approved: rows.filter(approved).length,
      rejected: rows.filter(rejected).length,
      totalDelta: weekDelta(rows, () => true),
      pendingDelta: weekDelta(rows, pending),
      approvedDelta: weekDelta(rows, approved),
      rejectedDelta: weekDelta(rows, rejected),
    };
  }, [rows]);

  const stages = useMemo(() => ["ALL", ...Array.from(new Set(rows.map((row) => row.currentStage)))], [rows]);

  const filtered = rows.filter((row) => {
    const hay = `${applicationId(row.userId)} ${row.applicantName} ${row.email} ${row.phone} ${row.currentStageLabel}`.toLowerCase();
    const q = (section === "dashboard" ? headerQuery || tableQuery : headerQuery || tableQuery).toLowerCase();
    const matchesQuery = !q || hay.includes(q);
    const matchesStatus = status === "ALL" || row.currentStage === status;
    return matchesQuery && matchesStatus;
  });

  const tableRows = section === "dashboard" ? filtered.slice(0, 8) : filtered;
  const title =
    section === "users"
      ? "Users"
      : section === "knowledge"
        ? "Knowledge base"
        : section === "settings"
          ? "Settings"
          : section === "applications"
            ? "Applications"
            : "Admin Dashboard";

  return (
    <AdminLayout
      title={title}
      headerRight={
        <label className="relative hidden sm:block">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            value={headerQuery}
            onChange={(event) => setHeaderQuery(event.target.value)}
            placeholder="Search by name or ID"
            className="w-64 rounded-full border border-slate-200 bg-white py-2 pl-9 pr-4 text-sm outline-none focus:border-blue-600"
          />
        </label>
      }
    >
      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      {section === "users" && <UsersPanel rows={rows} />}
      {section === "knowledge" && <KnowledgeBasePanel />}
      {section === "settings" && <SettingsPanel />}

      {(section === "dashboard" || section === "applications") && (
        <>
          {section === "dashboard" && (
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Kpi label="Total Applications" value={stats.total} delta={stats.totalDelta} icon={IdCard} iconClass="bg-blue-50 text-blue-600" />
              <Kpi label="Pending Review" value={stats.pending} delta={stats.pendingDelta} invertGood icon={ClipboardList} iconClass="bg-violet-50 text-violet-600" />
              <Kpi label="Approved" value={stats.approved} delta={stats.approvedDelta} icon={CheckCircle2} iconClass="bg-emerald-50 text-emerald-600" />
              <Kpi label="Rejected" value={stats.rejected} delta={stats.rejectedDelta} invertGood icon={ThumbsDown} iconClass="bg-rose-50 text-rose-600" />
            </div>
          )}

          <section className="mt-6 rounded-2xl bg-white p-5 shadow-sm">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-base font-semibold text-slate-900">
                {section === "dashboard" ? "Recent Applications" : "All Applications"}
              </h2>
              <div className="flex flex-wrap items-center gap-2">
                <label className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input
                    value={tableQuery}
                    onChange={(event) => setTableQuery(event.target.value)}
                    placeholder="Search by name or ID"
                    className="w-52 rounded-full border border-slate-200 py-2 pl-9 pr-3 text-sm outline-none focus:border-blue-600"
                  />
                </label>
                <select
                  value={status}
                  onChange={(event) => setStatus(event.target.value)}
                  className="rounded-full border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none"
                >
                  <option value="ALL">All Status</option>
                  {stages
                    .filter((item) => item !== "ALL")
                    .map((item) => (
                      <option key={item} value={item}>
                        {rows.find((row) => row.currentStage === item)?.currentStageLabel || item}
                      </option>
                    ))}
                </select>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="text-xs font-medium uppercase tracking-wide text-slate-400">
                  <tr>
                    <th className="px-3 py-3">ID</th>
                    <th className="px-3 py-3">Applicant Name</th>
                    <th className="px-3 py-3">Loan Amount</th>
                    <th className="px-3 py-3">Tenure</th>
                    <th className="px-3 py-3">Current Stage</th>
                    <th className="px-3 py-3">Submitted On</th>
                    <th className="px-3 py-3">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {tableRows.map((row) => (
                    <tr key={row.userId} className="border-t border-slate-100">
                      <td className="px-3 py-3">
                        <Link to={`/admin/applications/${row.userId}`} className="font-medium text-blue-600 hover:underline">
                          {applicationId(row.userId)}
                        </Link>
                      </td>
                      <td className="px-3 py-3 font-medium text-slate-900">{row.applicantName || "Unnamed"}</td>
                      <td className="px-3 py-3">
                        {rupee.format(row.selectedLoanAmount ?? row.requestedLoanAmount ?? 0)}
                      </td>
                      <td className="px-3 py-3">{row.tenureMonths ? `${row.tenureMonths} Months` : "—"}</td>
                      <td className="px-3 py-3">
                        <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${stageBadgeClass(row.currentStage)}`}>
                          {row.currentStage === "WAITING_FOR_ADMIN_REVIEW" || row.currentStage === "LIVE_SELFIE"
                            ? "Selfie Pending"
                            : row.currentStageLabel}
                        </span>
                      </td>
                      <td className="px-3 py-3 text-slate-500">{formatDateTime(row.submittedAt)}</td>
                      <td className="px-3 py-3">
                        <Link
                          to={`/admin/applications/${row.userId}`}
                          className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                        >
                          View
                        </Link>
                      </td>
                    </tr>
                  ))}
                  {tableRows.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-3 py-8 text-center text-slate-500">
                        No applications match this search.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </AdminLayout>
  );
}

function Kpi({
  label,
  value,
  delta,
  invertGood,
  icon: Icon,
  iconClass,
}: {
  label: string;
  value: number;
  delta: number;
  invertGood?: boolean;
  icon: typeof IdCard;
  iconClass: string;
}) {
  const up = delta >= 0;
  const good = invertGood ? !up : up;
  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="mt-2 text-3xl font-bold text-slate-900">{value}</p>
          <p className={`mt-2 flex items-center gap-1 text-xs font-medium ${good ? "text-emerald-600" : "text-rose-600"}`}>
            {up ? <ArrowUpRight className="h-3.5 w-3.5" /> : <ArrowDownRight className="h-3.5 w-3.5" />}
            {up ? "+" : ""}
            {delta}% this week
          </p>
        </div>
        <span className={`flex h-10 w-10 items-center justify-center rounded-full ${iconClass}`}>
          <Icon className="h-5 w-5" />
        </span>
      </div>
    </div>
  );
}

function UsersPanel({ rows }: { rows: AdminApplicationSummary[] }) {
  return (
    <section className="rounded-2xl bg-white p-5 shadow-sm">
      <h2 className="text-base font-semibold text-slate-900">Users</h2>
      <p className="mt-1 text-sm text-slate-500">Customer accounts with an application record.</p>
      <ul className="mt-4 divide-y divide-slate-100">
        {rows.map((row) => (
          <li key={row.userId} className="flex items-center justify-between py-3 text-sm">
            <div>
              <p className="font-medium text-slate-900">{row.applicantName || "Unnamed"}</p>
              <p className="text-slate-500">{row.email || row.phone}</p>
            </div>
            <Link to={`/admin/applications/${row.userId}`} className="font-semibold text-blue-700">
              View
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

function SettingsPanel() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [admins, setAdmins] = useState<AdminAccount[]>([]);
  const [canCreate, setCanCreate] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);

  const load = () => {
    adminApi
      .listAdmins()
      .then((page) => {
        setAdmins(page.admins);
        setCanCreate(page.canCreateAdmins);
      })
      .catch(() => undefined);
  };

  useEffect(() => {
    load();
  }, []);

  const create = async () => {
    setSaving(true);
    setError(null);
    setSaved(null);
    try {
      await adminApi.createAdmin({
        email,
        password,
        fullName: fullName.trim() || undefined,
      });
      setEmail("");
      setPassword("");
      setFullName("");
      setSaved("New admin account created. They can sign in on the same login page.");
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not create the admin.");
    } finally {
      setSaving(false);
    }
  };

  const removeAdmin = async () => {
    if (deleteId == null) {
      return;
    }
    setDeleting(true);
    setError(null);
    setSaved(null);
    try {
      await adminApi.deleteAdmin(deleteId);
      setSaved("Admin account deleted.");
      setDeleteId(null);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not delete the admin.");
    } finally {
      setDeleting(false);
    }
  };

  const pendingDelete = admins.find((admin) => admin.id === deleteId);

  return (
    <div className={`grid gap-6 ${canCreate ? "lg:grid-cols-[1.1fr_0.9fr]" : ""}`}>
      <ConfirmDialog
        open={deleteId != null}
        title="Delete admin?"
        message={
          pendingDelete
            ? `Remove ${pendingDelete.fullName || "this admin"} (${pendingDelete.email})? They will no longer be able to sign in.`
            : "Remove this admin account?"
        }
        confirmLabel="Delete admin"
        tone="red"
        busy={deleting}
        onConfirm={() => void removeAdmin()}
        onCancel={() => setDeleteId(null)}
      />
      {canCreate && (
        <section className="rounded-2xl bg-white p-6 shadow-sm">
          <h2 className="text-base font-semibold text-slate-900">Add new admin</h2>
          <p className="mt-1 text-sm text-slate-500">Only the super admin can create another admin login.</p>
          <label className="mt-5 block text-sm font-medium text-slate-700">
            Full name (optional)
            <input
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-blue-600"
            />
          </label>
          <label className="mt-4 block text-sm font-medium text-slate-700">
            Email
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-blue-600"
            />
          </label>
          <label className="mt-4 block text-sm font-medium text-slate-700">
            Password
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-blue-600"
            />
            <span className="mt-1 block text-xs font-normal text-slate-500">At least 8 characters.</span>
          </label>
          {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
          {saved && <p className="mt-3 text-sm text-emerald-700">{saved}</p>}
          <button
            type="button"
            disabled={saving || !email.trim() || password.length < 8}
            onClick={() => void create()}
            className="mt-5 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
          >
            {saving ? "Creating…" : "Add admin"}
          </button>
        </section>
      )}
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">Admin accounts</h2>
        {!canCreate && (
          <p className="mt-1 text-sm text-slate-500">You can view admin accounts. Only the super admin can add or delete them.</p>
        )}
        {canCreate && (
          <p className="mt-1 text-sm text-slate-500">You can add or remove admin logins. The super admin cannot be deleted.</p>
        )}
        {!canCreate && error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        {!canCreate && saved && <p className="mt-3 text-sm text-emerald-700">{saved}</p>}
        <ul className="mt-4 divide-y divide-slate-100 text-sm">
          {admins.map((admin) => (
            <li key={admin.id} className="flex items-center justify-between gap-3 py-3">
              <div>
                <div className="flex items-center gap-2">
                  <p className="font-medium text-slate-900">{admin.fullName || "Admin"}</p>
                  {admin.superAdmin && (
                    <span className="rounded-md bg-blue-50 px-2 py-0.5 text-[11px] font-semibold text-blue-700">Super admin</span>
                  )}
                </div>
                <p className="text-slate-500">{admin.email}</p>
              </div>
              {canCreate && !admin.superAdmin && (
                <button
                  type="button"
                  disabled={deleting}
                  onClick={() => setDeleteId(admin.id)}
                  className="rounded-lg border border-rose-200 px-3 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50 disabled:opacity-60"
                >
                  Delete
                </button>
              )}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function KnowledgeBasePanel() {
  const [docs, setDocs] = useState<KnowledgeDocument[]>([]);
  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [canManage, setCanManage] = useState<boolean | null>(null);

  const load = () => {
    adminApi
      .listKnowledgeDocs()
      .then(setDocs)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Could not load knowledge documents."));
  };

  useEffect(() => {
    adminApi
      .listAdmins()
      .then((page) => {
        setCanManage(page.canCreateAdmins);
        if (page.canCreateAdmins) {
          load();
        }
      })
      .catch(() => setCanManage(false));
  }, []);

  const upload = async () => {
    if (!file) {
      return;
    }
    setUploading(true);
    setError(null);
    setSaved(null);
    try {
      await adminApi.uploadKnowledgeDoc(file, title.trim() || undefined);
      setTitle("");
      setFile(null);
      setSaved("Document uploaded and indexed for the support chatbot.");
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not upload the document.");
    } finally {
      setUploading(false);
    }
  };

  const removeDoc = async () => {
    if (deleteId == null) {
      return;
    }
    setDeleting(true);
    setError(null);
    setSaved(null);
    try {
      await adminApi.deleteKnowledgeDoc(deleteId);
      setDeleteId(null);
      setSaved("Document removed from the knowledge base.");
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not delete the document.");
    } finally {
      setDeleting(false);
    }
  };

  const pending = docs.find((doc) => doc.id === deleteId);

  if (canManage === null) {
    return <p className="text-sm text-slate-500">Loading knowledge base…</p>;
  }

  if (!canManage) {
    return (
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">Knowledge base</h2>
        <p className="mt-2 text-sm text-slate-500">
          Only the super admin can upload and manage chatbot knowledge documents.
        </p>
      </section>
    );
  }

  return (
    <section className="rounded-2xl bg-white p-6 shadow-sm">
      <ConfirmDialog
        open={deleteId != null}
        title="Delete document?"
        message={
          pending
            ? `Remove "${pending.title}" from the chatbot knowledge base?`
            : "Remove this document from the knowledge base?"
        }
        confirmLabel="Delete document"
        tone="red"
        busy={deleting}
        onConfirm={() => void removeDoc()}
        onCancel={() => setDeleteId(null)}
      />
      <h2 className="text-base font-semibold text-slate-900">Knowledge base</h2>
      <p className="mt-1 text-sm text-slate-500">
        Upload FAQ, policy, or how-to docs (.txt, .md, .pdf). They power the customer support chatbot via Pinecone + OpenAI.
        Starter files live in <code className="text-xs">docs/knowledge-base/</code>.
      </p>
      <div className="mt-5 grid gap-3 sm:grid-cols-[1fr_auto]">
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Optional title"
          className="rounded-lg border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-blue-600"
        />
        <label className="inline-flex cursor-pointer items-center justify-center rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50">
          Choose file
          <input
            type="file"
            accept=".txt,.md,.pdf,text/plain,text/markdown,application/pdf"
            className="hidden"
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null);
              event.target.value = "";
            }}
          />
        </label>
      </div>
      {file && <p className="mt-2 text-xs text-slate-500">Selected: {file.name}</p>}
      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      {saved && <p className="mt-3 text-sm text-emerald-700">{saved}</p>}
      <button
        type="button"
        disabled={uploading || !file}
        onClick={() => void upload()}
        className="mt-4 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
      >
        {uploading ? "Indexing…" : "Upload & index"}
      </button>
      <ul className="mt-6 divide-y divide-slate-100 text-sm">
        {docs.length === 0 && <li className="py-3 text-slate-500">No documents indexed yet.</li>}
        {docs.map((doc) => (
          <li key={doc.id} className="flex items-start justify-between gap-3 py-3">
            <div>
              <p className="font-medium text-slate-900">{doc.title}</p>
              <p className="text-slate-500">
                {doc.originalName} · {doc.status}
                {doc.status === "INDEXED" ? ` · ${doc.chunkCount} chunks` : ""}
              </p>
              {doc.errorMessage && <p className="mt-1 text-xs text-rose-600">{doc.errorMessage}</p>}
            </div>
            <button
              type="button"
              disabled={deleting}
              onClick={() => setDeleteId(doc.id)}
              className="rounded-lg border border-rose-200 px-3 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50 disabled:opacity-60"
            >
              Delete
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
