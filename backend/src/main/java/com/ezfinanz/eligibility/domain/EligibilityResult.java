package com.ezfinanz.eligibility.domain;

/** Overall eligibility decision relative to the requested loan amount. */
public enum EligibilityResult {
    ELIGIBLE,
    PARTIALLY_ELIGIBLE,
    NOT_ELIGIBLE
}
