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

- **Multi-masjid.** A masjid registers itself together with its imam. There is no board or committee approval.
- **The imam manages the masjid.** Roles: `PLATFORM_ADMIN` (us), `IMAM` (manages their own masjid: approves requests, schedules, issues certificates, sets the fee), `COUPLE`.
- **First version scope:** nikah requests and scheduling; documents and checklist (license, IDs, wali/witnesses, mahr, counseling); certificate PDF and records; payments (fee/donation) and email/SMS reminders.
- Mobile app later, against the same REST API.

Schema: `backend/src/main/resources/db/migration/V1__core_schema.sql`.

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

Scaffold only: schema, security skeleton, health endpoint, Docker/CI/deploy. Registration, auth (JWT), requests and the Angular screens are next.
