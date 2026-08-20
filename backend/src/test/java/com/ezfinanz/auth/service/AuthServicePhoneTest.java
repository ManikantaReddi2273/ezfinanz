package com.ezfinanz.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthServicePhoneTest {

    @Test
    void tenDigitIndianNumberGetsCountryCode() {
        assertEquals("+919876543210", AuthService.normalizePhone("9876543210"));
    }

    @Test
    void alreadyInternationalNumberIsKept() {
        assertEquals("+919876543210", AuthService.normalizePhone("+919876543210"));
    }
}
