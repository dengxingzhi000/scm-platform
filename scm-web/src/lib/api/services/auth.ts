import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  TokenUpgradeResponse,
  UserInfo,
  WebAuthnAuthenticationRequest,
  WebAuthnChallengeResponse,
  WebAuthnCredentialDTO,
  WebAuthnRegisterChallengeResponse,
  WebAuthnRegistrationRequest,
} from "../contracts/auth";

const AUTH_BASE = "/auth/api/auth";

export const authService = {
  login: (data: LoginRequest) =>
    unwrap<LoginResponse>(apiClient.post(`${AUTH_BASE}/login`, data)),

  logout: () => unwrap<void>(apiClient.post(`${AUTH_BASE}/logout`)),

  refresh: (data: RefreshTokenRequest) =>
    unwrap<LoginResponse>(apiClient.post(`${AUTH_BASE}/refresh`, data)),

  getUserInfo: () => unwrap<UserInfo>(apiClient.get(`${AUTH_BASE}/userinfo`)),

  forceLogout: (userId: string, reason: string) =>
    unwrap<void>(
      apiClient.post(`${AUTH_BASE}/force-logout/${userId}`, null, {
        params: { reason },
      })
    ),

  oauth2Logout: (clientId?: string) =>
    unwrap<void>(
      apiClient.post(
        "/auth/oauth2/logout",
        null,
        clientId ? { params: { clientId } } : undefined
      )
    ),

  webauthn: {
    authenticationChallenge: () =>
      unwrap<WebAuthnChallengeResponse>(
        apiClient.post(`${AUTH_BASE}/webauthn/authenticate/challenge`)
      ),

    authenticate: (data: WebAuthnAuthenticationRequest) =>
      unwrap<TokenUpgradeResponse>(
        apiClient.post(`${AUTH_BASE}/webauthn/authenticate/verify`, data)
      ),

    registrationChallenge: () =>
      unwrap<WebAuthnRegisterChallengeResponse>(
        apiClient.post(`${AUTH_BASE}/webauthn/register/challenge`)
      ),

    register: (data: WebAuthnRegistrationRequest) =>
      unwrap<WebAuthnCredentialDTO>(
        apiClient.post(`${AUTH_BASE}/webauthn/register/verify`, data)
      ),

    listCredentials: () =>
      unwrap<WebAuthnCredentialDTO[]>(
        apiClient.get(`${AUTH_BASE}/webauthn/credentials`)
      ),

    updateDeviceName: (credentialId: string, deviceName: string) =>
      unwrap<WebAuthnCredentialDTO>(
        apiClient.put(
          `${AUTH_BASE}/webauthn/credentials/${credentialId}/name`,
          null,
          { params: { deviceName } }
        )
      ),

    deleteCredential: (credentialId: string) =>
      unwrap<void>(
        apiClient.delete(`${AUTH_BASE}/webauthn/credentials/${credentialId}`)
      ),

    deactivateCredential: (credentialId: string) =>
      unwrap<void>(
        apiClient.put(
          `${AUTH_BASE}/webauthn/credentials/${credentialId}/deactivate`
        )
      ),

    credentialHealth: () =>
      unwrap<WebAuthnCredentialDTO[]>(
        apiClient.get(`${AUTH_BASE}/webauthn/credentials/health`)
      ),
  },
};
