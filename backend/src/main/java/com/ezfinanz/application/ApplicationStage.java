package com.ezfinanz.application;

/**
 * Ordered stages of the customer loan application journey, from contact verification through disbursement.
 */
public enum ApplicationStage {
    CONTACTS_PENDING("Contacts pending"),
    KYC("KYC"),
    ELIGIBILITY("Eligibility"),
    NOT_ELIGIBLE("Not eligible"),
    EMI("EMI terms"),
    BANK("Bank account"),
    DECLARATION("Declaration"),
    LIVE_SELFIE("Live selfie"),
    READY_TO_SUBMIT("Ready to submit"),
    WAITING_FOR_ADMIN_REVIEW("Waiting for Admin Review"),
    SELFIE_REJECTED("Selfie rejected"),
    READY_FOR_DISBURSEMENT("Ready for disbursement"),
    DISBURSED("Disbursed");

    private final String label;

    ApplicationStage(String label) {
        this.label = label;
    }

    /** Human-readable label for UI and notifications. */
    public String getLabel() {
        return label;
    }
}
