/**
 * Application step: EMI amount/tenure selection (wraps {@link EmiTermsPanel}).
 */
import { EmiTermsPanel } from "./EmiTermsPanel";
import { FlowCard } from "./FlowCard";

/** Thin FlowCard wrapper around the EMI terms panel. */
export function EmiStep({ onContinue, readOnly }: { onContinue: () => void; readOnly?: boolean }) {
  return (
    <FlowCard>
      <EmiTermsPanel readOnly={readOnly} onContinue={onContinue} />
    </FlowCard>
  );
}
