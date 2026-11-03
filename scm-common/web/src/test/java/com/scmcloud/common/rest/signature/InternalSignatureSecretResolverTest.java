package com.scmcloud.common.rest.signature;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The signer and the verifier of the service-to-service channel must resolve the same secret.
 * These tests pin the resolution order and the "blank candidate is skipped" rule.
 */
class InternalSignatureSecretResolverTest {

    @Test
    void platformPropertyShouldWin() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("security.signature.app-secrets.internal-service", "from-property");
        environment.setProperty("API_SECRET_INTERNAL_SERVICE", "from-environment");

        assertEquals("from-property", resolver(environment).resolveSecret());
    }

    @Test
    void environmentVariableShouldBeUsedWhenThePropertyIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("API_SECRET_INTERNAL_SERVICE", "from-environment");

        assertEquals("from-environment", resolver(environment).resolveSecret());
    }

    @Test
    void blankCandidatesShouldBeSkipped() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("security.signature.app-secrets.internal-service", "");
        environment.setProperty("API_SECRET_INTERNAL_SERVICE", "from-environment");

        assertEquals("from-environment", resolver(environment).resolveSecret());
    }

    @Test
    void legacyVariableShouldStillBeAccepted() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("API_SECRET_INTERNAL_SECRET", "legacy");

        assertEquals("legacy", resolver(environment).resolveSecret());
    }

    @Test
    void missingSecretShouldResolveToAnEmptyString() {
        assertEquals("", resolver(new MockEnvironment()).resolveSecret());
    }

    @Test
    void appIdShouldNeverBeBlank() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("API_APP_ID", "");

        assertEquals(InternalSignatureSecretResolver.DEFAULT_APP_ID, resolver(environment).resolveAppId());
    }

    private InternalSignatureSecretResolver resolver(MockEnvironment environment) {
        return new InternalSignatureSecretResolver(environment);
    }
}
