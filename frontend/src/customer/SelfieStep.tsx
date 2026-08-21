/**
 * Application step: live selfie capture, draft confirm, and send application.
 */
import { useEffect, useRef, useState } from "react";
import { ApiError, authApi, selfieApi, type SelfieStatus } from "../api/client";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { useAuth } from "../auth/AuthContext";
import { CheckCircle2, CircleAlert, Glasses, Send, Sun, UserRound } from "lucide-react";
import { FlowCard, flowGhost, flowPrimary } from "./FlowCard";
import { isApplicationRejected, isReadyToSend } from "./steps";

type PendingAction = "confirm" | "send" | null;

/** Camera selfie flow plus final submit for admin review. */
export function SelfieStep({ readOnly }: { readOnly?: boolean }) {
  const { user, updateUser } = useAuth();
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const cameraSession = useRef(0);
  const [status, setStatus] = useState<SelfieStatus | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [cameraOn, setCameraOn] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [sending, setSending] = useState(false);
  const [retaking, setRetaking] = useState(false);
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);

  const stopCamera = () => {
    cameraSession.current += 1;
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    setCameraOn(false);
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
  };

  const startCamera = async () => {
    setError(null);
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    const session = ++cameraSession.current;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "user" },
        audio: false,
      });
      if (session !== cameraSession.current) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }
      streamRef.current = stream;
      const video = videoRef.current;
      if (!video) {
        stream.getTracks().forEach((track) => track.stop());
        setError("Camera preview is not ready. Try Open camera again.");
        return;
      }
      video.srcObject = stream;
      await video.play();
      if (session !== cameraSession.current) {
        return;
      }
      setCameraOn(true);
    } catch (err) {
      if (session !== cameraSession.current) {
        return;
      }
      const name = err instanceof DOMException ? err.name : "";
      if (name === "NotAllowedError" || name === "PermissionDeniedError") {
        setError("Camera permission was denied. Allow the camera or upload a photo.");
      } else if (name === "NotFoundError") {
        setError("No camera was found. Upload a photo instead.");
      } else {
        setError("Could not open the camera. Use the page at http://localhost:5173 and upload a JPG or PNG instead.");
      }
    }
  };

  const loadSavedSelfie = async () => {
    try {
      const row = await selfieApi.get();
      setStatus(row);
      try {
        const blob = await selfieApi.photoBlob();
        setPreview(URL.createObjectURL(blob));
      } catch {
        setPreview(null);
      }
      return row;
    } catch (err) {
      if (!(err instanceof ApiError) || err.status !== 404) {
        setError(err instanceof ApiError ? err.message : "Could not load selfie status.");
      }
      return null;
    }
  };

  useEffect(() => {
    let cancelled = false;
    loadSavedSelfie().then((row) => {
      if (
        cancelled ||
        readOnly ||
        row?.reviewStatus === "DRAFT" ||
        row?.reviewStatus === "PENDING" ||
        row?.reviewStatus === "REJECTED"
      ) {
        return;
      }
      void startCamera();
    });
    return () => {
      cancelled = true;
      stopCamera();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const blobToFile = (blob: Blob, name: string) =>
    new File([blob], name, { type: blob.type || "image/jpeg" });

  const capture = async () => {
    const video = videoRef.current;
    if (!video || video.readyState < 2) {
      setError("Wait for the camera preview, then capture.");
      return;
    }
    const width = video.videoWidth || 640;
    const height = video.videoHeight || 480;
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext("2d");
    if (!context) {
      setError("Could not capture this frame.");
      return;
    }
    context.drawImage(video, 0, 0, width, height);
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.92));
    if (!blob) {
      setError("Could not create the photo. Try upload from gallery.");
      return;
    }
    const file = blobToFile(blob, "live-selfie.jpg");
    setPendingFile(file);
    setPreview(URL.createObjectURL(blob));
    stopCamera();
  };

  const retake = () => {
    setRetaking(true);
    setPendingFile(null);
    setPreview(null);
    setError(null);
    void startCamera();
  };

  const confirmSelfie = async () => {
    if (!pendingFile) {
      setError("Capture or upload a selfie first.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const row = await selfieApi.confirmDraft(pendingFile);
      setStatus(row);
      setPendingFile(null);
      setRetaking(false);
      updateUser(await authApi.me());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the selfie.");
    } finally {
      setSaving(false);
      setPendingAction(null);
    }
  };

  const sendApplication = async () => {
    setSending(true);
    setError(null);
    try {
      const row = await selfieApi.sendApplication();
      setStatus(row);
      updateUser(await authApi.me());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not send the application.");
    } finally {
      setSending(false);
      setPendingAction(null);
    }
  };

  const pending = status?.reviewStatus === "PENDING";
  const approved = status?.reviewStatus === "APPROVED";
  const rejected = status?.reviewStatus === "REJECTED";
  const draft = status?.reviewStatus === "DRAFT" && !retaking;
  const awaitingConfirm = Boolean(pendingFile);
  const resubmitting = rejected && !retaking && !draft && !awaitingConfirm;
  const canCapture = !readOnly && !approved && !status?.disbursed && !pending && !draft && !awaitingConfirm && !resubmitting;
  const readyToSend = Boolean(user && isReadyToSend(user));
  const canSendApplication = Boolean(draft || readyToSend) && Boolean(
    user?.declarationCompleted && user?.bankCompleted && user?.emiCompleted && user?.eligibilityPassed,
  );
  const showEditableBanner = !readOnly && !pending && !approved && !status?.disbursed;

  return (
    <FlowCard>
      <ConfirmDialog
        open={pendingAction === "confirm"}
        title="Confirm selfie?"
        message="Save this photo as your selfie? You can retake it before sending your application."
        confirmLabel="Confirm Selfie"
        tone="blue"
        busy={saving}
        onConfirm={() => void confirmSelfie()}
        onCancel={() => setPendingAction(null)}
      />
      <ConfirmDialog
        open={pendingAction === "send"}
        title="Send application?"
        message={
          isApplicationRejected(user!)
            ? "Send your updated application for admin review again? Make sure you have reviewed all steps and captured a new selfie."
            : "Submit your application for admin review? You will not be able to edit details after sending."
        }
        confirmLabel="Send Application"
        tone="green"
        busy={sending}
        onConfirm={() => void sendApplication()}
        onCancel={() => setPendingAction(null)}
      />

      <div className="text-center">
        <p className="text-sm leading-6 text-slate-600">
          Please look into the camera and take a clear selfie. Confirm it before sending your application.
        </p>

        {showEditableBanner && (
          <div className="mt-4 rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-left text-sm text-blue-900">
            {rejected
              ? "Your application was rejected. You can edit any previous step, capture a new selfie, and send the application again."
              : (
                <>
                  You can still edit any previous step before tapping <strong>Send Application</strong>.
                </>
              )}
          </div>
        )}

        {pending && (
          <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-left text-sm text-amber-950">
            Waiting for Admin Review. Your application has been sent successfully.
          </div>
        )}
        {draft && !pending && (
          <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-left text-sm text-emerald-900">
            Selfie confirmed. Review your details, then send the application when you are ready.
          </div>
        )}
        {rejected && !retaking && !draft && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-left text-sm text-rose-900">
            <p className="font-semibold">Application rejected</p>
            <p className="mt-1">{status?.rejectionReason || "Please review your details and capture a clearer selfie."}</p>
            <p className="mt-2 text-xs text-rose-800">Tap Retake Photo below to capture a new selfie, then send your application again.</p>
          </div>
        )}
        {approved && !status?.disbursed && (
          <div className="mt-4 flex items-start gap-2 rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-left text-sm text-emerald-900">
            <CheckCircle2 className="h-5 w-5 shrink-0" /> Selfie approved. Waiting for disbursement.
          </div>
        )}
        {status?.disbursed && (
          <div className="mt-4 flex items-start gap-2 rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-left text-sm text-emerald-900">
            <CheckCircle2 className="h-5 w-5 shrink-0" /> Loan disbursed to your registered bank account.
          </div>
        )}

        <div className="relative mx-auto mt-6 h-64 w-64 overflow-hidden rounded-full border-4 border-blue-100 bg-slate-100">
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className={`absolute inset-0 h-full w-full object-cover ${cameraOn ? "" : "opacity-0"}`}
          />
          {!cameraOn && preview && (
            <img src={preview} alt="Selfie" className="absolute inset-0 h-full w-full object-cover" />
          )}
          {!cameraOn && !preview && (
            <div className="absolute inset-0 flex items-center justify-center text-slate-300">
              <UserRound className="h-16 w-16" />
            </div>
          )}
        </div>

        <div className="mt-5 flex justify-center gap-6 text-xs text-slate-500">
          <span className="flex flex-col items-center gap-1">
            <Sun className="h-4 w-4 text-blue-600" /> Good lighting
          </span>
          <span className="flex flex-col items-center gap-1">
            <Glasses className="h-4 w-4 text-blue-600" /> No cap / glasses
          </span>
          <span className="flex flex-col items-center gap-1">
            <UserRound className="h-4 w-4 text-blue-600" /> Clear face
          </span>
        </div>

        {awaitingConfirm && (
          <div className="mt-6 space-y-3">
            <button
              type="button"
              disabled={saving}
              onClick={() => setPendingAction("confirm")}
              className={flowPrimary}
            >
              {saving ? "Saving…" : "Confirm Selfie"}
            </button>
            <button type="button" disabled={saving} onClick={retake} className={flowGhost}>
              Retake Photo
            </button>
          </div>
        )}

        {resubmitting && (
          <div className="mt-6 space-y-3">
            <button type="button" disabled={saving} onClick={retake} className={flowPrimary}>
              Retake Photo
            </button>
            <label className="block cursor-pointer text-sm font-semibold text-blue-700">
              Upload from Gallery
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/*"
                className="hidden"
                onChange={(event) => {
                  const next = event.target.files?.[0];
                  event.target.value = "";
                  if (!next) {
                    return;
                  }
                  setRetaking(true);
                  setPendingFile(next);
                  setPreview(URL.createObjectURL(next));
                  stopCamera();
                }}
              />
            </label>
          </div>
        )}

        {canCapture && (
          <div className="mt-6 space-y-3">
            {cameraOn ? (
              <button type="button" disabled={saving} onClick={() => void capture()} className={flowPrimary}>
                Capture Photo
              </button>
            ) : (
              <button type="button" disabled={saving} onClick={() => void startCamera()} className={flowPrimary}>
                Open Camera
              </button>
            )}
            <label className="block cursor-pointer text-sm font-semibold text-blue-700">
              Upload from Gallery
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/*"
                className="hidden"
                onChange={(event) => {
                  const next = event.target.files?.[0];
                  event.target.value = "";
                  if (!next) {
                    return;
                  }
                  setPendingFile(next);
                  setPreview(URL.createObjectURL(next));
                  stopCamera();
                }}
              />
            </label>
          </div>
        )}

        {(draft || readyToSend) && !pending && !approved && !status?.disbursed && (
          <div className="mt-6 space-y-3">
            <button
              type="button"
              disabled={sending || !canSendApplication}
              onClick={() => setPendingAction("send")}
              className="btn-hover-green flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 py-3 text-sm font-semibold text-white transition-all duration-200 hover:bg-emerald-700 disabled:opacity-60"
            >
              <Send className="h-4 w-4" />
              {sending ? "Sending…" : rejected ? "Resubmit Application" : "Send Application"}
            </button>
            {!canSendApplication && (
              <p className="text-xs text-slate-500">Complete all previous steps before sending your application.</p>
            )}
            <button type="button" disabled={saving || sending} onClick={retake} className={flowGhost}>
              Retake Photo
            </button>
          </div>
        )}

        {error && (
          <p className="mt-3 flex items-center justify-center gap-1 text-sm text-red-600">
            <CircleAlert className="h-4 w-4" />
            {error}
          </p>
        )}
      </div>
    </FlowCard>
  );
}
