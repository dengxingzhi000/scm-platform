package com.scmcloud.auth.webauthn;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * WebAuthn 楠岃瘉锟?
 * 浣跨敤 WebAuthn4J 搴撳疄锟絎3C WebAuthn 鏍囧噯楠岃瘉
 *
 * @author Deng
 * @since 2025-12-15
 */
@Component
@Slf4j
public class WebAuthnValidator {
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final AAGUID ZERO_AAGUID = new AAGUID(new byte[16]);

    private final WebAuthnConfig webAuthnConfig;
    private final WebAuthnManager webAuthnManager;
    private final ObjectConverter objectConverter;

    public WebAuthnValidator(WebAuthnConfig webAuthnConfig) {
        this.webAuthnConfig = Objects.requireNonNull(webAuthnConfig, "webAuthnConfig");
        this.objectConverter = new ObjectConverter();
        this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
    }

    /**
     * 楠岃瘉娉ㄥ唽鍝嶅簲
     *
     * @param clientDataJSON    瀹㈡埛绔暟锟絁SON (Base64URL)
     * @param attestationObject 璇佹槑瀵硅薄 (Base64URL)
     * @param expectedChallenge 棰勬湡鐨勬寫鎴橈拷
     * @return 楠岃瘉鍚庣殑鍑瘉鏁版嵁
     */
    public RegistrationResult validateRegistration(
            String clientDataJSON,
            String attestationObject,
            String expectedChallenge) {

        try {
            RegistrationData registrationData = parseAndValidateRegistration(
                    clientDataJSON,
                    attestationObject,
                    expectedChallenge
            );
            RegistrationResult result = buildRegistrationResult(registrationData);
            log.debug("WebAuthn registration validated successfully");
            return result;
        } catch (VerificationException e) {
            log.warn("WebAuthn registration validation failed: {}", e.getMessage());
            throw new IllegalStateException("WebAuthn 娉ㄥ唽楠岃瘉澶辫触", e);
        } catch (RuntimeException e) {
            log.error("WebAuthn registration processing error", e);
            throw new IllegalStateException("WebAuthn 娉ㄥ唽澶勭悊閿欒", e);
        }
    }

    /**
     * 楠岃瘉璁よ瘉鍝嶅簲
     *
     * @param credentialId      鍑瘉ID (Base64URL)
     * @param clientDataJSON    瀹㈡埛绔暟锟絁SON (Base64URL)
     * @param authenticatorData 璁よ瘉鍣ㄦ暟锟?Base64URL)
     * @param signature         绛惧悕 (Base64URL)
     * @param expectedChallenge 棰勬湡鐨勬寫鎴橈拷
     * @param storedPublicKey   瀛樺偍鐨勫叕锟?COSE 鏍煎紡)
     * @param storedSignCount   瀛樺偍鐨勭鍚嶈锟?
     * @return 鏂扮殑绛惧悕璁℃暟
     */
    public AuthenticationResult validateAuthentication(
            String credentialId,
            String clientDataJSON,
            String authenticatorData,
            String signature,
            String expectedChallenge,
            byte[] storedPublicKey,
            long storedSignCount) {

        try {
            AuthenticationData authenticationData = parseAndValidateAuthentication(
                    credentialId,
                    clientDataJSON,
                    authenticatorData,
                    signature,
                    expectedChallenge,
                    storedPublicKey,
                    storedSignCount
            );

            AuthenticatorData<?> authData = authenticationData.getAuthenticatorData();
            if (authData == null) {
                throw new IllegalStateException("AuthenticatorData is null in authentication response");
            }
            long newSignCount = authData.getSignCount();
            log.debug("WebAuthn authentication validated successfully, newSignCount={}", newSignCount);
            return new AuthenticationResult(newSignCount, true);
        } catch (VerificationException e) {
            log.warn("WebAuthn authentication validation failed: {}", e.getMessage());
            throw new IllegalStateException("WebAuthn 璁よ瘉楠岃瘉澶辫触", e);
        } catch (RuntimeException e) {
            log.error("WebAuthn authentication processing error", e);
            throw new IllegalStateException("WebAuthn 璁よ瘉澶勭悊閿欒", e);
        }
    }

    /**
     * 锟紺OSE 鍏挜搴忓垪鍖栦负瀛楄妭鏁扮粍
     *
     * @param coseKey COSE 鍏挜锛屼笉鑳戒负 null
     * @return 搴忓垪鍖栧悗鐨勫瓧鑺傛暟锟?
     */
    public byte[] serializeCOSEKey(COSEKey coseKey) {
        Objects.requireNonNull(coseKey, "coseKey must not be null");
        return objectConverter.getCborConverter().writeValueAsBytes(coseKey);
    }

    private RegistrationData parseAndValidateRegistration(
            String clientDataJSON,
            String attestationObject,
            String expectedChallenge) {
        byte[] clientDataJsonBytes = base64UrlDecode(clientDataJSON, "clientDataJSON");
        byte[] attestationObjectBytes = base64UrlDecode(attestationObject, "attestationObject");

        ServerProperty serverProperty = buildServerProperty(expectedChallenge);
        RegistrationRequest registrationRequest = new RegistrationRequest(attestationObjectBytes, clientDataJsonBytes);
        RegistrationParameters registrationParameters = new RegistrationParameters(
                serverProperty,
                null,
                webAuthnConfig.isUserVerificationRequired()
        );

        RegistrationData registrationData = webAuthnManager.parse(registrationRequest);
        webAuthnManager.verify(registrationData, registrationParameters);
        return registrationData;
    }

    private RegistrationResult buildRegistrationResult(RegistrationData registrationData) {
        AttestationObject attestationObject = registrationData.getAttestationObject();
        if (attestationObject == null) {
            throw new IllegalStateException("AttestationObject is null");
        }

        AuthenticatorData<?> authData = attestationObject.getAuthenticatorData();

        AttestedCredentialData credentialData = authData.getAttestedCredentialData();
        if (credentialData == null) {
            throw new IllegalStateException("No attested credential data found");
        }

        COSEKey coseKey = credentialData.getCOSEKey();

        return new RegistrationResult(
                credentialData.getCredentialId(),
                coseKey,
                authData.getSignCount(),
                credentialData.getAaguid()
        );
    }

    private AuthenticationData parseAndValidateAuthentication(
            String credentialId,
            String clientDataJSON,
            String authenticatorData,
            String signature,
            String expectedChallenge,
            byte[] storedPublicKey,
            long storedSignCount) {
        Objects.requireNonNull(storedPublicKey, "storedPublicKey");

        byte[] credentialIdBytes = base64UrlDecode(credentialId, "credentialId");
        byte[] clientDataJsonBytes = base64UrlDecode(clientDataJSON, "clientDataJSON");
        byte[] authenticatorDataBytes = base64UrlDecode(authenticatorData, "authenticatorData");
        byte[] signatureBytes = base64UrlDecode(signature, "signature");

        ServerProperty serverProperty = buildServerProperty(expectedChallenge);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                credentialIdBytes,
                null,
                authenticatorDataBytes,
                clientDataJsonBytes,
                null,
                signatureBytes
        );

        CredentialRecord credentialRecord = createCredentialRecord(credentialIdBytes, storedPublicKey, storedSignCount);

        AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                serverProperty,
                credentialRecord,
                Collections.singletonList(credentialIdBytes),
                webAuthnConfig.isUserVerificationRequired()
        );

        AuthenticationData authenticationData = webAuthnManager.parse(authenticationRequest);
        webAuthnManager.verify(authenticationData, authenticationParameters);
        return authenticationData;
    }

    private CredentialRecord createCredentialRecord(byte[] credentialIdBytes, byte[] storedPublicKey, long storedSignCount) {
        COSEKey coseKey = deserializeStoredPublicKey(storedPublicKey);
        AttestedCredentialData attestedCredentialData = new AttestedCredentialData(
                ZERO_AAGUID,
                credentialIdBytes,
                coseKey
        );

        return new CredentialRecordImpl(
                null,           // attestationStatement
                null,           // uvInitialized
                null,           // backupEligible
                null,           // backupState
                storedSignCount,
                attestedCredentialData,
                null,           // authenticatorExtensions
                null,           // clientData
                null,           // clientExtensions
                null            // transports
        );
    }

    private COSEKey deserializeStoredPublicKey(byte[] storedPublicKey) {
        COSEKey coseKey = objectConverter.getCborConverter().readValue(storedPublicKey, COSEKey.class);
        if (coseKey == null) {
            throw new IllegalStateException("Failed to deserialize stored public key");
        }
        return coseKey;
    }

    @SuppressWarnings("deprecation") // ServerProperty 鏋勯€犲櫒宸插簾寮冿紝浣嗘棤鏇夸唬 API
    private ServerProperty buildServerProperty(String expectedChallenge) {
        String rpOrigin = requireNonBlank(webAuthnConfig.getOrigin(), "webauthn.rp.origin");
        String rpId = requireNonBlank(webAuthnConfig.getId(), "webauthn.rp.id");

        Origin origin = new Origin(rpOrigin);
        Challenge challenge = new DefaultChallenge(base64UrlDecode(expectedChallenge, "expectedChallenge"));
        return new ServerProperty(origin, rpId, challenge, null);
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must not be blank");
        }
        return value;
    }

    private static byte[] base64UrlDecode(String base64Url, String fieldName) {
        if (base64Url == null || base64Url.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        try {
            return BASE64_URL_DECODER.decode(base64Url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " is not valid Base64URL", e);
        }
    }

    /**
     * 娉ㄥ唽缁撴灉
     */
    public record RegistrationResult(
            byte[] credentialId,
            COSEKey publicKey,
            long signCount,
            AAGUID aaguid
    ) {
        public RegistrationResult {
            credentialId = credentialId == null ? null : credentialId.clone();
        }

        public byte[] credentialId() {
            return credentialId == null ? null : credentialId.clone();
        }

        public String getCredentialIdBase64() {
            return credentialId == null ? null : BASE64_URL_ENCODER.encodeToString(credentialId);
        }

        /**
         * 鑾峰彇 AAGUID 浣滀负 java.util.UUID
         */
        public UUID getAaguid() {
            if (aaguid == null || aaguid.equals(new AAGUID(new byte[16]))) {
                return null;
            }
            return UUID.fromString(aaguid.toString());
        }
    }

    /**
     * 璁よ瘉缁撴灉
     */
    public record AuthenticationResult(
            long newSignCount,
            boolean success
    ) {}
}
