# Deploying Masjidly to Contabo

Same flow as `ba`.

1. **Open a PR into `main`** (Checks workflow runs). Optional labels: `major`, `minor` (default), `patch`, `no-deploy`.
2. **Merge it.** Three things happen automatically:
   - *Build & deploy* (push) builds the backend and frontend images and pushes them to ghcr.io tagged with the commit SHA.
   - *Release* tags the merge commit with the next version (latest tag + 1 by label, e.g. `1.1.0`) and publishes a release `v1.1.0` from the PR title and description.
   - *Deploy Latest Tag* waits for both, then deploys that release (unless the PR has `no-deploy`, the build failed, or a newer release exists).
3. **Deploy by hand / roll back:** *Actions → Deploy Latest Tag → Run workflow* deploys the newest release. *Actions → Build & deploy → Run workflow* with *tag* = an older commit SHA rolls back (no rebuild).

`deploy.sh` backs up the database, pulls the images, restarts, checks `/api/public/health`, and starts the previous version again if the new one is not healthy.

## One-time setup

- Server: the Contabo VPS already has Docker and the `deploy` user from `ba`. Create `/opt/nikahgator` owned by `deploy` (or run `server-setup.sh` as root, which is safe to repeat and prints the SSH key for the secrets).
- Repo secrets: `CONTABO_HOST`, `CONTABO_USER` (`deploy`), `CONTABO_SSH_KEY`, optional `CONTABO_SSH_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- Repo labels: `major`, `minor`, `patch`, `no-deploy`.
- Domain: add the block from `Caddyfile.snippet` to the shared Caddyfile in `/opt/rahbar` and recreate Caddy. The frontend joins Caddy's Docker network (`CADDY_NETWORK=rahbar_default`).
- First deploy creates `/opt/nikahgator/.env` with random DB passwords and JWT secret.
- Optional: *Settings → Environments → production* to require approval before deploys.
