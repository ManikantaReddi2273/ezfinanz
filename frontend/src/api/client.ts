export type Role = "CUSTOMER" | "ADMIN";

export type User = {
  id: number;
  fullName: string | null;
  email: string | null;
  phone: string | null;
  role: Role;
  emailVerified: boolean;
  phoneVerified: boolean;
  fullyVerified: boolean;
  kycCompleted: boolean;
  eligibilityCompleted: boolean;
  eligibilityPassed: boolean;
  eligibilityResult: "ELIGIBLE" | "PARTIALLY_ELIGIBLE" | "NOT_ELIGIBLE" | null;
  emiCompleted: boolean;
  bankCompleted: boolean;
  declarationCompleted: boolean;
  selfieSubmitted: boolean;
  selfieStatus: "PENDING" | "APPROVED" | "REJECTED" | null;
  disbursed: boolean;
  applicationStage: string;
  applicationStageLabel: string;
};

export type AuthResponse = {
  token: string;
  tokenType: string;
  user: User;
};

export class ApiError extends Error {
  code: string;
  status: number;

  constructor(code: string, message: string, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
const TOKEN_KEY = "ezfinanz_token";

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function storeToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
  });

  const data: unknown = await response.json().catch(() => ({}));
  if (!response.ok) {
    const payload = data as { code?: string; message?: string };
    throw new ApiError(
      payload.code ?? "ERROR",
      payload.message ?? "Request failed",
      response.status,
    );
  }
  return data as T;
}

export const authApi = {
  signupEmail: (body: { email: string; password: string; fullName?: string }) =>
    api<{ message: string }>("/api/auth/signup/email", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  loginEmail: (body: { email: string; password: string }) =>
    api<AuthResponse>("/api/auth/login/email", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  loginGoogle: (idToken: string) =>
    api<AuthResponse>("/api/auth/login/google", {
      method: "POST",
      body: JSON.stringify({ idToken }),
    }),
  resendEmailOtp: (email: string) =>
    api<{ message: string }>("/api/auth/otp/email/resend", {
      method: "POST",
      body: JSON.stringify({ email }),
    }),
  verifyEmailOtp: (email: string, otp: string) =>
    api<AuthResponse>("/api/auth/otp/email/verify", {
      method: "POST",
      body: JSON.stringify({ email, otp }),
    }),
  sendPhoneOtp: (phone: string) =>
    api<{ message: string }>("/api/auth/otp/phone/send", {
      method: "POST",
      body: JSON.stringify({ phone }),
    }),
  verifyPhoneOtp: (phone: string, otp: string) =>
    api<AuthResponse>("/api/auth/otp/phone/verify", {
      method: "POST",
      body: JSON.stringify({ phone, otp }),
    }),
  me: () => api<User>("/api/auth/me"),
  updateProfile: (fullName: string) =>
    api<User>("/api/auth/profile", {
      method: "PATCH",
      body: JSON.stringify({ fullName }),
    }),
  sendEmailVerification: (email?: string) =>
    api<{ message: string }>("/api/auth/verification/email/send", {
      method: "POST",
      body: JSON.stringify({ email: email || undefined }),
    }),
  confirmEmailVerification: (otp: string) =>
    api<User>("/api/auth/verification/email/confirm", {
      method: "POST",
      body: JSON.stringify({ otp }),
    }),
  sendPhoneVerification: (phone?: string) =>
    api<{ message: string }>("/api/auth/verification/phone/send", {
      method: "POST",
      body: JSON.stringify({ phone: phone || undefined }),
    }),
  confirmPhoneVerification: (otp: string) =>
    api<User>("/api/auth/verification/phone/confirm", {
      method: "POST",
      body: JSON.stringify({ otp }),
    }),
};

export type Gender = "MALE" | "FEMALE" | "OTHER" | "PREFER_NOT_TO_SAY";
export type IdType = "PAN" | "AADHAAR" | "PASSPORT" | "DRIVING_LICENSE" | "VOTER_ID";

export type KycProfile = {
  fullName: string;
  dateOfBirth: string;
  age: number;
  gender: Gender;
  addressLine: string;
  city: string;
  state: string;
  pincode: string;
  idType: IdType;
  idNumber: string;
  hasDocument: boolean;
  documentFileName: string | null;
  submittedAt: string;
};

export async function apiForm<T>(path: string, form: FormData, method = "POST"): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const response = await fetch(`${API_URL}${path}`, { method, body: form, headers });
  const data: unknown = await response.json().catch(() => ({}));
  if (!response.ok) {
    const payload = data as { code?: string; message?: string };
    throw new ApiError(payload.code ?? "ERROR", payload.message ?? "Request failed", response.status);
  }
  return data as T;
}

export type IncomeType = "MONTHLY" | "ANNUAL";
export type EligibilityResult = "ELIGIBLE" | "PARTIALLY_ELIGIBLE" | "NOT_ELIGIBLE";
export type CreditBand = "EXCELLENT" | "GOOD" | "FAIR" | "POOR";

export type EligibilityAssessment = {
  incomeType: IncomeType;
  incomeAmount: number;
  requestedLoanAmount: number;
  creditScore: number;
  outstandingDebts: number;
  employerName: string;
  designation: string;
  monthlyIncome: number;
  annualIncome: number;
  dtiRatio: number;
  dtiPercent: number;
  creditBand: CreditBand;
  maxEligibleAmount: number;
  result: EligibilityResult;
  reasons: string[];
  assessedAt: string;
};

export const eligibilityApi = {
  get: () => api<EligibilityAssessment>("/api/eligibility"),
  assess: (body: {
    incomeType: IncomeType;
    incomeAmount: number;
    requestedLoanAmount: number;
    creditScore: number;
    outstandingDebts: number;
    employerName: string;
    designation: string;
  }) =>
    api<EligibilityAssessment>("/api/eligibility", {
      method: "POST",
      body: JSON.stringify(body),
    }),
};

export type EmiQuote = {
  minAmount: number;
  maxAmount: number;
  tenures: number[];
  creditBand: CreditBand;
  principal: number;
  tenureMonths: number;
  annualInterestPercent: number;
  processingFee: number;
  gstOnProcessingFee: number;
  documentationFee: number;
  stampDuty: number;
  otherCharges: number;
  totalCharges: number;
  monthlyEmi: number;
  totalInterest: number;
  totalRepayment: number;
  netDisbursement: number;
  irrPercent: number;
  selectedAt: string | null;
};

export const emiApi = {
  quote: (amount?: number, tenureMonths?: number) => {
    const params = new URLSearchParams();
    if (amount != null) {
      params.set("amount", String(amount));
    }
    if (tenureMonths != null) {
      params.set("tenureMonths", String(tenureMonths));
    }
    const query = params.toString();
    return api<EmiQuote>(`/api/emi/quote${query ? `?${query}` : ""}`);
  },
  get: () => api<EmiQuote>("/api/emi"),
  save: (principal: number, tenureMonths: number) =>
    api<EmiQuote>("/api/emi", {
      method: "POST",
      body: JSON.stringify({ principal, tenureMonths }),
    }),
};

export type BankAccount = {
  accountHolderName: string;
  accountNumber: string;
  accountNumberMasked: string;
  ifscCode: string;
  bankName: string;
  updatedAt: string;
};

export const bankApi = {
  get: () => api<BankAccount>("/api/bank"),
  save: (body: {
    accountHolderName: string;
    accountNumber: string;
    ifscCode: string;
    bankName: string;
  }) =>
    api<BankAccount>("/api/bank", {
      method: "POST",
      body: JSON.stringify(body),
    }),
};

export type DeclarationStatus = {
  accepted: boolean;
  termsVersion: string;
  acceptedAt: string | null;
};

export const declarationApi = {
  get: () => api<DeclarationStatus>("/api/declaration"),
  accept: () =>
    api<DeclarationStatus>("/api/declaration", {
      method: "POST",
      body: JSON.stringify({ accepted: true }),
    }),
};

export const kycApi = {
  get: () => api<KycProfile>("/api/kyc"),
  save: (form: FormData) => apiForm<KycProfile>("/api/kyc", form, "POST"),
  async documentBlob(): Promise<Blob> {
    const token = getStoredToken();
    const headers = new Headers();
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
    const response = await fetch(`${API_URL}/api/kyc/document`, { headers });
    if (!response.ok) {
      throw new ApiError("ERROR", "Could not open the ID document.", response.status);
    }
    return response.blob();
  },
};

export type SelfieStatus = {
  submitted: boolean;
  reviewStatus: "PENDING" | "APPROVED" | "REJECTED";
  rejectionReason: string | null;
  submittedAt: string;
  disbursed: boolean;
  disbursedAt: string | null;
  applicationStage: string;
  applicationStageLabel: string;
};

export const selfieApi = {
  get: () => api<SelfieStatus>("/api/selfie"),
  submit: (photo: File) => {
    const form = new FormData();
    form.append("photo", photo);
    return apiForm<SelfieStatus>("/api/selfie", form, "POST");
  },
  photoBlob: () => fetchBlob("/api/selfie/photo"),
};

export type DashboardNotice = {
  id: string;
  title: string;
  target: string;
};

export type CustomerDashboardData = {
  applicationId: string;
  lastUpdated: string;
  statusBadge: string;
  requestedAmount: number | null;
  tenureMonths: number | null;
  monthlyEmi: number | null;
  creditScore: number | null;
  eligibility: EligibilityAssessment | null;
  emi: EmiQuote | null;
  hasKycDocument: boolean;
  kycDocumentName: string | null;
  hasSelfie: boolean;
  notices: DashboardNotice[];
};

export const customerApi = {
  dashboard: () => api<CustomerDashboardData>("/api/customer/dashboard"),
};

export type SupportTicket = {
  id: number;
  subject: string;
  message: string;
  createdAt: string;
};

export const supportApi = {
  list: () => api<SupportTicket[]>("/api/support"),
  send: (subject: string, message: string) =>
    api<SupportTicket>("/api/support", {
      method: "POST",
      body: JSON.stringify({ subject, message }),
    }),
};

export type AdminApplicationSummary = {
  userId: number;
  applicantName: string | null;
  email: string | null;
  phone: string | null;
  requestedLoanAmount: number | null;
  selectedLoanAmount: number | null;
  tenureMonths: number | null;
  currentStage: string;
  currentStageLabel: string;
  createdAt: string;
  submittedAt: string;
};

export type AdminApplicationDetail = {
  userId: number;
  applicantName: string | null;
  email: string | null;
  phone: string | null;
  emailVerified: boolean;
  phoneVerified: boolean;
  fullyVerified: boolean;
  currentStage: string;
  currentStageLabel: string;
  kyc: KycProfile | null;
  eligibility: EligibilityAssessment | null;
  emi: EmiQuote | null;
  bankAccount: BankAccount | null;
  declaration: DeclarationStatus | null;
  selfie: SelfieStatus | null;
};

export const adminApi = {
  list: () => api<AdminApplicationSummary[]>("/api/admin/applications"),
  get: (userId: number) => api<AdminApplicationDetail>(`/api/admin/applications/${userId}`),
  approveSelfie: (userId: number) =>
    api<AdminApplicationDetail>(`/api/admin/applications/${userId}/selfie/approve`, { method: "POST" }),
  rejectSelfie: (userId: number, reason?: string) =>
    api<AdminApplicationDetail>(`/api/admin/applications/${userId}/selfie/reject`, {
      method: "POST",
      body: JSON.stringify({ reason: reason || undefined }),
    }),
  disburse: (userId: number) =>
    api<AdminApplicationDetail>(`/api/admin/applications/${userId}/disburse`, { method: "POST" }),
  kycDocumentBlob: (userId: number) => fetchBlob(`/api/admin/applications/${userId}/kyc-document`),
  selfieBlob: (userId: number) => fetchBlob(`/api/admin/applications/${userId}/selfie`),
  listAdmins: () => api<AdminStaffPage>("/api/admin/admins"),
  createAdmin: (body: { email: string; password: string; fullName?: string }) =>
    api<AdminAccount>("/api/admin/admins", {
      method: "POST",
      body: JSON.stringify(body),
    }),
};

export type AdminAccount = {
  id: number;
  email: string;
  fullName: string | null;
  createdAt: string;
  superAdmin: boolean;
};

export type AdminStaffPage = {
  canCreateAdmins: boolean;
  admins: AdminAccount[];
};

async function fetchBlob(path: string): Promise<Blob> {
  const token = getStoredToken();
  const headers = new Headers();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const response = await fetch(`${API_URL}${path}`, { headers });
  if (!response.ok) {
    throw new ApiError("ERROR", "Could not load the file.", response.status);
  }
  return response.blob();
}

