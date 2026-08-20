import { EmiTermsPanel } from "./EmiTermsPanel";
import { FlowCard } from "./FlowCard";

export function EmiStep({ onContinue, readOnly }: { onContinue: () => void; readOnly?: boolean }) {
  return (
    <FlowCard>
      <EmiTermsPanel readOnly={readOnly} onContinue={onContinue} />
    </FlowCard>
  );
}
