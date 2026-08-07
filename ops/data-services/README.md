# Data services

This Compose project provides the PostgreSQL and Redis services used by the
EC2 runtime. The sample application keeps its H2 default; this configuration
does not wire the application to either service.

## Start

```bash
cp .env.example .env
# Set POSTGRES_PASSWORD to a locally generated secret.
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

`POSTGRES_PASSWORD` is an initialization value for the official PostgreSQL
image. With the `postgres_data` volume present, changing `.env` alone does not
change the existing database password. Never delete the volume as a password
rotation shortcut.

## Rotate the PostgreSQL password

Run this from the directory containing `compose.yaml`. The example uses the
default `app` role and a hex-only password so the SQL command remains safely
quoted.

```bash
new_password="$(openssl rand -hex 32)"

docker compose --env-file .env exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U app -d app \
  -c "ALTER ROLE app PASSWORD '${new_password}'"

docker compose --env-file .env exec -T -e PGPASSWORD="${new_password}" postgres \
  psql -v ON_ERROR_STOP=1 -h 127.0.0.1 -U app -d app -c 'SELECT 1'
```

After the smoke test succeeds, update `POSTGRES_PASSWORD` in `.env`. Keep the
named volume and do not print the secret in logs or commit it.
