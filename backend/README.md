# AI Math Assistant backend

Optional. The calculator, scanner/OCR, history and the offline tutor all work without it.

## Why a backend?
An AI provider key inside an APK can be extracted by anyone. This tiny server keeps
the key in an environment variable and only exposes one endpoint to the app.

## Run locally
```bash
cd backend
ANTHROPIC_API_KEY=your_key_here node server.js          # Node.js 18+
curl -X POST localhost:8787/v1/math-assistant -H 'content-type: application/json' \
     -d '{"question":"Explain 15% of 2400"}'
```
Optional env vars: `PORT` (default 8787), `AI_MODEL` (default `claude-haiku-5-5`).

## Point the app at it
Add to the project's `local.properties` (never committed), then rebuild:
```
ai.backend.url=http://10.0.2.2:8787          # Android emulator → your computer
# ai.backend.url=https://math.yourdomain.com # production (HTTPS required on devices)
```
Clear-text HTTP is allowed only for 10.0.2.2 / localhost (see `res/xml/network_security_config.xml`).

## Deploy
Any Node host works (Render, Railway, Fly.io, a VPS, Cloud Run). Put it behind HTTPS,
keep the built-in rate limit, and set the key as a secret in the host's dashboard.
