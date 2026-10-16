# Security requirements

- System Custom Tabs + S256 PKCE, random state, exact callback matching, expiring single-use pending requests, process-death recovery. Prefer verified HTTPS App Links. No WebView credential handling.
- Standard platform/library crypto primitives only; never implement a cipher or verify/parse identity from JWT manually. Resolve identity with userinfo. Never put backend JWT signing or Supabase service keys in an APK.
- Keystore-backed encryption through supported Tink/platform integration; exclude credential data from backup. Specify key loss, corruption and reinstall behavior.
- Single-flight refresh per account; atomic rotated token persistence; logout generation guards so late refresh cannot resurrect sessions. Do not clear accounts for timeouts/5xx.
- Bearer interceptor is HTTPS + explicit host allowlist restricted; cross-origin redirects must not leak Authorization. Bounded retry and separate refresh transport to avoid recursion/deadlock.
- Verify Binder caller before metadata or handoff. Capture identity before dispatching asynchronous work. Resolve all UID packages and reject ambiguous mappings unless explicitly covered by policy.
- API 28+: SigningInfo/hasSigningCertificate. API 26–27: legacy GET_SIGNATURES. Define certificate rotation/multiple-signer behavior; reject unexpected signers. No signature permission dependency across separately signed apps.
- Signed trust-list refresh requires a pinned verification key, expiry/version rollback protection and defined offline behavior; expired/untrusted updates cannot expand access.
- FOSS policy disables broker discovery/provider access, not only the chooser. Unknown protocol/cert falls back gracefully without leaking account existence.
- Public client_id is not proof of a first-party installation. Broker server authentication must be resolved before exchange is enabled. No embedded shared secret workaround.
- Sign-out: server revocation is authoritative; IPC notification best effort. Ordinary broadcast extras do not prove sender identity. Local removal works offline; report remote revocation pending/failure honestly.
