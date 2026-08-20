# EZFINANZ

## Personal Loan Application Solution

### Project Overview

Your task is to design and build a complete personal loan solution. The system should allow new customers to apply for a loan online and let admins review and approve applications. The goal is to create a simple, clear, and user-friendly flow from start to finish.

## What the Solution Should Include

- Customer can sign up and log in using Email, Phone, or OAuth (Google / similar)
- Email and Phone number verification
- KYC details collection
- Loan eligibility check
- EMI term selection
- Add bank account
- Confirmation of declaration
- Live selfie or photo verification (final step)
- Admin can see the full application flow, approve the selfie.

> **Note:** You are free to choose any tech stack. Focus on making the flow complete, clear, and easy to use.

## Customer Workflow

This is the complete journey a customer follows when applying for a personal loan.

### 1. Sign-Up and Login

Both customers and admins use the same login page. After login, the system sends them to the correct dashboard based on their role.

Customers can sign up or log in using any of these options:

- Email and Password (with email verification)
- Phone Number (with OTP verification)
- OAuth (for example Google login)

### 2. Email and Phone Verification

After signing up, the customer must verify both email and phone number.

- System sends an OTP or verification link to the email
- System sends an OTP to the phone number
- Both must be verified before the customer can move to later steps

### 3. KYC (Know Your Customer)

The customer fills in basic identity and address details.

**Details to collect:**

- Full Name, Age or Date of Birth, Gender
- Current Address
- ID Type and ID Number (for example PAN or Aadhaar – can be simulated)
- Optional: Upload a photo of the ID document

### 4. Loan Eligibility

The customer enters financial details. The system checks if they are eligible for the loan.

**Details to collect:**

- Monthly or Annual Income
- Requested Loan Amount
- CIBIL / Credit Score
- Current Debts or Outstanding Balances
- Employer Name and Designation

**How eligibility can be decided (example):**

- Check credit score range (for example 750+ is Excellent)
- Calculate Debt-to-Income ratio
- Compare income with requested loan amount
- Show clear result: Eligible, Partially Eligible, or Not Eligible

### 5. EMI Term Selection

If the customer is eligible, they choose how long they want to repay the loan.

- **Customer selects loan amount and tenure:** Customer can enter or select the required loan amount and choose a repayment tenure (for example, 6, 12, 18, 24, or 36 months). The system should display the applicable interest rate, processing fee, GST, other applicable charges, and the final loan amount considered for calculation.
- **System calculates and displays complete loan terms:** Based on the selected loan amount, interest rate, tenure, processing fee, GST, and other charges, the system should calculate and display the approximate monthly EMI, total interest, total repayment amount, total charges, net disbursement amount, and applicable IRR. The customer should be able to change the loan amount or tenure and immediately see the updated calculation.

### 6. Add Bank Account

The customer adds the bank account where the loan amount will be sent.

- Account Holder Name
- Account Number
- IFSC Code
- Bank Name

### 7. Confirmation of Declaration

Before the final step, the customer must read and accept a declaration.

- Show clear terms (information is true, consent for checks, etc.)
- Customer must tick a checkbox and confirm to continue

### 8. Live Selfie / Photo Verification

This is the last identity check before the loan is given.

- Customer takes a live selfie using the camera (or uploads a photo)
- Photo is saved with the application
- Status becomes **“Waiting for Admin Review”**

## Admin Workflow

Admins can see every application and take the final decisions.

### 1. Admin Dashboard

Admins can view a list of all submitted applications. Each row should show at least:

- Applicant Name
- Loan Amount Requested
- Tenure
- Current Stage (KYC, Eligibility, Selfie Pending, etc.)
- Submission Date and Time

### 2. View Full Application Flow

When an admin opens any application, they should be able to see the complete journey of that customer:

- Login / Verification status
- KYC details
- Eligibility result and scores
- Selected EMI tenure
- Bank account details
- Declaration confirmation
- Selfie / Photo submitted by the customer

### 3. Confirm Live Selfie / Photo

Admin must review the selfie or photo and take action.

- Approve the photo
- Reject the photo (with optional reason)

## End-to-End Flow Summary

### Customer Side

1. Sign up or log in (Email / Phone / OAuth)
2. Verify Email and Phone
3. Complete KYC
4. Submit loan details and check eligibility
5. Select EMI tenure
6. Add bank account
7. Confirm declaration
8. Submit live selfie / photo
9. See final disbursement status

### Admin Side

1. Log in and open the dashboard
2. View all applications and their current stage
3. Open any application and see the full flow
4. Review and approve or reject the selfie / photo
5. Confirm disbursement when everything is ready

## Technical Notes

- Make the frontend responsive so it works well on mobile and desktop.
- Use a proper backend to handle eligibility logic, loan calculations, and application status.
- Store personal and financial data securely.
- Follow basic security practices such as password hashing and role-based access.
- You can simulate OTP, email, SMS, KYC verification, credit-score checks, bank details, and other third-party services if real services are not available.
- You can populate your own sample/test data for KYC details, income, credit score, loan amount, tenure, interest rate, bank details, and repayment schedules.
- Implement EMI and IRR calculators with clear input fields, formulas, and result displays.
- Allow testing of the loan flow with multiple sample loan scenarios.
- The application should remain fully functional even when external integrations are simulated or mocked.

---

*End of Document*
