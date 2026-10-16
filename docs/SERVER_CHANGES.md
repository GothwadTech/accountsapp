# Accounts backend changes — separate PR plan

No Accounts code or deployment is modified by this scaffold.

## Browser-auth prerequisite

Register standalone, sample and consumer public OAuth clients (no APK client secret), exact HTTPS and fallback scheme redirects, supported scopes. Keep existing web auth design intact. Add App Link association files at the relevant host repositories. DEV_MODE localhost relaxation is not a production Android callback strategy.

## Extensions before broker

1. Backwards-compatible `ecosystem_apps.first_party boolean NOT NULL DEFAULT false`; admin-controlled classification. First-party consent policy must not bypass login, account selection, PKCE, redirect or scope checks.
2. RFC 8693 token exchange restricted to allowed source/target clients, with validated source audience, no scope escalation, revocation checks, rate limits and auditing. Specify independent server proof of trusted app possession; first_party + public client_id alone is insufficient.
3. Short-lived, atomic one-time target-bound handoff redemption with challenge and replay protection; determine transaction semantics and exact OAuth error responses.
4. Signed public `GET /api/apps/trusted` registry (canonical Worker path `/apps/trusted`): package/certificate/client tuples, schema version, expiry, signing key rotation and rollback defense. No private key in SDK.
5. Native revocation API and device/session-family model for per-app and device-wide logout. Existing cookie-authenticated `/oauth/revoke` cannot simply be called with an OAuth bearer and assumed equivalent. Document whether outstanding one-hour access tokens remain valid or are actively revoked.
6. Inventory `/me` management capabilities; add scoped, reauthenticated native endpoints only where needed. Identity scopes alone must not authorize password/security updates.

Tests: existing web OAuth regression; public-client PKCE; redirect validation; untrusted exchange; audience/scope escalation; consent policy; replay/concurrency; rotation races; revoked family; registry signature expiry; denial vs upstream outage. Deploy schema → compatible Worker → mobile feature enablement. Do not enable broker before these contracts are implemented and reviewed.
