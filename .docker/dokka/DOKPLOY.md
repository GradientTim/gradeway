# Dokka deployment on Dokploy

The Dokka site is served at `https://dokka.gradienttim.dev/<project>/` (for this repo:
`https://dokka.gradienttim.dev/gradeway/`). Each project is its own Dokploy application sharing
the same host, routed by path prefix.

## 1. Build settings (General tab, Build Type: Dockerfile)

- **Docker File:** `.docker/dokka/Dockerfile`
- **Docker Context Path:** `.` (repo root, the Dockerfile copies the whole repo)
- **Build-time Arguments** (Environment tab, called "Build Args" in some versions):
  `DOKKA_PATH=<project>`. Optional here, it defaults to `gradeway`.
- Redeploy so the new image gets built.

## 2. Domains tab

| Field          | Value                   |
|----------------|-------------------------|
| Host           | `dokka.gradienttim.dev` |
| Path           | `/gradeway`             |
| Internal Path  | `/`                     |
| Strip Path     | **off**                 |
| Container Port | `80`                    |
| HTTPS          | on (Let's Encrypt)      |

Strip Path must stay off: nginx inside the container expects requests as `/gradeway/...`.

For a future project, use the same host with its own path (e.g. `/otherproject`).

## 3. DNS

Add an A (or CNAME) record for `dokka.gradienttim.dev` pointing to the Dokploy server.

## 4. Old domain (`gradeway-dokka.gradienttim.dev`)

Its root no longer works since the files live under `/gradeway/`. Redirect it via the app's **Redirects** tab:

- **Regex:** `^https?://gradeway-dokka\.gradienttim\.dev/(.*)`
- **Replacement:** `https://dokka.gradienttim.dev/gradeway/${1}`
- **Permanent:** on

Remove the old domain once DNS and the certificate for the new one are in place.

## 5. Verify

- `https://dokka.gradienttim.dev/gradeway` redirects to `/gradeway/` and shows the docs.
- Module links (`/gradeway/core-api/...`) work.
- `https://dokka.gradienttim.dev/` returns 404 until something is deployed at the root.

## Reusing for another project

Copy `.docker/dokka/Dockerfile`, `nginx.conf` and `Dockerfile.dockerignore`, adjust the module
list in the dockerignore, and set `DOKKA_PATH=<project>`.
