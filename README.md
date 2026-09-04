# 🔑 authserver | A Keycloak-Inspired OAuth2/OIDC Authorization Server, Built From Scratch

A multi-tenant OAuth2/OIDC authorization server built on top of Spring Authorization
Server. Not a wrapper around Keycloak, an exploration of what Keycloak actually *does*
under the hood, rebuilt piece by piece to understand the protocol, not just configure it.

This isn't a toy that only talks to itself. It's the identity provider behind
**[Keyra](https://github.com/vaalemax/keyra_authserver_implementation)**, a real password
manager, including a live authorization check on every request to a permission-gated
feature, and stateless-token invalidation that survives a user being disabled mid-session.

---

## 🎯 Why this project exists

Most portfolio auth projects stop at "I added Spring Security and a login form." This one
exists to answer a harder question: **what does it actually take to build the thing
Keycloak is**: realm isolation, per-tenant signing keys, an admin API, and an
authorization model that goes beyond a flat list of roles?

Spring Authorization Server gives you a spec-compliant OAuth2/OIDC implementation. It does
not give you multi-tenancy, an admin console, scope-gated custom claims, or a way to
invalidate a stateless JWT mid-session: those are architectural problems I have solved
myself. The features below are the result; the
[Interesting Problems Solved](#-interesting-problems-solved) section is the honest account
of how they got built, including the attempts that didn't work the first time.

---

## ✨ Key Features

- **Path-based multi-tenancy** — every realm gets its own issuer
  (`/{realm}/oauth2/authorize`, `/{realm}/oauth2/token`, `/{realm}/.well-known/openid-configuration`),
  the same pattern Keycloak uses, implemented on top of Spring Authorization Server's
  multi-issuer support rather than assumed for free.
- **Per-realm signing keys** — each realm generates and owns its own RSA keypair. A
  token issued by one realm cannot be validated against another realm's public key, even
  if both realms happen to share a username.
- **Realm isolation enforced at every boundary, not just assumed** — a session
  authenticated against one realm cannot silently reach another realm's protocol
  endpoints, whether that session came from a realm login or from the Admin Console's own
  OAuth2 login. This took more than one attempt to get right (see below).
- **Stateless tokens that can still be shut off** — JWTs are self-contained by design and
  can't be revoked in the traditional sense, but disabling a user immediately rejects
  their already-issued bearer tokens on every endpoint that checks authorization, not just
  their next login attempt.
- **Custom claims and refresh tokens, properly scope-gated** — the `roles` claim and
  refresh token issuance are both opt-in per OAuth2 request (`roles` and `offline_access`
  scopes respectively), not bolted onto every token regardless of what the client actually
  asked for.
- **A real Admin API** — full CRUD, not just create-and-forget, across realms, clients,
  users, roles, and permissions, authenticated via OAuth2 client credentials against a
  dedicated `master` realm.
- **A working Admin Console** — a Thymeleaf UI that is itself an OAuth2 client of the
  `master` realm, logging in through the exact same Authorization Code flow every other
  application on this server uses. Full management of realms, clients, users, roles,
  permissions, and role assignments: no curl required to run the system day to day.
- **A hybrid RBAC/ABAC authorization engine** — roles carry permissions, but permissions
  can declare `{{placeholder}}` conditions resolved per user-role assignment at request
  time. A `POST /{realm}/auth/can` endpoint answers "can this token's holder perform this
  action on this subject" with a live decision, including the resolved condition.
- **Proven with a real client, not just Postman** — Keyra's login, its optional
  two-factor vault unlock, and a permission-gated activity log are all wired against this
  server's real Authorization Code + PKCE flow and its `/auth/can` endpoint.

---

## 🏗️ Architecture

```
/{realm}/oauth2/authorize, /oauth2/token, /oauth2/jwks, /.well-known/openid-configuration
        → the OAuth2/OIDC protocol surface, one per realm

/admin/**
        → REST admin API, authenticated via client_credentials against the master realm

/console/**
        → Thymeleaf admin UI, authenticated via authorization_code against the master realm

/{realm}/auth/can
        → live ABAC/RBAC decision endpoint, authenticated via the caller's own realm token
```

Each realm is a genuine tenant boundary: its own users, its own clients, its own signing
key, its own roles and permissions. Two realms (say, `keyra` for a password manager and
a hypothetical `acme-crm` for something else entirely) can register a client with the
same `client_id`, assign a user the same username, and never collide: the realm is part
of the identity, not a filter applied on top of a shared one.

**Code organization follows the same boundary.** Each domain concept — `realm`, `client`,
`user`, `authorization` — is its own package with a hexagonal layering inside:

```
realm/
├── domain/          → Realm, RealmRepository (interface only)
├── application/      → RealmService — use cases, zero HTTP awareness
├── infrastructure/   → JpaRealmRepository, RealmExistenceFilter
└── presentation/      → RealmAdminController, ConsoleRealmController, DTOs, mappers
```

`application` services never throw `ResponseStatusException`: they raise plain domain
exceptions (`NoSuchElementException`, `IllegalArgumentException`, `IllegalStateException`),
and each `presentation` controller translates those into the right HTTP status or Console
flash message.

`security/` sits outside any single domain on purpose: it's infrastructure shared across
all of them (login handlers, JWT customizers, cross-realm guards), not logic that belongs
to one bounded context.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3, Spring Authorization Server |
| Security | Spring Security 6, Nimbus JOSE + JWT, RSA (RS256) |
| Persistence | Spring Data JPA (Hibernate), PostgreSQL |
| Schema migrations | Liquibase |
| Web layer | Spring MVC, Thymeleaf (login pages, Admin Console) |
| Config | spring-dotenv (`.env` loaded natively into the Spring environment) |
| Build | Maven |
| Infra | Docker Compose (PostgreSQL) |

---

## 🔒 Security Highlights

- **PKCE-required Authorization Code flow** as the default grant for confidential and
  public clients alike.
- **Per-realm RSA key isolation**: compromising or rotating one realm's signing key has
  no effect on any other realm.
- **Disabled users lose access within the same request, not the next login**: a custom
  `AuthenticationManagerResolver` rejects bearer tokens for disabled users on the
  authorization decision endpoint, and a dedicated filter does the same for the OIDC
  UserInfo endpoint, closing the gap that plain JWT statelessness would otherwise leave
  open for the life of the token.
- **Scope-gated claims and grants**: a client that doesn't ask for `roles` doesn't get
  role information in its tokens; one that doesn't ask for `offline_access` doesn't get a
  refresh token. Both required replacing Spring Authorization Server's default token
  generation, not just flipping a config flag.
- **Admin operations require a real OAuth2 access token**, scoped and short-lived
  (15 minutes), not a static credential checked with `equals()`.
- **Discovery documents don't leak tenant existence**: requesting the metadata for a
  realm that doesn't exist, or one that's been disabled, returns 404, not a fully-formed
  (if useless) discovery document.
- **Authorization decisions are computed live**, not cached in a token claim that could
  drift out of date the moment a permission assignment changes.

---

## 🧩 Interesting Problems Solved

The parts of this project most worth talking about weren't features — they were bugs that
turned out to be genuinely instructive once diagnosed properly.

**A `JwtDecoder` bean silently became the decoder for everything.** Adding a
realm-specific `JwtDecoder` for the Admin API's `master`-realm validation, as a normal
Spring `@Component`, was enough for Spring Authorization Server's own OIDC UserInfo
endpoint to pick it up and use it for *every* realm, because its internal wiring reuses
any `JwtDecoder` bean it finds in the context instead of building its own from the
configured key source, if one already exists. The fix wasn't a config flag; it was
building that decoder manually, inside the one security chain that should ever see it,
instead of letting it live as a globally discoverable bean.

**Realm isolation looked correct twice, and wasn't, for two different reasons.** First
attempt: an `AuthorizationManager`-based check comparing the authenticated principal's
realm against the request path worked perfectly for the very first, unauthenticated
request, and was silently never consulted again for any request afterward, because Spring
Authorization Server's internal protocol filters resolve an already-authenticated request
before `AuthorizationFilter` ever gets a turn. The fix reframed the problem: instead of
trying to *deny* a mismatched session after the fact, a filter now clears the security
context before the protocol filters see it, so a mismatched request arrives looking
exactly like any other unauthenticated one, and falls through the same, already-correct
"please log in" path. Second surprise, found later: that same filter only recognized
realm-login sessions — an Admin Console session, a completely different principal type
from a completely different login mechanism, sailed straight through unchecked, because it
was never the type the filter was written to look for. Both mechanisms now go through the
same guard.

**Disabling a user doesn't touch a JWT already in someone's pocket, by design, not by
oversight.** A signed, unexpired token stays verifiable purely from its signature, with or
without the database agreeing that its owner should still have access. Closing that gap
took two different techniques for two different chains: on the endpoints this project
fully controls (the `/auth/can` decision endpoint), a custom `AuthenticationManagerResolver`
rejects the token during authentication itself, before any business logic runs. On the
OIDC UserInfo endpoint, whose internals Spring Authorization Server owns, there's no
equivalent hook, so a filter lets authentication succeed and then inspects the result
against the current database state, clearing it if the user has since been disabled. Same
guarantee, two mechanisms, chosen by what each chain actually exposes.

**Issuing a refresh token only for clients that asked for one isn't supported out of the
box, and it's a known gap, not a misunderstanding.** Spring Authorization Server issues a
refresh token to any client configured for the grant type, regardless of which scopes were
actually requested; there's an open upstream issue asking for exactly this. Closing it
meant replacing the default token generator with a composed one: reusing the framework's
own JWT and access-token generators, but swapping in a refresh-token generator that returns
`null` unless `offline_access` was explicitly granted. The easy-to-miss part: doing this
manually also means manually re-wiring the custom claims customizer into the new JWT
generator, since it no longer gets attached automatically — a one-line omission that would
have silently dropped the `roles` claim from every token.

**The server, as a client of itself, deadlocked at startup.** Wiring the Admin Console as
an OAuth2 client of this server's own `master` realm — issuer-uri discovery included —
meant the application tried to call its own `/.well-known/openid-configuration` endpoint
*while still booting*, before it was listening for connections. The fix was building the
`ClientRegistration` from explicit endpoint URLs in code instead of relying on live
discovery, sidestepping the self-reference entirely.

---

## ⚠️ Known Limitations

- **No signing key rotation.** Each realm's RSA keypair is generated once, at creation,
  with no admin action (yet) to rotate it.
- **The login page isn't per-realm branded.** All realms share one physical `/login`
  page; the realm and the post-login redirect target are threaded through as request
  parameters rather than through distinct, brandable per-realm paths
  (`/realms/{realm}/login-actions/...` in real Keycloak). A deliberate simplification, not
  an oversight.
- **"Admin" in the `master` realm is coarse.** Any authenticated user in `master` is
  treated as an administrator: there's no further role check within that realm. Adequate
  for a single-operator setup, not for a team.
- **No brute-force protection on login.** Unlike Keyra (which has IP-based rate limiting),
  this server's login endpoint has none yet.
- **Stateless-token invalidation covers the endpoints this project owns, not every
  conceivable one.** `/auth/can` and `/userinfo` both check whether a user has been
  disabled since their token was issued; a hypothetical future resource-server endpoint
  that validates tokens independently would need the same treatment applied deliberately —
  it isn't automatic just because the mechanism exists elsewhere.
- **Admin Console is functional, not polished.** No CSS, no JavaScript, by design: it
  proves the OAuth2 wiring and the CRUD flows work, it doesn't try to look like a finished
  product.

---

## 🚀 Running Locally

### With Docker (recommended)

```bash
git clone https://github.com/vaalemax/authserver.git
cd authserver

docker compose up -d   # starts PostgreSQL

cp .env.example .env
# set ADMIN_CLIENT_SECRET, ADMIN_CONSOLE_USERNAME, ADMIN_CONSOLE_PASSWORD,
# ADMIN_CONSOLE_CLIENT_SECRET

mvn spring-boot:run
```

`.env` is loaded automatically into the Spring environment at startup, no manual export
step needed. The server will be available at `http://localhost:9000`. A `master` realm, an
`admin-cli` client (client_credentials), and an `admin-console` user/client are seeded
automatically on first startup.

### Trying it out

```bash
# Get an admin token
TOKEN=$(curl -s -X POST http://localhost:9000/master/oauth2/token \
  -u admin-cli:$ADMIN_CLIENT_SECRET \
  -d grant_type=client_credentials -d scope=admin | jq -r .access_token)

# Create a realm
curl -X POST http://localhost:9000/admin/realms -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"demo","displayName":"Demo"}'
```

Or skip curl entirely: log in to `http://localhost:9000/console/realms` with the seeded
admin console credentials, and manage realms, clients, users, roles, and permissions
directly from the browser.

**Requirements:** Docker and Docker Compose, JDK 21, Maven.

---

## 📸 Screenshots

<img width="655" height="375" alt="image" src="https://github.com/user-attachments/assets/83cb0c34-d5d3-4652-8925-c5d1a7887c1c" />

<img width="1116" height="520" alt="Screenshot 2026-08-22 014052" src="https://github.com/user-attachments/assets/a0906a40-f06d-449a-bce7-6eeeb7f2346f" />

---

## 📄 License

This project is available for portfolio/demonstration purposes.

---

Built by **Valerio Massimo Moretti** · Software Engineer
