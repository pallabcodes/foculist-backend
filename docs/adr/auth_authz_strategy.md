# Authentication & Authorization Implementation Plan

Based on the Hexagonal Tiered Simulation strategy, this document outlines the specific features, Docker emulator limitations, rate-limiting mechanisms, and task breakdown for implementing a standalone Authentication & Authorization service.

## 1. Feature Breakdown & Scope
To provide a commercial, Google-grade Auth/Authz system, we must support:
- **Registration & Login**: Native email/password authentication.
- **Social SSO**: Google, GitHub, Apple OAuth2 bridging.
- **Session Management**: JWT Access Tokens (short-lived) and Refresh Tokens (long-lived, rotatable).
- **MFA (Multi-Factor Authentication)**: TOTP (Authenticator apps) and SMS.
- **Password Management**: Forgot password, reset flows via email.
- **Authorization (Authz)**: RBAC (Role-Based Access Control) injected into JWT claims, verified locally by the API Gateway.

## 2. Cloud Target vs. Local Emulators

Per our established 5-tier service adoption strategy, the target cloud managed service is **AWS Cognito**. Here is how we map it to our tiers:

### Tier 1 & 2 Application (The "Hybrid" Approach)
AWS Cognito is available in the AWS Free Tier (Tier 1), but lacks a direct Local Docker counterpart from AWS. Thus, we utilize a combination of natively free capabilities and Docker emulation:

- **Tier 2 Docker Emulator**: `localstack/localstack:latest`
  - *Usage*: Fast, fully offline emulation of basic User Pools and JWT issuance.
  - *Constraints*: Lacks advanced Risk-Based Auth and SMS MFA without the paid LocalStack Pro tier.
- **Tier 1 Rate-Limited Real AWS**: For features where we *must* test the Real AWS Cognito locally (to avoid buying LocalStack Pro), we use the actual AWS SDK locally but **strictly enforce rate limits** so we never breach the AWS Free Tier (50,000 MAU).

### Implementation of the Rate Guard (Tier 1 / Tier 5 Guarding)
We will build an `AwsRateLimiterInterceptor` (using Resilience4j or Guava) that wraps the `CognitoIdentityProviderClient`. 
When `@Profile("local")` is active, the rate limiter allows a maximum of 5 distinct AWS API mutations per day. If a developer exceeds this, the interceptor throws a `LocalRateLimitExceededException`. This physically prevents infinite script loops from incurring AWS charges or exhausting free tiers.

### Gateway Verification (Zero-Cost Authz)
To scale perfectly and cost $0 in AWS bills, the API Gateway will **never** call AWS Cognito to validate a token. It will fetch the public JWKS (JSON Web Key Set) from Cognito once, cache it in memory, and cryptographically verify all incoming JWTs. This gives us infinite local/prod validation with zero API calls.

## 3. Implementation Task List

This follows our Hexagonal architecture to isolate the core logic from the specific AWS Cognito SDK.

1.  **Define Domain Ports**
    -   Create `IdentityProviderPort.java` (methods: `register`, `authenticate`, `confirmMfa`, `refreshToken`).
2.  **Implement Tier 1 Emulator Config**
    -   Create `docker-compose-localstack.yml`.
    -   Write an initialization script to bootstrap a Cognito User Pool into LocalStack on startup.
3.  **Implement AWS Cognito Adapter**
    -   Implement `AwsCognitoIdentityProviderAdapter` using `software.amazon.awssdk:cognitoidentityprovider`.
4.  **Implement Strict Rate Limiter**
    -   Build `LocalDevRateLimiter` using Resilience4j to wrap the Adapter ONLY when the `local` Spring profile is active.
5.  **API Gateway JWT Configuration**
    -   Configure `spring-boot-starter-oauth2-resource-server` in our `gateway-bff` to point to the LocalStack/Cognito JWKS URI for offline token verification.
6.  **Frontend Integration**
    -   Update `next-boilarplate` to consume the agnostic `identity-service` endpoints (so the frontend never hits Cognito directly, protecting our Hexagonal boundary).
