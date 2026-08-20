import { useEffect, useRef, useState } from "react";
import { ApiError, authApi, selfieApi, type SelfieStatus } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { CheckCircle2, CircleAlert, Glasses, Sun, UserRound } from "lucide-react";
import { FlowCard, flowPrimary } from "./FlowCard";

export function SelfieStep({ readOnly }: { readOnly?: boolean }) {
  const { user, updateUser } = useAuth();
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const cameraSession = useRef(0);
  const [status, setStatus] = useState<SelfieStatus | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [cameraOn, setCameraOn] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

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

  useEffect(() => {
    let cancelled = false;
    selfieApi
      .get()
      .then(async (row) => {
        if (cancelled) {
          return;
        }
        setStatus(row);
        try {
          const blob = await selfieApi.photoBlob();
          if (!cancelled) {
            setPreview(URL.createObjectURL(blob));
          }
        } catch {
          if (!cancelled) {
            setPreview(null);
          }
        }
      })
      .catch((err) => {
        if (cancelled) {
          return;
        }
        if (!(err instanceof ApiError) || err.status !== 404) {
          setError(err instanceof ApiError ? err.message : "Could not load selfie status.");
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const alreadyDone =
      readOnly || user?.disbursed || user?.selfieStatus === "APPROVED" || user?.selfieStatus === "PENDING";
    if (!alreadyDone) {
      void startCamera();
    }
    return () => stopCamera();
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
    setPreview(URL.createObjectURL(blob));
    stopCamera();
    await submitFile(file);
  };

  const submitFile = async (photo: File) => {
    setSaving(true);
    setError(null);
    try {
      const row = await selfieApi.submit(photo);
      setStatus(row);
      updateUser(await authApi.me());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the selfie.");
    } finally {
      setSaving(false);
    }
  };

  const pending = status?.reviewStatus === "PENDING";
  const approved = status?.reviewStatus === "APPROVED";
  const rejected = status?.reviewStatus === "REJECTED";
  const canCapture = !readOnly && !approved && !status?.disbursed;

  return (
    <FlowCard>
    <div className="text-center">
      <p className="text-sm leading-6 text-slate-600">Please look into the camera and take a clear selfie.</p>

      {pending && (
        <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-left text-sm text-amber-950">
          Waiting for Admin Review. Your selfie was saved on the server.
        </div>
      )}
      {rejected && (
        <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-left text-sm text-rose-900">
          Selfie rejected. {status?.rejectionReason || "Please capture a clearer photo."}
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
          className="absolute inset-0 h-full w-full object-cover"
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

      {canCapture && (
        <div className="mt-6 space-y-3">
          {cameraOn ? (
            <button
              type="button"
              disabled={saving}
              onClick={() => void capture()}
              className={flowPrimary}
            >
              {saving ? "Saving…" : "Capture Photo"}
            </button>
          ) : (
            <button
              type="button"
              disabled={saving}
              onClick={() => void startCamera()}
              className={flowPrimary}
            >
              {saving ? "Saving…" : "Capture Photo"}
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
                setPreview(URL.createObjectURL(next));
                stopCamera();
                void submitFile(next);
              }}
            />
          </label>
          <p className="text-xs text-slate-400">Saved for admin review.</p>
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
