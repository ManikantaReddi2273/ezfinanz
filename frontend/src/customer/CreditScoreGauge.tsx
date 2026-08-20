export function CreditScoreGauge({ score, band }: { score: number; band?: string }) {
  const min = 300;
  const max = 900;
  const clamped = Math.min(max, Math.max(min, score));
  const ratio = (clamped - min) / (max - min);
  const dash = Math.PI * 78;
  const filled = dash * ratio;
  const label = band ? band.charAt(0) + band.slice(1).toLowerCase() : "";

  return (
    <div className="relative mx-auto w-[240px]">
      <svg viewBox="0 0 240 140" className="w-full">
        <path
          d="M42 108 A78 78 0 0 1 198 108"
          fill="none"
          stroke="#E5E7EB"
          strokeWidth="14"
          strokeLinecap="round"
        />
        <path
          d="M42 108 A78 78 0 0 1 198 108"
          fill="none"
          stroke="#22C55E"
          strokeWidth="14"
          strokeLinecap="round"
          strokeDasharray={`${filled} ${dash}`}
        />
      </svg>
      <div className="absolute inset-x-0 top-[48px] text-center">
        <p className="text-[11px] font-medium text-slate-500">Credit Score</p>
        <p className="text-3xl font-bold text-slate-900">{score}</p>
        {label && <p className="text-xs font-semibold text-emerald-600">{label}</p>}
      </div>
    </div>
  );
}
