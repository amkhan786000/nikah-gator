# Nikah Gator

Manage nikah (Islamic marriage) requests, scheduling, documents and certificates for masjids in the United States.

Monorepo, same layout and Contabo deployment as the `ba` project.

| Folder | What |
|---|---|
| `backend/` | Java 21, Spring Boot 4, Spring Data JPA, Spring Security, Flyway, MySQL 8 |
| `frontend/` | Angular (standalone, SCSS), served by nginx in production |
| `deploy/` | Production compose file, `deploy.sh`, Caddy snippet, env example (Contabo) |
| `.github/workflows/` | `checks.yml` (PRs), `deploy.yml` (build images to ghcr.io, deploy to Contabo) |

## Product model

- **SaaS, multi-masjid.** The platform admin signs in first (created from `ADMIN_EMAIL` on an empty database) and creates each masjid together with its imam. There is no self-registration and no board approval.
- The imam gets an email with a temporary password, signs in with an emailed code (optionally "remember this device"), chooses a new password, and manages **their own masjid only**: registers nikahs, prints the certificate.
- Roles: `PLATFORM_ADMIN`, `IMAM`.
- UI: the same Bootstrap/Adminto theme and layout as Rahbar (`ba`), mobile friendly (tables become cards on phones).
- Planned next: documents and checklist, certificate PDF and numbering, payments, reminders.

Schema: `backend/src/main/resources/db/migration/`.

## Run locally

```bash
cp .env.example .env
docker compose up --build        # http://localhost:4200  (API on :8080, MySQL on :3308)
```

Or run the pieces directly (MySQL from compose on :3308):

```bash
docker compose up -d mysql
cd backend && ./mvnw spring-boot:run
cd frontend && npm start         # proxies /api to :8080
```

## Deploy (Contabo)

See `deploy/`. Images are built by GitHub Actions and pulled on the server into `/opt/nikahgator`; `deploy.sh` backs up the DB, restarts and rolls back on a failed health check (`/api/public/health`). Add the site's domain to the shared Caddy (`deploy/Caddyfile.snippet`). Required repo secrets: `CONTABO_HOST`, `CONTABO_USER`, `CONTABO_SSH_KEY` (optional `CONTABO_SSH_PORT`, `MAIL_*`).

## Status

Working: login with emailed code + remember device, forgot/reset/change password, admin creates masjid + imam, imam registers/edits nikahs and prints a certificate, Angular screens for all of it.
