# Deploying TicketFlow (Render)

One Render **Blueprint** (`render.yaml`) provisions everything from this repo:
a Dockerized web service, a managed PostgreSQL, and a managed Redis-compatible
Key Value store. No CLI required.

## 1. Push this repo to GitHub

Render deploys from a GitHub (or GitLab) repo it can see.

## 2. Create the Blueprint

1. [dashboard.render.com](https://dashboard.render.com) → **New** → **Blueprint**.
2. Connect the `TicketFlow` repo. Render finds `render.yaml` automatically.
3. Review the plan — it should show 3 resources: `ticketflow` (web),
   `ticketflow-db` (Postgres), `ticketflow-redis` (Key Value) — all on the free
   plan.
4. **Apply**.

Render builds the `Dockerfile` (a few minutes the first time), provisions the
database and Redis, and wires the environment variables `render.yaml` declares
(`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `REDIS_URL`,
`JWT_SECRET`) automatically — you don't type any of them in.

## 3. Watch it come up

The **Logs** tab on the `ticketflow` service should show, in order:

```
Migrating schema "public" to version "1 - initial schema"
Migrating schema "public" to version "2 - seed demo event"
Started TicketflowApplication in N seconds
```

Render polls `/actuator/health` (declared in `render.yaml`) to know the deploy
succeeded.

## 4. Try it

Render gives the web service a URL like `https://ticketflow-xxxx.onrender.com`.

```bash
curl https://ticketflow-xxxx.onrender.com/actuator/health
# {"status":"UP"}
```

Interactive API docs: `https://ticketflow-xxxx.onrender.com/swagger-ui.html`
— click **Authorize**, paste a JWT from `POST /auth/login`, and every protected
call in the UI sends it.

The seed migration runs automatically, so `GET /events` already has the demo
show with 500 seats.

## Free-tier realities (be upfront about these)

- **The web service sleeps after 15 minutes idle** and takes ~30–50s to wake on
  the next request. The first hit after a quiet period will look slow — that's
  the platform, not the app.
- **The free Postgres is deleted after 90 days** (Render emails a warning first).
  For a portfolio link you intend to keep alive, either recreate it periodically
  or upgrade that one resource to a paid instance plan (a few dollars a month) —
  the web service and Redis can stay free.
- `app.hold.duration` / `app.hold.sweep-interval-ms` and the JWT TTL are all
  still configurable via the same env vars documented in `application.yml` if
  you want to change them post-deploy (Render → service → **Environment**).

## Updating the deploy

`autoDeploy: true` in `render.yaml` means every push to the connected branch
redeploys automatically. To change an env var or resource plan, edit
`render.yaml` and push, or edit it directly in the Render dashboard (dashboard
edits and the file can drift — prefer editing the file).

## Rolling back

Render → `ticketflow` service → **Events** tab → pick an earlier successful
deploy → **Rollback**.
