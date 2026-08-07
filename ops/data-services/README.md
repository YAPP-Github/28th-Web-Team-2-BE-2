# Data services

This Compose project provides the PostgreSQL and Redis services used by the
EC2 runtime. The sample application keeps its H2 default; this configuration
does not wire the application to either service.

## EC2 bootstrap

The following manual steps target Amazon Linux 2023 on ARM64. Replace
`<EC2_HOST>` with the server address. Do not put the real host, private key, or
database password in this repository.

### Install Docker and Compose

Run as `ec2-user`:

```bash
sudo dnf install -y docker curl
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

compose_version=v5.4.0
sudo install -d -m 0755 /usr/local/lib/docker/cli-plugins
sudo curl -fL --retry 3 \
  "https://github.com/docker/compose/releases/download/${compose_version}/docker-compose-linux-aarch64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod 0755 /usr/local/lib/docker/cli-plugins/docker-compose
```

Log out and reconnect so the `docker` group is applied, then verify:

```bash
docker --version
docker compose version
```

### Place the Compose files

From a local checkout, copy the manifest and the secret-free example:

```bash
scp -i ~/.ssh/backend.pem \
  ops/data-services/compose.yaml \
  ec2-user@<EC2_HOST>:/tmp/data-services-compose.yaml
scp -i ~/.ssh/backend.pem \
  ops/data-services/.env.example \
  ec2-user@<EC2_HOST>:/tmp/data-services.env.example
```

On the server, keep the directory and secret file readable only by the
operator group:

```bash
ssh -i ~/.ssh/backend.pem ec2-user@<EC2_HOST>

sudo install -d -o root -g ec2-user -m 0750 /opt/data-services
sudo install -o root -g root -m 0644 \
  /tmp/data-services-compose.yaml /opt/data-services/compose.yaml
sudo install -o root -g ec2-user -m 0640 \
  /tmp/data-services.env.example /opt/data-services/.env
sudo rm -f /tmp/data-services-compose.yaml /tmp/data-services.env.example
sudoedit /opt/data-services/.env
```

Set `POSTGRES_PASSWORD` to a generated hex-only secret in `.env`. The
directory should remain `root:ec2-user` with mode `0750`, and `.env` should
remain `root:ec2-user` with mode `0640`.

### Start and verify

```bash
cd /opt/data-services
docker compose --env-file .env -f compose.yaml config --quiet
docker compose --env-file .env -f compose.yaml up -d --wait --wait-timeout 60
docker compose --env-file .env -f compose.yaml ps

set -a
. ./.env
set +a
docker compose --env-file .env -f compose.yaml exec -T \
  -e PGPASSWORD="$POSTGRES_PASSWORD" postgres \
  psql -v ON_ERROR_STOP=1 -h 127.0.0.1 \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc 'SELECT 1'
docker compose --env-file .env -f compose.yaml exec -T redis redis-cli ping
```

The Compose file binds both services to `127.0.0.1`; public access requires a
separate, explicitly reviewed network change.

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
