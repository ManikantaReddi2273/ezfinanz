package com.ezfinanz.auth.domain;

/** Why an OTP was issued (sign-up, login, or verifying contact details). */
public enum OtpPurpose {
    SIGNUP,
    LOGIN,
    VERIFY_EMAIL,
    VERIFY_PHONE
}
