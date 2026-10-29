package com.scmcloud.auth.webauthn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebAuthnValidatorTest {

    @Test
    void registrationResultDefensivelyCopiesCredentialId() {
        byte[] original = new byte[]{1, 2};
        WebAuthnValidator.RegistrationResult result =
                new WebAuthnValidator.RegistrationResult(original, null, 1L, null);

        original[0] = 9;
        assertEquals(1, result.credentialId()[0]);

        byte[] copy = result.credentialId();
        copy[0] = 8;
        assertEquals(1, result.credentialId()[0]);
    }

    @Test
    void getCredentialIdBase64UsesBase64UrlWithoutPadding() {
        WebAuthnValidator.RegistrationResult result =
                new WebAuthnValidator.RegistrationResult(new byte[]{1, 2}, null, 0L, null);

        assertEquals("AQI", result.getCredentialIdBase64());
    }

    @Test
    void registrationResultAcceptsNullCredentialId() {
        WebAuthnValidator.RegistrationResult result =
                new WebAuthnValidator.RegistrationResult(null, null, 7L, null);

        assertNull(result.credentialId());
        assertNull(result.getCredentialIdBase64());
        assertEquals(7L, result.signCount());
    }

    @Test
    void registrationResultCopiesEmptyCredentialId() {
        byte[] original = new byte[0];
        WebAuthnValidator.RegistrationResult result =
                new WebAuthnValidator.RegistrationResult(original, null, 0L, null);

        assertNotSame(original, result.credentialId());
        assertNotSame(result.credentialId(), result.credentialId());
        assertEquals("", result.getCredentialIdBase64());
    }
}

