# Broker v1 — draft, disabled until backend review

Discovery uses an explicit known package list and Android package-visibility queries. Provider authorities are host-specific to avoid manifest conflicts. No QUERY_ALL_PACKAGES.

State machine:
`Unavailable → BrowserFallback`, or `Discover → VerifyCaller → Metadata → UserSelection → HandoffPending → Redeem → UserInfo → Persist → SignedIn`.

Any unknown version, trust failure, timeout or unsupported server capability falls back to browser without exposing tokens. Cancellation/replay/expiry terminate the transaction. Never silently select a sibling account without the intended user action.

Proposed handoff: target generates verifier/challenge and nonce; verified sibling requests a short-lived, single-use handoff code bound to target client, selected user, challenge and allowed scopes; target redeems with verifier and fetches userinfo before atomic persistence. Source refresh token never leaves its app; sibling should not receive target reusable credentials.

This is a proposed broker-specific wrapper around server token exchange, not a claim that RFC 8693 defines handoff codes. Challenge binding prevents intercepted-code redemption but does not independently authenticate a first-party app to the Worker. Server trust proof remains a release blocker.

Version envelope, concrete IPC methods/error codes, payload limits, deadlines and native server authentication must be finalized before coding. Use bounded asynchronous IPC/network handling, not blocking the provider/main thread. Test untrusted metadata access, conflicting UID mappings, replay, concurrent handoffs, package removal and both sample hosts.
