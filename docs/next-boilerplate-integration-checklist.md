# Next-Boilerplate Integration Checklist

## 1) Frontend env setup
- Set `NEXT_PUBLIC_API_BASE_URL` to gateway URL (example: `http://localhost:8080`).
- Set `NEXT_PUBLIC_DEFAULT_TENANT` (example: `public`).

## 2) Route contract to use
- Preferred backend entrypoint is gateway (`gateway-bff`).
- Use these frontend routes against gateway:
  - `/api/auth/*`
  - `/api/user` (compatibility profile/create)
  - `/api/users/*`
  - `/api/projects/*`
  - `/api/planning/*`
  - `/api/calendar/*`
  - `/api/meetings/*`
  - `/api/sync/*`
  - `/api/resources/*`
  - `/bff/dashboard`

## 3) Local next-boilerplate API route collisions
- The frontend reference contains internal Next routes under `app/api/*` (for example `app/api/user/route.ts`).
- For backend integration, either:
  - remove/disable these mock routes, or
  - ensure frontend service code uses `NEXT_PUBLIC_API_BASE_URL` (axios instance) instead of relative `/api/*` fetches.

## 4) Tenant header
- Gateway enforces tenant context.
- Frontend must send `X-Tenant-ID` on each backend call (already supported in `lib/axiosInstance.ts`).

## 5) CORS
- Gateway CORS is enabled via env:
  - `CORS_ALLOWED_ORIGINS`
  - `CORS_ALLOWED_METHODS`
  - `CORS_ALLOWED_HEADERS`
  - `CORS_ALLOW_CREDENTIALS`
- Local example:
  - `CORS_ALLOWED_ORIGINS=http://localhost:3000`

## 6) Smoke test
- Start backend gateway + services.
- Verify from frontend/browser:
  - `GET {NEXT_PUBLIC_API_BASE_URL}/api/user`
  - `GET {NEXT_PUBLIC_API_BASE_URL}/api/planning/sprints`
  - `GET {NEXT_PUBLIC_API_BASE_URL}/bff/dashboard`
