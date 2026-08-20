import type { User } from "../api/client";
import { isFullyVerified } from "../auth/paths";

export type StepId =
  | "account"
  | "verify"
  | "kyc"
  | "eligibility"
  | "emi"
  | "bank"
  | "declaration"
  | "selfie";

export type StepStatus = "complete" | "current" | "locked";

export type LoanStep = {
  id: StepId;
  number: number;
  label: string;
  hint: string;
};

export const LOAN_STEPS: LoanStep[] = [
  { id: "account", number: 1, label: "Sign Up & Login", hint: "Account created" },
  { id: "verify", number: 2, label: "Verify Email & Phone", hint: "Verify both contacts" },
  { id: "kyc", number: 3, label: "KYC Details", hint: "Identity and address" },
  { id: "eligibility", number: 4, label: "Loan Eligibility", hint: "Income and credit check" },
  { id: "emi", number: 5, label: "EMI Selection", hint: "Amount and tenure" },
  { id: "bank", number: 6, label: "Bank Account", hint: "Disbursement details" },
  { id: "declaration", number: 7, label: "Declaration", hint: "Confirm terms" },
  { id: "selfie", number: 8, label: "Selfie Verification", hint: "Admin review" },
];

export function isApplicationRejected(user: User): boolean {
  return user.selfieStatus === "REJECTED";
}

export function isApplicationSubmitted(user: User): boolean {
  return Boolean(
    user.disbursed || user.selfieStatus === "APPROVED" || user.selfieStatus === "PENDING",
  );
}

export function isSelfieDraft(user: User): boolean {
  return user.selfieStatus === "DRAFT";
}

export function isReadyToSend(user: User): boolean {
  return Boolean(
    isSelfieDraft(user) &&
      user.declarationCompleted &&
      user.bankCompleted &&
      user.emiCompleted &&
      user.eligibilityPassed,
  );
}

export function maxReachableStepIndex(user: User): number {
  if (user.selfieSubmitted && user.selfieStatus !== "REJECTED") {
    return 7;
  }
  if (user.declarationCompleted) {
    return 7;
  }
  if (user.bankCompleted) {
    return 6;
  }
  if (user.emiCompleted) {
    return 5;
  }
  if (user.eligibilityPassed) {
    return 4;
  }
  if (user.eligibilityCompleted) {
    return 3;
  }
  if (user.kycCompleted) {
    return 2;
  }
  if (isFullyVerified(user)) {
    return 1;
  }
  return 0;
}

export function canNavigateToStep(user: User, stepId: StepId): boolean {
  if (stepId === "account") {
    return true;
  }
  if (isApplicationSubmitted(user)) {
    return stepStatus(user, stepId) !== "locked";
  }
  const idx = LOAN_STEPS.findIndex((step) => step.id === stepId);
  return idx >= 0 && idx <= maxReachableStepIndex(user);
}

export function stepStatus(user: User, stepId: StepId): StepStatus {
  const verified = isFullyVerified(user);
  const kycDone = Boolean(user.kycCompleted);
  const eligibilityDone = Boolean(user.eligibilityCompleted);
  const eligibilityPassed = Boolean(user.eligibilityPassed);

  if (stepId === "account") {
    return "complete";
  }
  if (stepId === "verify") {
    return verified ? "complete" : "current";
  }
  if (stepId === "kyc") {
    if (!verified) {
      return "locked";
    }
    return kycDone ? "complete" : "current";
  }
  if (stepId === "eligibility") {
    if (!kycDone) {
      return "locked";
    }
    return eligibilityDone ? "complete" : "current";
  }
  if (stepId === "emi") {
    if (!eligibilityPassed) {
      return "locked";
    }
    return user.emiCompleted ? "complete" : "current";
  }
  if (stepId === "bank") {
    if (!user.emiCompleted) {
      return "locked";
    }
    return user.bankCompleted ? "complete" : "current";
  }
  if (stepId === "declaration") {
    if (!user.bankCompleted) {
      return "locked";
    }
    return user.declarationCompleted ? "complete" : "current";
  }
  if (stepId === "selfie") {
    if (!user.declarationCompleted) {
      return "locked";
    }
    if (user.disbursed || user.selfieStatus === "APPROVED" || user.selfieStatus === "PENDING" || user.selfieStatus === "DRAFT") {
      return "complete";
    }
    if (user.selfieStatus === "REJECTED") {
      return "current";
    }
    return "current";
  }
  return "locked";
}

export function isStepReadOnly(user: User, stepId: StepId): boolean {
  if (isApplicationSubmitted(user)) {
    return true;
  }
  if (stepId === "account") {
    return true;
  }
  return false;
}

export function defaultStep(user: User): StepId {
  if (!isFullyVerified(user)) {
    return "verify";
  }
  if (!user.kycCompleted) {
    return "kyc";
  }
  if (!user.eligibilityPassed) {
    return "eligibility";
  }
  if (!user.emiCompleted) {
    return "emi";
  }
  if (!user.bankCompleted) {
    return "bank";
  }
  if (!user.declarationCompleted) {
    return "declaration";
  }
  if (isReadyToSend(user)) {
    return "selfie";
  }
  if (!user.selfieSubmitted || user.selfieStatus === "REJECTED") {
    return "selfie";
  }
  return "selfie";
}

export function completedCount(user: User): number {
  let count = 1;
  if (isFullyVerified(user)) {
    count += 1;
  }
  if (user.kycCompleted) {
    count += 1;
  }
  if (user.eligibilityCompleted) {
    count += 1;
  }
  if (user.emiCompleted) {
    count += 1;
  }
  if (user.bankCompleted) {
    count += 1;
  }
  if (user.declarationCompleted) {
    count += 1;
  }
  if (user.selfieSubmitted && user.selfieStatus !== "REJECTED") {
    count += 1;
  }
  return count;
}

export function lockMessage(user: User): string {
  if (!isFullyVerified(user)) {
    return "Verify your email and phone before continuing.";
  }
  if (!user.kycCompleted) {
    return "Complete KYC before opening later steps.";
  }
  if (!user.eligibilityPassed) {
    return "You need an eligible or partially eligible result before EMI terms.";
  }
  if (!user.emiCompleted) {
    return "Confirm EMI terms before adding a bank account.";
  }
  if (!user.bankCompleted) {
    return "Add a disbursement bank account before the declaration.";
  }
  if (!user.declarationCompleted) {
    return "Accept the declaration before confirming your selfie.";
  }
  return "Complete the previous step before continuing.";
}
