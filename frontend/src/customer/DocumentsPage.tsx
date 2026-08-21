/**
 * Lists and downloads KYC / selfie files attached to the customer's application.
 */
import { FileText, Image } from "lucide-react";
import { useEffect, useState } from "react";
import { customerApi, kycApi, selfieApi, type CustomerDashboardData } from "../api/client";

async function download(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}

/** Customer documents page with download actions for stored files. */
export function DocumentsPage() {
  const [data, setData] = useState<CustomerDashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    customerApi
      .dashboard()
      .then(setData)
      .catch(() => setError("Could not load documents."));
  }, []);

  return (
    <section className="max-w-xl rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">Documents</h2>
      <p className="mt-1 text-sm text-slate-500">Files stored with your application on the server.</p>
      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}
      <ul className="mt-5 space-y-3">
        <li className="flex items-center justify-between rounded-xl border border-slate-100 px-4 py-3">
          <span className="flex items-center gap-2 text-sm font-medium text-slate-800">
            <FileText className="h-4 w-4 text-blue-600" />
            KYC ID {data?.kycDocumentName ? `(${data.kycDocumentName})` : ""}
          </span>
          {data?.hasKycDocument ? (
            <button
              type="button"
              className="text-sm font-semibold text-blue-700"
              onClick={async () => {
                const blob = await kycApi.documentBlob();
                await download(blob, data.kycDocumentName || "kyc-document");
              }}
            >
              Download
            </button>
          ) : (
            <span className="text-xs text-slate-400">Not uploaded</span>
          )}
        </li>
        <li className="flex items-center justify-between rounded-xl border border-slate-100 px-4 py-3">
          <span className="flex items-center gap-2 text-sm font-medium text-slate-800">
            <Image className="h-4 w-4 text-blue-600" />
            Live selfie
          </span>
          {data?.hasSelfie ? (
            <button
              type="button"
              className="text-sm font-semibold text-blue-700"
              onClick={async () => {
                const blob = await selfieApi.photoBlob();
                await download(blob, "selfie.jpg");
              }}
            >
              Download
            </button>
          ) : (
            <span className="text-xs text-slate-400">Not submitted</span>
          )}
        </li>
      </ul>
    </section>
  );
}
