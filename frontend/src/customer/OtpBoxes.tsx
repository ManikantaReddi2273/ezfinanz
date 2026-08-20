import { useRef } from "react";

export function OtpBoxes({
  value,
  onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  const refs = useRef<Array<HTMLInputElement | null>>([]);
  const digits = value.padEnd(6, " ").slice(0, 6).split("");

  const setDigit = (index: number, char: string) => {
    const next = value.split("");
    while (next.length < 6) {
      next.push("");
    }
    next[index] = char;
    onChange(next.join("").replace(/\s/g, "").slice(0, 6));
  };

  return (
    <div className="flex justify-center gap-2">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(node) => {
            refs.current[index] = node;
          }}
          inputMode="numeric"
          maxLength={1}
          value={digit.trim()}
          onChange={(event) => {
            const char = event.target.value.replace(/\D/g, "").slice(-1);
            setDigit(index, char);
            if (char) {
              refs.current[index + 1]?.focus();
            }
          }}
          onKeyDown={(event) => {
            if (event.key === "Backspace" && !digits[index].trim()) {
              refs.current[index - 1]?.focus();
            }
          }}
          onPaste={(event) => {
            event.preventDefault();
            const pasted = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
            onChange(pasted);
          }}
          className="h-11 w-9 rounded-md border border-slate-200 text-center text-lg font-semibold text-slate-900 outline-none focus:border-blue-600"
        />
      ))}
    </div>
  );
}
