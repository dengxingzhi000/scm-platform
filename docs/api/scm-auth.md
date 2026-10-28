# scm-auth — OpenAPI 文档摘要

- 来源: `http://192.168.0.125:8106/api/public/v3/api-docs`
- 标题: SCM Platform API - auth-service (1.0.0)
- 认证: Bearer JWT (Authorization header)

## WebAuthnCredential

### PUT /api/auth/webauthn/credentials/{credentialId}/name
- 说明: 修改凭证设备名
- 描述: 为指定凭证设置便于识别的设备名称。
- 参数: `credentialId`(path,必填: string) `deviceName`(query,必填: string)
- 响应: `ApiResponseWebauthnCredentialDTO`

### PUT /api/auth/webauthn/credentials/{credentialId}/deactivate
- 说明: 停用凭证
- 描述: 停用指定凭证，停用后将无法用于登录认证。
- 参数: `credentialId`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/auth/webauthn/credentials
- 说明: 查询凭证列表
- 描述: 获取当前用户所有处于启用状态的 WebAuthn 凭证。
- 响应: `ApiResponseListWebauthnCredentialDTO`

### GET /api/auth/webauthn/credentials/health
- 说明: 检查凭证健康状态
- 描述: 检测当前用户凭证的使用状态并返回需要处理的异常凭证。
- 响应: `ApiResponseListWebauthnCredentialDTO`

### DELETE /api/auth/webauthn/credentials/{credentialId}
- 说明: 删除凭证
- 描述: 删除当前用户名下指定的 WebAuthn 凭证。
- 参数: `credentialId`(path,必填: string)
- 响应: `ApiResponseVoid`

## OAuth2Logout

### POST /oauth2/logout
- 说明: OAuth2 登出
- 描述: 撤销当前访问令牌：指定 clientId 时仅撤销该客户端授权，否则执行全局登出。
- 参数: `Authorization`(header,必填: string) `clientId`(query: string)
- 响应: `ApiResponseVoid`

## WebAuthnRegistration

### POST /api/auth/webauthn/register/verify
- 说明: 校验注册并保存凭证
- 描述: 校验 WebAuthn 注册结果并保存新的通行密钥凭证。
- 请求体: `WebauthnRegistrationRequest`
- 响应: `ApiResponseWebauthnCredentialDTO`

### POST /api/auth/webauthn/register/challenge
- 说明: 生成注册挑战
- 描述: 为当前用户生成 WebAuthn 凭证注册所需的随机挑战值。
- 响应: `ApiResponseWebAuthnRegisterChallengeResponse`

## WebAuthnAuthentication

### POST /api/auth/webauthn/authenticate/verify
- 说明: 校验认证并升级令牌
- 描述: 校验 WebAuthn 断言结果，通过后升级会话令牌并记录本次认证尝试。
- 请求体: `WebauthnAuthenticationRequest`
- 响应: `ApiResponseTokenUpgradeResponse`

### POST /api/auth/webauthn/authenticate/challenge
- 说明: 生成认证挑战
- 描述: 为当前用户生成 WebAuthn 断言认证所需的随机挑战值。
- 响应: `ApiResponseWebAuthnChallengeResponse`

## Auth

### POST /api/auth/refresh
- 说明: 刷新令牌
- 描述: 使用刷新令牌换取新的访问令牌，并同步更新设备与会话信息。
- 请求体: `RefreshTokenRequest`
- 响应: `ApiResponseLoginResponse`

### POST /api/auth/logout
- 说明: 用户登出
- 描述: 注销当前登录令牌并清理相关会话信息。
- 响应: `ApiResponseVoid`

### POST /api/auth/login
- 说明: 用户登录
- 描述: 校验用户名与密码，签发访问令牌与刷新令牌。
- 请求体: `LoginRequest`
- 响应: `ApiResponseLoginResponse`

### POST /api/auth/force-logout/{userId}
- 说明: 强制下线用户
- 描述: 管理员强制指定用户下线并记录下线原因。
- 参数: `userId`(path,必填: string) `reason`(query,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/auth/userinfo
- 说明: 获取当前用户信息
- 描述: 根据当前登录身份返回用户基础信息及其权限数据。
- 响应: `ApiResponseUserInfo`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseWebauthnCredentialDTO** — `code`: integer, `message`: string, `data`: WebauthnCredentialDTO, `timestamp`: integer
- **WebauthnCredentialDTO** — `credentialId`: string, `deviceName`: string, `algorithm`: string, `aaguid`: string, `transports`: string, `authenticatorAttachment`: string, `isActive`: boolean, `backupState`: boolean, `lastUsedAt`: string, `createdTime`: string
- **WebauthnRegistrationRequest** — `credentialId`: string, `clientDataJSON`: string, `attestationObject`: string, `deviceId`: string, `publicKeyPem`: string, `algorithm`: string, `deviceName`: string, `aaguid`: string, `transports`: string, `authenticatorAttachment`: string, `userVerification`: string, `backupState`: boolean, `backupEligible`: boolean
- **ApiResponseWebAuthnRegisterChallengeResponse** — `code`: integer, `message`: string, `data`: WebAuthnRegisterChallengeResponse, `timestamp`: integer
- **WebAuthnRegisterChallengeResponse** — `challenge`: string, `rpId`: string, `timeout`: integer, `user`: object, `attestation`: string
- **WebauthnAuthenticationRequest** — `credentialId`: string, `clientDataJSON`: string, `authenticatorData`: string, `signature`: string, `userHandle`: string, `signCount`: integer
- **ApiResponseTokenUpgradeResponse** — `code`: integer, `message`: string, `data`: TokenUpgradeResponse, `timestamp`: integer
- **TokenUpgradeResponse** — `accessToken`: string, `tokenType`: string, `expiresIn`: integer
- **ApiResponseWebAuthnChallengeResponse** — `code`: integer, `message`: string, `data`: WebAuthnChallengeResponse, `timestamp`: integer
- **WebAuthnChallengeResponse** — `challenge`: string, `rpId`: string, `timeout`: integer, `allowCredentials`: object[], `userVerification`: string
- **RefreshTokenRequest** — `refreshToken`: string, `deviceId`: string
- **ApiResponseLoginResponse** — `code`: integer, `message`: string, `data`: LoginResponse, `timestamp`: integer
- **LoginResponse** — `accessToken`: string, `refreshToken`: string, `tokenType`: string, `expiresIn`: integer, `userId`: string, `username`: string, `realName`: string, `roles`: string[], `permissions`: string[], `needChangePassword`: boolean, `message`: string
- **LoginRequest** — `username`: string, `password`: string, `twoFactorCode`: string, `deviceId`: string
- **ApiResponseListWebauthnCredentialDTO** — `code`: integer, `message`: string, `data`: WebauthnCredentialDTO[], `timestamp`: integer
- **ApiResponseUserInfo** — `code`: integer, `message`: string, `data`: UserInfo, `timestamp`: integer
- **PermissionDTO** — `id`: string, `parentId`: string, `permissionCode`: string, `permissionName`: string, `permissionType`: integer, `routePath`: string, `component`: string, `redirect`: string, `icon`: string, `apiPath`: string, `httpMethod`: string, `permissionScope`: string, `permissionLevel`: integer, `riskLevel`: integer, `needApproval`: boolean, `needTwoFactor`: boolean, `sortOrder`: integer, `visible`: boolean, `status`: integer, `createTime`: string, `updateTime`: string, `children`: PermissionDTO[]
- **UserInfo** — `userId`: string, `username`: string, `realName`: string, `avatar`: string, `email`: string, `phone`: string, `deptId`: string, `deptName`: string, `userLevel`: integer, `roles`: string[], `permissions`: string[], `menuTree`: PermissionDTO[]