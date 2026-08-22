# 🔑 authserver | A Keycloak-Inspired OAuth2/OIDC Authorization Server, Built From Scratch

A multi-tenant OAuth2/OIDC authorization server built on top of Spring Authorization
Server. Not a wrapper around Keycloak, an exploration of what Keycloak actually *does*
under the hood, rebuilt piece by piece to understand the protocol, not just configure it.

This isn't a toy that only talks to itself. It's the identity provider behind
**[Keyra]([#](https://github.com/vaalemax/keyra_authserver_implementation))**, a real password manager, including a live authorization check on every request to a permission-gated feature.

---

## 🎯 Why this project exists

Most portfolio auth projects stop at "I added Spring Security and a login form." This one
exists to answer a harder question: **what does it actually take to build the thing
Keycloak is**: realm isolation, per-tenant signing keys, an admin API, and an
authorization model that goes beyond a flat list of roles?

Spring Authorization Server gives you a spec-compliant OAuth2/OIDC implementation. It does
not give you multi-tenancy, an admin console, or fine-grained authorization: those are
architectural problems I have solved myself. The features below are the result; the
[Interesting Problems Solved](#-interesting-problems-solved) section is the honest account
of how they got built.

---

## ✨ Key Features

- **Path-based multi-tenancy** — every realm gets its own issuer
  (`/{realm}/oauth2/authorize`, `/{realm}/oauth2/token`, `/{realm}/.well-known/openid-configuration`),
  the same pattern Keycloak uses, implemented on top of Spring Authorization Server's
  multi-issuer support rather than assumed for free.
- **Per-realm signing keys** — each realm generates and owns its own RSA keypair. A
  token issued by one realm cannot be validated against another realm's public key, even
  if both realms happen to share a username.
- **Realm isolation enforced, not just assumed** — a session authenticated against one
  realm cannot silently reach another realm's protocol endpoints. This took more than one
  attempt to get right (see below).
- **A real Admin API** — REST endpoints to manage realms, clients, users, roles, and
  permissions, authenticated via OAuth2 client credentials against a dedicated `master`
  realm — not a hardcoded username and password.
- **A working Admin Console** — a Thymeleaf UI that is itself an OAuth2 client of the
  `master` realm, logging in through the exact same Authorization Code flow every other
  application on this server uses. The admin tool eats its own dog food.
- **A hybrid RBAC/ABAC authorization engine** — roles carry permissions, but permissions
  can declare `{{placeholder}}` conditions resolved per user-role assignment at request
  time. A `POST /{realm}/auth/can` endpoint answers "can this token's holder perform this
  action on this subject" with a live decision, including the resolved condition — not
  just a boolean.
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
| Build | Maven |
| Infra | Docker Compose (PostgreSQL) |

---

## 🔒 Security Highlights

- **PKCE-required Authorization Code flow** as the default grant for confidential and
  public clients alike.
- **Per-realm RSA key isolation**: compromising or rotating one realm's signing key has
  no effect on any other realm.
- **Admin operations require a real OAuth2 access token**, scoped and short-lived
  (15 minutes), not a static credential checked with `equals()`.
- **Discovery documents don't leak tenant existence**: requesting the metadata for a
  realm that doesn't exist returns 404, not a fully-formed (if useless) discovery document.
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

**Realm isolation looked correct and wasn't.** An `AuthorizationManager`-based check
(`.access(...)`) that compared the authenticated principal's realm against the request
path worked perfectly for the very first, unauthenticated request, and was silently never
consulted again for any request afterward, because Spring Authorization Server's internal
protocol filters resolve an already-authenticated request before `AuthorizationFilter` (the
component that actually runs `authorizeHttpRequests` rules) ever gets a turn. The fix
reframed the problem: instead of trying to *deny* a mismatched session after the fact,
a filter now clears the security context *before* the protocol filters see it, so a
realm-mismatched request arrives looking exactly like any other unauthenticated one, and
falls through the same, already-correct "please log in" path.

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
- **Admin API is create/list only** on most resources: no update or delete yet on
  clients, users, roles, or permissions.
- **No brute-force protection on login.** Unlike Keyra (which has IP-based rate limiting),
  this server's login endpoint has none yet.
- **Admin Console is functional, not polished.** No CSS, no JavaScript, by design: it
  proves the OAuth2 wiring works, it doesn't try to look like a finished product.

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

The server will be available at `http://localhost:9000`. A `master` realm, an
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
admin console credentials.

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
