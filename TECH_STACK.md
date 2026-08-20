# EZFINANZ — Locked Tech Stack

This document **locks the technology choices** for the EZFINANZ personal loan application solution.

It maps to [Personal20SystemE29320220Challenge.md](./Personal20SystemE29320220Challenge.md). The challenge allows any stack; the product must deliver a complete, clear customer and admin flow. This stack is chosen to do that without extra operational complexity.

**Architecture:** React (TypeScript) + Spring Boot 3 **modular monolith** + PostgreSQL + JWT.

**Not in v1:** microservices, Kubernetes, Kafka, Eureka, multiple databases, MinIO/S3, Google OAuth (future).

---

## 1. Locked stack

| Layer | Choice | Why (locked) |
| --- | --- | --- |
| Frontend | React + TypeScript + Vite | Fast, typed, standard SPA for the loan wizard and admin dashboard |
| Routing / forms | React Router + React Hook Form | Multi-step customer journey with validation |
| Styling | Tailwind CSS | Responsive mobile and desktop without a heavy UI kit |
| Backend | Spring Boot 3 (single service) | Eligibility, EMI, IRR, and application status live in one process |
| Architecture style | Modular monolith | Domain modules (auth, KYC, loan, admin) without distributed services |
| Security | Spring Security + JWT | Same login page; role-based routing for `CUSTOMER` and `ADMIN` |
| Database | PostgreSQL | Users, applications, KYC, bank, stage audit |
| Files | Local file system | ID document photo and live selfie (configured upload directory) |
| Phone OTP | Twilio SMS | Real SMS OTP for phone signup and verification |
| Email OTP | SMTP | Real email OTP for signup and email verification |
| OAuth | Google OAuth — **future** | Not in v1; email/password and phone OTP cover login now |
| API docs | Springdoc OpenAPI | Reviewers can inspect and try APIs |

---

## 2. What this stack must support

From the challenge, the stack must make these flows real end to end:

**Customer**

1. Sign up / log in (email + password, phone + OTP). Google OAuth is planned for later.
2. Verify email and phone
3. KYC
4. Eligibility check
5. EMI tenure selection (live recalculation)
6. Bank account
7. Declaration
8. Live selfie / photo
9. Disbursement status

**Admin**

1. Same login, admin dashboard
2. List all applications (name, amount, tenure, stage, submitted at)
3. Full application detail
4. Approve or reject selfie (optional reject reason)
5. Confirm disbursement

**Technical notes from the brief**

- Responsive frontend
- Backend owns eligibility, loan math, and status
- Secure storage of personal and financial data
- Password hashing and role-based access
- Third-party checks may be simulated except where this document locks a real integration (Twilio SMS, SMTP email)
- Google OAuth is deferred; not required for v1 login
- EMI and IRR calculators with clear inputs, formulas, and results

---

## 3. Frontend (locked)

| Item | Choice |
| --- | --- |
| Runtime | Node.js (LTS) |
| App | React 18+ with TypeScript |
| Bundler | Vite |
| Routing | React Router |
| Forms | React Hook Form |
| Styling | Tailwind CSS |
| HTTP | Fetch or Axios, with JWT on protected calls |

**UI split**

- Shared login / signup
- Customer wizard (gated by application stage)
- Admin dashboard and application detail
- Responsive layouts for mobile and desktop

**Out of frontend scope**

- Eligibility and EMI/IRR math (backend only)
- Storing PII as the system of record (PostgreSQL)

---

## 4. Backend (locked)

| Item | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3 |
| API | REST |
| Persistence | Spring Data JPA + Hibernate |
| Security | Spring Security, BCrypt password hashing, JWT |
| Docs | Springdoc OpenAPI (Swagger UI) |
| Build | Maven |

### Modular monolith (not microservices)

One deployable Spring Boot application. Packages stay separate so the code is easy to grow later:

| Module | Responsibility |
| --- | --- |
| `auth` | Email/password, phone OTP (Twilio), email OTP (SMTP), JWT, roles |
| `application` | One loan application per customer, current stage, timestamps |
| `kyc` | Identity and address, optional ID photo |
| `eligibility` | Income, amount, CIBIL, debts, employer; Eligible / Partially Eligible / Not Eligible |
| `loan` | EMI tenure, interest, fees, GST, net disbursement, IRR |
| `bank` | Disbursement account (holder, number, IFSC, bank name) |
| `declaration` | Terms accepted, timestamp |
| `selfie` | Live photo upload, waiting-for-admin status |
| `admin` | List, detail, selfie approve/reject, disbursement confirm |
| `files` | Store and serve ID photo and selfie on the **local file system** |
| `notify` | Twilio SMS OTP and SMTP email OTP |

All modules share **one PostgreSQL database** and **one application status field**. That keeps the customer journey consistent.

---

## 5. Security and identity (locked)

| Concern | Choice |
| --- | --- |
| Login page | One page for customers and admins |
| Post-login routing | JWT claims / role → customer wizard or admin dashboard |
| Roles | `CUSTOMER`, `ADMIN` |
| Passwords | BCrypt (never store plain text) |
| API auth | Bearer JWT |
| Email verification | OTP sent over **SMTP** |
| Phone verification | OTP sent over **Twilio SMS** |
| OAuth | Google OAuth 2.0 — **future** (not in v1) |

Customers must verify **both email and phone** before KYC and later steps.

---

## 6. Data and files (locked)

| Concern | Choice |
| --- | --- |
| Primary database | PostgreSQL |
| Core records | Users, roles, applications, KYC, eligibility, loan terms, bank accounts, declarations, selfies, stage history |
| File storage | **Local file system** only (configured upload directory on the server) |
| Secrets | Environment variables / `.env` (Twilio, SMTP, JWT, DB). Never commit secrets |

---

## 7. Simulations vs real integrations

| Capability | Locked approach |
| --- | --- |
| Phone OTP | **Twilio** SMS (real) |
| Email OTP | **SMTP** (real) |
| Google login | **Future** — not built in v1 |
| File storage | **Local file system** |
| KYC / PAN / Aadhaar checks | **Simulated** |
| CIBIL / credit score | Customer-entered or sample data; eligibility rules run in backend |
| Bank account validation | **Simulated** (format checks only) |

The application must stay fully usable without external KYC or credit-bureau vendors. Login and verification in v1 depend on Twilio and SMTP being configured.

---

## 8. Loan calculation (backend)

Eligibility and repayment math run in Spring Boot, with unit tests.

**Eligibility (example rules to implement)**

- Credit score bands (for example 750+ Excellent)
- Debt-to-income ratio
- Income vs requested amount
- Result: Eligible, Partially Eligible, or Not Eligible

**EMI / IRR (recalculate when amount or tenure changes)**

Inputs: loan amount, tenure (6 / 12 / 18 / 24 / 36 months), interest rate, processing fee, GST, other charges.

Outputs: monthly EMI, total interest, total repayment, total charges, net disbursement, IRR.

---

## 9. Explicitly not locked (do not use for v1)

Do **not** introduce these unless the product requirements change:

- Microservices / API gateway / service discovery
- Kafka or other event buses
- Kubernetes
- Redis (unless OTP TTL becomes a real need later)
- A second database
- MinIO / S3 object storage (files stay on the local file system)
- Google OAuth (planned for a later phase)
- A heavy frontend UI kit as the primary styling system

---

## 10. Suggested repo layout

```
ezfinanz/
├── Personal20SystemE29320220Challenge.md
├── TECH_STACK.md
├── frontend/          # React + TypeScript + Vite
└── backend/           # Spring Boot 3 modular monolith
```

---

*This file is the source of truth for EZFINANZ technology choices. Change the product first; then update this document.*
