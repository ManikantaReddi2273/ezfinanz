package com.ezfinanz.declaration.dto;

import jakarta.validation.constraints.AssertTrue;

public class DeclarationRequest {

    @AssertTrue(message = "You must accept the declaration to continue")
    private boolean accepted;

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
