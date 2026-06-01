# Nginx API Gateway for Metroid Wiki

This folder contains the Nginx configuration for the Metroid Wiki API Gateway.

## Gateway behavior

- `/articulos/*` → forwarded to the Artículos service on `http://localhost:3001/articulos/*`
- `/auth/*` → forwarded to the Usuarios/Auth service on `http://localhost:3002/auth/*`
- `/multimedia/*` → forwarded to the Multimedia service on `http://localhost:3003/*`
- `/health` → gateway health check

> The current codebase exposes `articulos` and `usuarios` services as HTTP endpoints. The `multimedia` route is configured as a placeholder for a future service on port `3003`.

## How to use

### 1. Start services with Docker Compose

From `api/v1` run:

```bash
cd api/v1
docker compose up --build
```

This starts:
- `articulos` service on port `3001`
- `usuarios` service on port `3002`
- `nginx` gateway on port `80`

### 2. Legacy local start

If you still want to start a single service manually:

```bash
cd api/v1/servicios/articulos
npm install
npm start
```

```bash
cd api/v1/servicios/usuarios
npm install
npm start
```

### 3. Nginx direct start

If you only want to run the gateway locally without Docker:

```bash
nginx -c "$(pwd)/api/v1/nginx/nginx.conf"
```

Or using Docker directly:

```bash
docker run --rm -p 80:80 \
  -v "$(pwd)/api/v1/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
  -v "$(pwd)/api/v1/nginx/conf.d:/etc/nginx/conf.d:ro" \
  nginx:latest
```

## Gateway endpoints

- `http://localhost/health`
- `http://localhost/articulos`
- `http://localhost/articulos/{id}`
- `http://localhost/auth/login`
- `http://localhost/auth/registro`
- `http://localhost/multimedia/...` (placeholder)

## Notes

- The gateway is intentionally separate from the existing `app.js` Express proxy.
- Use Nginx when you want a real API gateway layer in front of the services.
- If your Multimedia service is not built yet, the `/multimedia` route will return connection errors until it exists.
