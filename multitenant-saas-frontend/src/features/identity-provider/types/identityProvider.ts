export type IdentityProviderProtocol = 'OIDC'
export type TenantIdentityProviderStatus = 'DRAFT' | 'VERIFIED' | 'DISABLED'
export type TenantSsoMode = 'OPTIONAL' | 'REQUIRED'

export interface TenantIdentityProvider {
    id: string
    tenantId: string
    protocol: IdentityProviderProtocol
    displayName: string
    issuerUri: string
    clientId: string
    scopes: string[]
    status: TenantIdentityProviderStatus
    ssoMode: TenantSsoMode
    clientSecretHint: string
    secretVersion: number
    verifiedAt: string | null
    disabledAt: string | null
    secretRotatedAt: string
    createdAt: string
    updatedAt: string
}

export interface TenantIdentityProviderCreateInput {
    displayName: string
    protocol: IdentityProviderProtocol
    issuerUri: string
    clientId: string
    clientSecret: string
    scopes: string[]
}

export interface TenantIdentityProviderUpdateInput {
    displayName: string
    issuerUri: string
    clientId: string
    scopes: string[]
}

export interface TenantIdentityProviderVerification {
    id: string
    status: TenantIdentityProviderStatus
    issuerUri: string
    authorizationEndpoint: string
    tokenEndpoint: string
    jwkSetUri: string
    verifiedAt: string
}

export interface TenantIdentityProviderSecretRotated {
    id: string
    clientSecretHint: string
    secretVersion: number
    status: TenantIdentityProviderStatus
    secretRotatedAt: string
}
