# Educue Deployment Guide

**Last verified:** 10 August 2026

> **Production runbook for Educue**
>
> Target stack:
>
> - Ubuntu 24.04 LTS on a GCP Compute Engine VM
> - PostgreSQL 17 installed natively on the VM
> - OpenJDK 21
> - Spring Boot backend managed by `systemd`
> - React/Vite portal built in GitHub Actions and served by Nginx
> - Next.js university/home site built as a static export in GitHub Actions and served by Nginx
> - Nginx reverse proxy + Let's Encrypt TLS
> - Cloudflare DNS/proxy with **Full (strict)** TLS
> - GitHub Actions CI/CD using `feature` for development and `main` for production
> - Versioned releases with atomic `current` symlinks and rollback
>
> Production hostnames:
>
> - `https://portal.ebadynamics.co.ke` — React/Vite portal
> - `https://portal.ebadynamics.co.ke/api/*` — Spring Boot API
> - `https://home.ebadynamics.co.ke` — Next.js static university site
>
> This guide deliberately keeps Docker **off the production VM**. Docker is used only by GitHub Actions for the temporary PostgreSQL CI service container.

---

## Table of Contents

1. [Architecture](#1-architecture)
2. [Before You Start](#2-before-you-start)
3. [GCP, DNS and Firewall Requirements](#3-gcp-dns-and-firewall-requirements)
4. [Base Ubuntu Setup](#4-base-ubuntu-setup)
5. [Install PostgreSQL 17](#5-install-postgresql-17)
6. [Install OpenJDK 21](#6-install-openjdk-21)
7. [Create Runtime Users and Folder Layout](#7-create-runtime-users-and-folder-layout)
8. [Configure the Backend Environment](#8-configure-the-backend-environment)
9. [Install and Bootstrap Nginx](#9-install-and-bootstrap-nginx)
10. [Issue Let's Encrypt Certificates](#10-issue-lets-encrypt-certificates)
11. [Install the Final HTTPS Nginx Configuration](#11-install-the-final-https-nginx-configuration)
12. [Create the Spring Boot systemd Service](#12-create-the-spring-boot-systemd-service)
13. [Spring Boot Health Endpoint Requirements](#13-spring-boot-health-endpoint-requirements)
14. [Create the GitHub Deployment User and SSH Key](#14-create-the-github-deployment-user-and-ssh-key)
15. [Install the Backend Deployment Script](#15-install-the-backend-deployment-script)
16. [Install the React Portal Deployment Script](#16-install-the-react-portal-deployment-script)
17. [Install the Next Home Deployment Script](#17-install-the-next-home-deployment-script)
18. [Configure sudoers for Deployment](#18-configure-sudoers-for-deployment)
19. [Git Branch Model](#19-git-branch-model)
20. [GitHub Environment and Secret Model](#20-github-environment-and-secret-model)
21. [Backend GitHub Actions Setup](#21-backend-github-actions-setup)
22. [React Portal GitHub Actions Setup](#22-react-portal-github-actions-setup)
23. [Next.js Home Site GitHub Actions Setup](#23-nextjs-home-site-github-actions-setup)
24. [First Deployment Order](#24-first-deployment-order)
25. [Verification Checklist](#25-verification-checklist)
26. [Rollback and Release Management](#26-rollback-and-release-management)
27. [Semantic Versioning and Git Tags](#27-semantic-versioning-and-git-tags)
28. [Operational Commands](#28-operational-commands)
29. [Troubleshooting](#29-troubleshooting)
30. [Security Notes](#30-security-notes)
31. [Official Documentation](#31-official-documentation)

---

# 1. Architecture

```text
                         Internet
                            |
                            v
                       Cloudflare
                            |
                         HTTPS
                            |
                            v
                         Nginx
              +-------------+--------------+
              |                            |
              |                            |
 portal.ebadynamics.co.ke       home.ebadynamics.co.ke
              |                            |
       +------+-------+               Next static files
       |              |              /var/www/educue-home
       |              |
 React static       /api/*
 files               |
 /var/www/           v
 educue-portal   Spring Boot
                 127.0.0.1:8080
                       |
                       v
                 PostgreSQL 17
                 127.0.0.1:5432
```

GitHub Actions acts as the build/deployment control plane:

```text
feature branch
     |
     v
CI build/test
     |
     v
Pull Request
     |
     v
main branch
     |
     v
CI build/test
     |
     v
production artifact
     |
     v
SSH deploy user
     |
     v
versioned release directory
     |
     v
atomic current symlink
```

The production VM does **not** need:

- Maven
- Node.js
- npm
- Docker
- Git repository checkouts

The VM only needs:

- Java
- PostgreSQL
- Nginx
- `systemd`
- SSH
- standard Unix tools (`tar`, `curl`, `sha256sum`, `flock`)

---

# 2. Before You Start

Assumptions:

```text
OS: Ubuntu 24.04 LTS
Backend: Java 21 + Spring Boot + Maven wrapper
Database: PostgreSQL 17
Portal: React + Vite
Home: Next.js static export
DNS provider: Cloudflare
CI/CD: GitHub Actions
```

Reserve a **static external IP** for the GCP VM.

Production hostnames:

```text
portal.ebadynamics.co.ke
home.ebadynamics.co.ke
```

Do not put actual passwords or private keys into this guide, source control, GitHub issues, or build logs.

---

# 3. GCP, DNS and Firewall Requirements

## 3.1 Cloudflare DNS

Create:

| Type | Name | Value | During first TLS issue | After TLS works |
|---|---|---|---|---|
| A | `portal` | VM static IP | DNS only | Proxied |
| A | `home` | VM static IP | DNS only | Proxied |

Using **DNS only** during the first certificate issuance removes Cloudflare from the HTTP-01 validation path and simplifies troubleshooting.

## 3.2 GCP firewall

Public application ports:

```text
TCP 80
TCP 443
```

Do **not** expose:

```text
TCP 5432  PostgreSQL
TCP 8080  Spring Boot
```

### SSH and GitHub Actions

The workflows in this guide use GitHub-hosted runners that SSH directly to the VM static public IP.

Therefore, port `22` must be reachable by the GitHub-hosted runner.

For administrator SSH through GCP IAP, Google documents the IPv4 source range:

```text
35.235.240.0/20
```

If you later restrict SSH exclusively to this IAP range, the direct GitHub Actions SSH deployments in this guide will stop working unless deployment is moved to IAP, a self-hosted runner, or another private mechanism.

## 3.3 Optional UFW

GCP firewall rules can be the primary perimeter.

If you additionally enable UFW, keep an existing SSH session open and test a **second** session before closing the first.

For the current direct-GitHub-SSH design:

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing

sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

sudo ufw enable
sudo ufw status verbose
```

---

# 4. Base Ubuntu Setup

```bash
sudo apt update
sudo apt upgrade -y
```

Install baseline tools:

```bash
sudo apt install -y \
  curl \
  ca-certificates \
  gnupg \
  unzip \
  tar \
  openssl \
  nginx \
  snapd
```

Verify:

```bash
lsb_release -a
```

---

# 5. Install PostgreSQL 17

The PostgreSQL project provides an official APT repository for Ubuntu.

## 5.1 Add the PostgreSQL repository

```bash
sudo apt install -y postgresql-common
sudo /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh
```

Install:

```bash
sudo apt update
sudo apt install -y postgresql-17 postgresql-client-17
```

Enable/start:

```bash
sudo systemctl enable postgresql
sudo systemctl start postgresql
```

Verify:

```bash
psql --version
sudo systemctl status postgresql --no-pager
```

## 5.2 Create the application role and database

```bash
sudo -u postgres psql
```

Inside `psql`:

```sql
CREATE ROLE educue_app LOGIN;
\password educue_app
CREATE DATABASE educue OWNER educue_app;
\q
```

## 5.3 Bind PostgreSQL locally

Find the config file:

```bash
sudo -u postgres psql -tAc "SHOW config_file;"
```

Open that `postgresql.conf` and ensure:

```conf
listen_addresses = 'localhost'
```

Restart:

```bash
sudo systemctl restart postgresql
```

Verify:

```bash
sudo ss -lntp | grep 5432
```

Expected binding:

```text
127.0.0.1:5432
[::1]:5432
```

not the public interface.

## 5.4 Test the application login

```bash
psql \
  -h 127.0.0.1 \
  -U educue_app \
  -d educue
```

Then:

```sql
SELECT current_database(), current_user;
\q
```

## 5.5 Root-only database credential recovery file

```bash
sudo nano /root/educue-postgres-credentials.env
```

```env
DB_URL=jdbc:postgresql://127.0.0.1:5432/educue
DB_USERNAME=educue_app
DB_PASSWORD=REPLACE_WITH_REAL_DATABASE_PASSWORD
```

Protect:

```bash
sudo chown root:root /root/educue-postgres-credentials.env
sudo chmod 600 /root/educue-postgres-credentials.env
```

The runtime Spring environment is separate:

```text
/opt/educue/config/educue.env
```

## 5.6 One-time clean production database reset

Use this only for the planned final reset. It permanently removes the current
production data. The backend must be stopped first so no requests can write
during the reset.

```bash
sudo systemctl stop educue
sudo -u postgres psql
```

Inside `psql`, terminate the application's remaining connections, recreate the
empty database, and preserve the existing application role and password:

```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'educue'
  AND pid <> pg_backend_pid();

DROP DATABASE educue;
CREATE DATABASE educue OWNER educue_app;
\q
```

Deploy or start the backend once:

```bash
sudo systemctl start educue
sudo journalctl -u educue -f
```

The application connects to the empty `educue` database and Flyway applies the
seven final migrations from `classpath:db/production` in order. Hibernate uses
`ddl-auto: validate`; it validates the result but does not create the schema.
The application can create the schema inside an empty database, but it cannot
connect to or recreate a database that does not exist.

Verify the migration history:

```bash
sudo -u postgres psql -d educue -c \
  'SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;'
```

---

# 6. Install OpenJDK 21

Ubuntu 24.04 provides OpenJDK 21.

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk-headless
```

Verify:

```bash
java -version
javac -version
```

The service will use:

```text
/usr/bin/java
```

---

# 7. Create Runtime Users and Folder Layout

## 7.1 Spring runtime account

```bash
sudo groupadd --system educue 2>/dev/null || true

sudo useradd \
  --system \
  --gid educue \
  --home /opt/educue \
  --shell /usr/sbin/nologin \
  educue 2>/dev/null || true
```

## 7.2 Backend directories

```bash
sudo mkdir -p \
  /opt/educue/releases \
  /opt/educue/config \
  /opt/educue/storage \
  /opt/educue/logs \
  /opt/educue/scripts
```

Permissions:

```bash
sudo chown root:educue /opt/educue
sudo chmod 755 /opt/educue

sudo chown educue:educue /opt/educue/releases
sudo chown educue:educue /opt/educue/storage
sudo chown educue:educue /opt/educue/logs

sudo chown root:educue /opt/educue/config
sudo chmod 750 /opt/educue/config
```

Layout:

```text
/opt/educue/
├── releases/
│   └── <git-sha>/
│       └── educue.jar
├── current -> releases/<git-sha>
├── config/
│   └── educue.env
├── storage/
├── logs/
└── scripts/
```

Do **not** create backend `current` yet. The first successful backend deployment creates it.

## 7.3 Frontend directories and bootstrap releases

```bash
sudo mkdir -p \
  /var/www/educue-portal/releases/bootstrap \
  /var/www/educue-home/releases/bootstrap
```

Portal placeholder:

```bash
sudo tee /var/www/educue-portal/releases/bootstrap/index.html >/dev/null <<'EOF'
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>Educue Portal</title>
</head>
<body>
  <h1>Educue Portal</h1>
  <p>Production deployment is being prepared.</p>
</body>
</html>
EOF
```

Home placeholder:

```bash
sudo tee /var/www/educue-home/releases/bootstrap/index.html >/dev/null <<'EOF'
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>Educue Home</title>
</head>
<body>
  <h1>Educue Home</h1>
  <p>Production deployment is being prepared.</p>
</body>
</html>
EOF
```

Symlinks:

```bash
sudo ln -sfn \
  /var/www/educue-portal/releases/bootstrap \
  /var/www/educue-portal/current

sudo ln -sfn \
  /var/www/educue-home/releases/bootstrap \
  /var/www/educue-home/current
```

Permissions:

```bash
sudo chown -R root:www-data /var/www/educue-portal
sudo chown -R root:www-data /var/www/educue-home

sudo find /var/www/educue-portal -type d -exec chmod 755 {} \;
sudo find /var/www/educue-portal -type f -exec chmod 644 {} \;

sudo find /var/www/educue-home -type d -exec chmod 755 {} \;
sudo find /var/www/educue-home -type f -exec chmod 644 {} \;
```

---

# 8. Configure the Backend Environment

Create:

```bash
sudo nano /opt/educue/config/educue.env
```

Template:

```env
SPRING_PROFILES_ACTIVE=production
APPLICATION_NAME=educue

SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8080

APP_TIME_ZONE=Africa/Nairobi
FORWARD_HEADERS_STRATEGY=framework
SHUTDOWN_TIMEOUT=30s

DB_URL=jdbc:postgresql://127.0.0.1:5432/educue
DB_USERNAME=educue_app
DB_PASSWORD=REPLACE_WITH_REAL_DATABASE_PASSWORD

DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5
DB_CONNECTION_TIMEOUT_MS=30000

JWT_SECRET=REPLACE_WITH_STRONG_RANDOM_SECRET
JWT_EXPIRATION_MINUTES=60
AUTH_COOKIE_SECURE=true

FRONTEND_BASE_URL=https://portal.ebadynamics.co.ke
PUBLIC_BASE_URL=https://portal.ebadynamics.co.ke
CORS_ALLOWED_ORIGINS=https://portal.ebadynamics.co.ke
DEFAULT_AVATAR_URL=REPLACE_IF_USED

BOOTSTRAP_ADMIN_EMAIL=REPLACE
BOOTSTRAP_ADMIN_PASSWORD=REPLACE
BOOTSTRAP_ADMIN_FULL_NAME="System Administrator"

PASSWORD_RESET_MAIL_ENABLED=true
EXPOSE_PASSWORD_RESET_TOKEN=false

SMTP_HOST=REPLACE
SMTP_PORT=587
SMTP_USERNAME=REPLACE
SMTP_PASSWORD=REPLACE
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_CONNECTION_TIMEOUT_MS=5000
SMTP_TIMEOUT_MS=10000
SMTP_WRITE_TIMEOUT_MS=10000

CLOUDINARY_CLOUD_NAME=REPLACE
CLOUDINARY_API_KEY=REPLACE
CLOUDINARY_API_SECRET=REPLACE

MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=25MB

ADMISSION_WORKER_DELAY_MS=2000
ADMISSION_RECOVERY_DELAY_MS=60000

FINANCE_RECEIPT_PREFIX=MB02

MPESA_BASE_URL=https://api.safaricom.co.ke
MPESA_C2B_REGISTER_PATH=/mpesa/c2b/v1/registerurl
MPESA_CONSUMER_KEY=REPLACE
MPESA_CONSUMER_SECRET=REPLACE
MPESA_C2B_SHORT_CODE=REPLACE
MPESA_STK_SHORT_CODE=REPLACE
MPESA_STK_PASSKEY=REPLACE
MPESA_STK_CALLBACK_TOKEN=REPLACE
MPESA_CALLBACK_TOKEN=REPLACE
MPESA_PHONE_HASH_PEPPER=REPLACE
MPESA_WORKER_DELAY_MS=500
MPESA_RECOVERY_DELAY_MS=60000

LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=INFO
LOG_LEVEL_SQL=WARN
HEALTH_SHOW_DETAILS=when_authorized
```

Generate random secrets where appropriate:

```bash
openssl rand -base64 64
```

or:

```bash
openssl rand -hex 32
```

Protect:

```bash
sudo chown root:educue /opt/educue/config/educue.env
sudo chmod 640 /opt/educue/config/educue.env
```

Verify:

```bash
sudo -u educue test -r /opt/educue/config/educue.env \
  && echo "educue.env readable"
```

Never commit this production file.

---

# 9. Install and Bootstrap Nginx

```bash
sudo apt update
sudo apt install -y nginx
sudo systemctl enable --now nginx
```

Remove packaged default site:

```bash
sudo rm -f /etc/nginx/sites-enabled/default
```

## 9.1 Shared Nginx settings

Create:

```bash
sudo nano /etc/nginx/conf.d/educue-global.conf
```

```nginx
server_tokens off;

map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

gzip on;
gzip_vary on;
gzip_proxied any;
gzip_comp_level 5;
gzip_min_length 1024;

gzip_types
    text/plain
    text/css
    text/xml
    application/json
    application/javascript
    application/xml
    application/rss+xml
    image/svg+xml;
```

## 9.2 ACME webroot

```bash
sudo mkdir -p /var/www/letsencrypt/.well-known/acme-challenge
sudo chown -R root:www-data /var/www/letsencrypt
sudo chmod -R 755 /var/www/letsencrypt
```

## 9.3 Temporary HTTP portal config

```bash
sudo nano /etc/nginx/sites-available/educue-portal
```

```nginx
server {
    listen 80;
    listen [::]:80;

    server_name portal.ebadynamics.co.ke;

    root /var/www/educue-portal/current;
    index index.html;

    client_max_body_size 30m;

    access_log /var/log/nginx/educue-portal.access.log;
    error_log /var/log/nginx/educue-portal.error.log warn;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
        default_type text/plain;
        try_files $uri =404;
    }

    location = /api {
        proxy_pass http://127.0.0.1:8080;

        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;

        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location ^~ /api/ {
        # No URI is appended to proxy_pass.
        # /api/foo stays /api/foo upstream.
        proxy_pass http://127.0.0.1:8080;

        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;

        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location ^~ /assets/ {
        try_files $uri =404;

        expires 1y;
        add_header Cache-Control "public, immutable";

        access_log off;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

## 9.4 Temporary HTTP home config

```bash
sudo nano /etc/nginx/sites-available/educue-home
```

```nginx
server {
    listen 80;
    listen [::]:80;

    server_name home.ebadynamics.co.ke;

    root /var/www/educue-home/current;
    index index.html;

    access_log /var/log/nginx/educue-home.access.log;
    error_log /var/log/nginx/educue-home.error.log warn;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
        default_type text/plain;
        try_files $uri =404;
    }

    location ^~ /_next/static/ {
        try_files $uri =404;

        expires 1y;
        add_header Cache-Control "public, immutable";

        access_log off;
    }

    location / {
        try_files $uri $uri.html $uri/ =404;
    }

    error_page 404 /404.html;

    location = /404.html {
        internal;
    }
}
```

Enable:

```bash
sudo ln -s \
  /etc/nginx/sites-available/educue-portal \
  /etc/nginx/sites-enabled/educue-portal

sudo ln -s \
  /etc/nginx/sites-available/educue-home \
  /etc/nginx/sites-enabled/educue-home
```

Test:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Local tests:

```bash
curl -I \
  -H "Host: portal.ebadynamics.co.ke" \
  http://127.0.0.1
```

```bash
curl -I \
  -H "Host: home.ebadynamics.co.ke" \
  http://127.0.0.1
```

---

# 10. Issue Let's Encrypt Certificates

Install Certbot:

```bash
sudo snap install core 2>/dev/null || true
sudo snap refresh core

sudo snap install --classic certbot
sudo ln -sfn /snap/bin/certbot /usr/local/bin/certbot
```

Verify:

```bash
certbot --version
```

Portal:

```bash
sudo certbot certonly \
  --webroot \
  -w /var/www/letsencrypt \
  -d portal.ebadynamics.co.ke
```

Home:

```bash
sudo certbot certonly \
  --webroot \
  -w /var/www/letsencrypt \
  -d home.ebadynamics.co.ke
```

List:

```bash
sudo certbot certificates
```

Expected certificate paths:

```text
/etc/letsencrypt/live/portal.ebadynamics.co.ke/fullchain.pem
/etc/letsencrypt/live/portal.ebadynamics.co.ke/privkey.pem

/etc/letsencrypt/live/home.ebadynamics.co.ke/fullchain.pem
/etc/letsencrypt/live/home.ebadynamics.co.ke/privkey.pem
```

Test renewal:

```bash
sudo certbot renew --dry-run
```

If HTTP validation fails while Cloudflare is proxied, temporarily make `portal` and `home` **DNS only**, issue certificates, then restore **Proxied**.

---

# 11. Install the Final HTTPS Nginx Configuration

## 11.1 Final portal config

Replace `/etc/nginx/sites-available/educue-portal`:

```nginx
server {
    listen 80;
    listen [::]:80;

    server_name portal.ebadynamics.co.ke;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
        default_type text/plain;
        try_files $uri =404;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;

    server_name portal.ebadynamics.co.ke;

    root /var/www/educue-portal/current;
    index index.html;

    ssl_certificate
        /etc/letsencrypt/live/portal.ebadynamics.co.ke/fullchain.pem;

    ssl_certificate_key
        /etc/letsencrypt/live/portal.ebadynamics.co.ke/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;

    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    access_log /var/log/nginx/educue-portal.access.log;
    error_log /var/log/nginx/educue-portal.error.log warn;

    client_max_body_size 30m;

    add_header Strict-Transport-Security
        "max-age=31536000; includeSubDomains"
        always;

    add_header X-Content-Type-Options
        "nosniff"
        always;

    add_header X-Frame-Options
        "DENY"
        always;

    add_header Referrer-Policy
        "strict-origin-when-cross-origin"
        always;

    add_header Permissions-Policy
        "camera=(), microphone=(), geolocation=()"
        always;

    location = /api {
        proxy_pass http://127.0.0.1:8080;

        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;

        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location ^~ /api/ {
        proxy_pass http://127.0.0.1:8080;

        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;

        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location ^~ /assets/ {
        try_files $uri =404;

        expires 1y;
        add_header Cache-Control "public, immutable";

        access_log off;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

## 11.2 Final home config

Replace `/etc/nginx/sites-available/educue-home`:

```nginx
server {
    listen 80;
    listen [::]:80;

    server_name home.ebadynamics.co.ke;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
        default_type text/plain;
        try_files $uri =404;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;

    server_name home.ebadynamics.co.ke;

    root /var/www/educue-home/current;
    index index.html;

    ssl_certificate
        /etc/letsencrypt/live/home.ebadynamics.co.ke/fullchain.pem;

    ssl_certificate_key
        /etc/letsencrypt/live/home.ebadynamics.co.ke/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;

    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    access_log /var/log/nginx/educue-home.access.log;
    error_log /var/log/nginx/educue-home.error.log warn;

    add_header Strict-Transport-Security
        "max-age=31536000; includeSubDomains"
        always;

    add_header X-Content-Type-Options
        "nosniff"
        always;

    add_header X-Frame-Options
        "SAMEORIGIN"
        always;

    add_header Referrer-Policy
        "strict-origin-when-cross-origin"
        always;

    add_header Permissions-Policy
        "camera=(), microphone=(), geolocation=()"
        always;

    location ^~ /_next/static/ {
        try_files $uri =404;

        expires 1y;
        add_header Cache-Control "public, immutable";

        access_log off;
    }

    location / {
        try_files $uri $uri.html $uri/ =404;
    }

    error_page 404 /404.html;

    location = /404.html {
        internal;
    }
}
```

Validate/reload:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Test:

```bash
curl -I https://portal.ebadynamics.co.ke
curl -I https://home.ebadynamics.co.ke
```

## 11.3 Cloudflare final setting

After origin TLS works:

1. Set `portal` and `home` to **Proxied**.
2. Cloudflare → SSL/TLS → Overview.
3. Select:

```text
Full (strict)
```

---

# 12. Create the Spring Boot systemd Service

```bash
sudo nano /etc/systemd/system/educue.service
```

```ini
[Unit]
Description=Educue Spring Boot Application
After=network-online.target postgresql.service
Wants=network-online.target
Requires=postgresql.service

[Service]
Type=simple

User=educue
Group=educue

WorkingDirectory=/opt/educue

EnvironmentFile=/opt/educue/config/educue.env

ExecStart=/usr/bin/java -jar /opt/educue/current/educue.jar

Restart=on-failure
RestartSec=5s

TimeoutStartSec=120
TimeoutStopSec=30

KillSignal=SIGTERM
SuccessExitStatus=143

StandardOutput=journal
StandardError=journal
SyslogIdentifier=educue

NoNewPrivileges=true
PrivateTmp=true
PrivateDevices=true
ProtectSystem=strict
ProtectHome=true
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectKernelLogs=true
ProtectControlGroups=true

RestrictSUIDSGID=true
LockPersonality=true
RestrictRealtime=true
RemoveIPC=true

ReadWritePaths=/opt/educue/storage
ReadWritePaths=/opt/educue/logs

LimitNOFILE=65535
UMask=0027

[Install]
WantedBy=multi-user.target
```

Reload:

```bash
sudo systemctl daemon-reload
```

Validate:

```bash
sudo systemd-analyze verify /etc/systemd/system/educue.service
```

Do not manually start it before the first backend artifact exists.

---

# 13. Spring Boot Health Endpoint Requirements

Deployment health URL:

```text
http://127.0.0.1:8080/actuator/health/readiness
```

Example Spring configuration:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

  endpoint:
    health:
      probes:
        enabled: true
      show-details: ${HEALTH_SHOW_DETAILS:when_authorized}
```

Environment:

```env
HEALTH_SHOW_DETAILS=when_authorized
```

## 13.1 Spring Security

Allow health:

```java
.requestMatchers(
        "/actuator/health",
        "/actuator/health/**"
).permitAll()
```

before:

```java
.anyRequest().authenticated()
```

If desired for monitoring:

```java
.requestMatchers(
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/metrics",
        "/actuator/info",
        "/actuator/prometheus"
).permitAll()
```

Nginx does not proxy `/actuator/*`, and Spring is bound to loopback.

## 13.2 Custom JWT filter

If a custom JWT filter rejects a missing token, `permitAll()` can still appear to return `403`.

Token absence should normally continue:

```java
if (token == null) {
    filterChain.doFilter(request, response);
    return;
}
```

Or skip health for a `OncePerRequestFilter`:

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();

    return path.equals("/actuator/health")
            || path.startsWith("/actuator/health/");
}
```

Verify:

```bash
curl -i \
  http://127.0.0.1:8080/actuator/health/readiness
```

Expected:

```text
HTTP/1.1 200
```

```json
{"status":"UP"}
```

---

# 14. Create the GitHub Deployment User and SSH Key

## 14.1 Generate the key on your LOCAL machine

Git Bash/Linux/macOS:

```bash
ssh-keygen \
  -t ed25519 \
  -C "github-actions-educue-production" \
  -f ~/.ssh/educue_prod_deploy \
  -N ""
```

PowerShell:

```powershell
ssh-keygen `
  -t ed25519 `
  -C "github-actions-educue-production" `
  -f "$HOME\.ssh\educue_prod_deploy" `
  -N ""
```

Files:

```text
educue_prod_deploy       PRIVATE
educue_prod_deploy.pub   PUBLIC
```

Never commit/share the private key.

## 14.2 Create deploy account

On VM:

```bash
sudo useradd \
  --create-home \
  --shell /bin/bash \
  deploy
```

Lock password:

```bash
sudo passwd -l deploy
```

SSH directory:

```bash
sudo install -d \
  -o deploy \
  -g deploy \
  -m 700 \
  /home/deploy/.ssh
```

Create:

```bash
sudo nano /home/deploy/.ssh/authorized_keys
```

Paste public key, optionally with:

```text
restrict ssh-ed25519 AAAA... github-actions-educue-production
```

Permissions:

```bash
sudo chown deploy:deploy /home/deploy/.ssh/authorized_keys
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

Incoming:

```bash
sudo install -d \
  -o deploy \
  -g deploy \
  -m 700 \
  /home/deploy/incoming
```

## 14.3 Test locally

```bash
ssh \
  -i ~/.ssh/educue_prod_deploy \
  deploy@YOUR_VM_STATIC_PUBLIC_IP
```

Then:

```bash
whoami
```

Expected:

```text
deploy
```

## 14.4 Generate GitHub known-host value

On VM:

```bash
VM_IP="YOUR_VM_STATIC_PUBLIC_IP"

echo "$VM_IP $(sudo awk '{print $1" "$2}' \
  /etc/ssh/ssh_host_ed25519_key.pub)"
```

Copy full output as:

```text
PROD_SSH_KNOWN_HOSTS
```

## 14.5 Private-key GitHub secret value

Local PowerShell:

```powershell
Get-Content $HOME\.ssh\educue_prod_deploy
```

or:

```bash
cat ~/.ssh/educue_prod_deploy
```

Copy the complete OpenSSH private key to:

```text
PROD_SSH_PRIVATE_KEY
```

---

# 15. Install the Backend Deployment Script

Create:

```bash
sudo nano /usr/local/sbin/educue-deploy
```

```bash
#!/usr/bin/env bash

set -Eeuo pipefail

APP_NAME="educue"

BASE_DIR="/opt/educue"
RELEASES_DIR="$BASE_DIR/releases"
CURRENT_LINK="$BASE_DIR/current"

INCOMING_DIR="/home/deploy/incoming"

HEALTH_URL="http://127.0.0.1:8080/actuator/health/readiness"

KEEP_RELEASES=1
HEALTH_ATTEMPTS=45
HEALTH_DELAY_SECONDS=2

exec 9>/var/lock/educue-deploy.lock

if ! flock -n 9; then
    echo "Another Educue deployment is already running."
    exit 1
fi

SHA="${1:-}"

if [[ ! "$SHA" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Invalid Git commit SHA: $SHA"
    exit 2
fi

ARTIFACT="$INCOMING_DIR/$SHA.jar"
RELEASE_DIR="$RELEASES_DIR/$SHA"

if [[ ! -f "$ARTIFACT" ]]; then
    echo "Deployment artifact does not exist: $ARTIFACT"
    exit 3
fi

if [[ -L "$ARTIFACT" ]]; then
    echo "Deployment artifact must not be a symbolic link."
    exit 4
fi

ARTIFACT_OWNER="$(stat -c '%U' "$ARTIFACT")"

if [[ "$ARTIFACT_OWNER" != "deploy" ]]; then
    echo "Unexpected artifact owner: $ARTIFACT_OWNER"
    exit 5
fi

if [[ ! -s "$ARTIFACT" ]]; then
    echo "Deployment artifact is empty."
    exit 6
fi

PREVIOUS_RELEASE=""

if [[ -L "$CURRENT_LINK" ]]; then
    PREVIOUS_RELEASE="$(readlink -f "$CURRENT_LINK" || true)"
fi

switch_current() {
    local target="$1"
    local temporary_link="$BASE_DIR/.current-${SHA}-$$"

    rm -f "$temporary_link"
    ln -s "$target" "$temporary_link"
    mv -Tf "$temporary_link" "$CURRENT_LINK"
}

wait_until_healthy() {
    echo "Checking application readiness..."

    for attempt in $(seq 1 "$HEALTH_ATTEMPTS"); do
        RESPONSE="$(
            curl \
              --silent \
              --show-error \
              --fail \
              --max-time 3 \
              "$HEALTH_URL" \
              2>/dev/null || true
        )"

        if echo "$RESPONSE" |
            grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'
        then
            echo "Application is healthy."
            return 0
        fi

        echo "Health check $attempt/$HEALTH_ATTEMPTS not ready."
        sleep "$HEALTH_DELAY_SECONDS"
    done

    return 1
}

rollback() {
    echo "Deployment failed — starting rollback"

    if [[ -n "$PREVIOUS_RELEASE" &&
          -d "$PREVIOUS_RELEASE" &&
          "$PREVIOUS_RELEASE" != "$RELEASE_DIR" ]]
    then
        echo "Restoring: $PREVIOUS_RELEASE"

        switch_current "$PREVIOUS_RELEASE"
        systemctl restart "$APP_NAME" || true

        if wait_until_healthy; then
            echo "Rollback successful."
        else
            echo "CRITICAL: previous release also failed health check."
        fi
    else
        echo "No previous release exists."

        rm -f "$CURRENT_LINK"
        systemctl disable --now "$APP_NAME" || true
    fi

    echo "Failed release retained: $RELEASE_DIR"
    exit 10
}

rm -rf "$RELEASE_DIR"

install -d \
  -o educue \
  -g educue \
  -m 0750 \
  "$RELEASE_DIR"

install \
  -o educue \
  -g educue \
  -m 0640 \
  "$ARTIFACT" \
  "$RELEASE_DIR/educue.jar"

rm -f "$ARTIFACT"

switch_current "$RELEASE_DIR"

systemctl enable "$APP_NAME" >/dev/null 2>&1 || true

if ! systemctl restart "$APP_NAME"; then
    rollback
fi

if ! wait_until_healthy; then
    rollback
fi

echo "BACKEND DEPLOYMENT SUCCESSFUL"
echo "Release: $RELEASE_DIR"

CURRENT_RELEASE="$(readlink -f "$CURRENT_LINK")"

mapfile -t OLD_RELEASES < <(
    find "$RELEASES_DIR" \
      -mindepth 1 \
      -maxdepth 1 \
      -type d \
      -printf '%T@ %p\n' |
    sort -nr |
    awk '{print $2}' |
    tail -n "+$((KEEP_RELEASES + 1))"
)

for OLD_RELEASE in "${OLD_RELEASES[@]}"; do
    if [[ "$OLD_RELEASE" != "$CURRENT_RELEASE" ]]; then
        rm -rf -- "$OLD_RELEASE"
    fi
done
```

Permissions:

```bash
sudo chown root:root /usr/local/sbin/educue-deploy
sudo chmod 755 /usr/local/sbin/educue-deploy
```

---

# 16. Install the React Portal Deployment Script

```bash
sudo nano /usr/local/sbin/educue-portal-deploy
```

```bash
#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="portal.ebadynamics.co.ke"

BASE_DIR="/var/www/educue-portal"
RELEASES_DIR="$BASE_DIR/releases"
CURRENT_LINK="$BASE_DIR/current"
INCOMING_DIR="/home/deploy/incoming"

KEEP_RELEASES=5

exec 9>/var/lock/educue-portal-deploy.lock

if ! flock -n 9; then
    echo "Another portal deployment is already running."
    exit 1
fi

SHA="${1:-}"

if [[ ! "$SHA" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Invalid Git SHA: $SHA"
    exit 2
fi

ARCHIVE_NAME="educue-portal-${SHA}.tar.gz"

ARCHIVE="$INCOMING_DIR/$ARCHIVE_NAME"
CHECKSUM="$INCOMING_DIR/${ARCHIVE_NAME}.sha256"
RELEASE_DIR="$RELEASES_DIR/$SHA"

if [[ ! -f "$ARCHIVE" ]]; then
    echo "Portal archive missing: $ARCHIVE"
    exit 3
fi

if [[ ! -f "$CHECKSUM" ]]; then
    echo "Checksum missing: $CHECKSUM"
    exit 4
fi

if [[ -L "$ARCHIVE" || ! -s "$ARCHIVE" ]]; then
    echo "Invalid archive."
    exit 5
fi

cd "$INCOMING_DIR"
sha256sum --check "${ARCHIVE_NAME}.sha256"

if tar -tzf "$ARCHIVE" |
    grep -Eq '(^/|(^|/)\.\.(/|$))'
then
    echo "Unsafe archive path."
    exit 6
fi

PREVIOUS_RELEASE=""

if [[ -L "$CURRENT_LINK" ]]; then
    PREVIOUS_RELEASE="$(readlink -f "$CURRENT_LINK" || true)"
fi

switch_current() {
    local target="$1"
    local temporary="$BASE_DIR/.current-${SHA}-$$"

    rm -f "$temporary"
    ln -s "$target" "$temporary"
    mv -Tf "$temporary" "$CURRENT_LINK"
}

rm -rf "$RELEASE_DIR"

install -d \
  -o root \
  -g www-data \
  -m 0755 \
  "$RELEASE_DIR"

tar \
  -xzf "$ARCHIVE" \
  -C "$RELEASE_DIR" \
  --no-same-owner \
  --no-same-permissions

if [[ ! -f "$RELEASE_DIR/index.html" ]]; then
    echo "index.html not found."
    rm -rf "$RELEASE_DIR"
    exit 7
fi

chown -R root:www-data "$RELEASE_DIR"
find "$RELEASE_DIR" -type d -exec chmod 0755 {} \;
find "$RELEASE_DIR" -type f -exec chmod 0644 {} \;

switch_current "$RELEASE_DIR"

if curl \
    --fail \
    --silent \
    --show-error \
    --max-time 15 \
    --resolve "${DOMAIN}:443:127.0.0.1" \
    "https://${DOMAIN}/" \
    >/dev/null
then
    echo "Portal health check successful."
else
    echo "Portal health check failed."

    if [[ -n "$PREVIOUS_RELEASE" &&
          -d "$PREVIOUS_RELEASE" ]]
    then
        switch_current "$PREVIOUS_RELEASE"
    else
        rm -f "$CURRENT_LINK"
    fi

    exit 10
fi

rm -f "$ARCHIVE"
rm -f "$CHECKSUM"

CURRENT_RELEASE="$(readlink -f "$CURRENT_LINK")"

mapfile -t OLD_RELEASES < <(
    find "$RELEASES_DIR" \
      -mindepth 1 \
      -maxdepth 1 \
      -type d \
      -printf '%T@ %p\n' |
    sort -nr |
    awk '{print $2}' |
    tail -n "+$((KEEP_RELEASES + 1))"
)

for OLD_RELEASE in "${OLD_RELEASES[@]}"; do
    if [[ "$OLD_RELEASE" != "$CURRENT_RELEASE" ]]; then
        rm -rf -- "$OLD_RELEASE"
    fi
done

echo "PORTAL DEPLOYMENT SUCCESSFUL"
echo "Release: $RELEASE_DIR"
```

```bash
sudo chown root:root /usr/local/sbin/educue-portal-deploy
sudo chmod 755 /usr/local/sbin/educue-portal-deploy
```

---

# 17. Install the Next Home Deployment Script

```bash
sudo nano /usr/local/sbin/educue-home-deploy
```

```bash
#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="home.ebadynamics.co.ke"

BASE_DIR="/var/www/educue-home"
RELEASES_DIR="$BASE_DIR/releases"
CURRENT_LINK="$BASE_DIR/current"
INCOMING_DIR="/home/deploy/incoming"

KEEP_RELEASES=5

exec 9>/var/lock/educue-home-deploy.lock

if ! flock -n 9; then
    echo "Another home deployment is already running."
    exit 1
fi

SHA="${1:-}"

if [[ ! "$SHA" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Invalid Git SHA: $SHA"
    exit 2
fi

ARCHIVE_NAME="educue-home-${SHA}.tar.gz"

ARCHIVE="$INCOMING_DIR/$ARCHIVE_NAME"
CHECKSUM="$INCOMING_DIR/${ARCHIVE_NAME}.sha256"
RELEASE_DIR="$RELEASES_DIR/$SHA"

if [[ ! -f "$ARCHIVE" ]]; then
    echo "Home archive missing: $ARCHIVE"
    exit 3
fi

if [[ ! -f "$CHECKSUM" ]]; then
    echo "Checksum missing: $CHECKSUM"
    exit 4
fi

if [[ -L "$ARCHIVE" || ! -s "$ARCHIVE" ]]; then
    echo "Invalid archive."
    exit 5
fi

cd "$INCOMING_DIR"
sha256sum --check "${ARCHIVE_NAME}.sha256"

if tar -tzf "$ARCHIVE" |
    grep -Eq '(^/|(^|/)\.\.(/|$))'
then
    echo "Unsafe archive path."
    exit 6
fi

PREVIOUS_RELEASE=""

if [[ -L "$CURRENT_LINK" ]]; then
    PREVIOUS_RELEASE="$(readlink -f "$CURRENT_LINK" || true)"
fi

switch_current() {
    local target="$1"
    local temporary="$BASE_DIR/.current-${SHA}-$$"

    rm -f "$temporary"
    ln -s "$target" "$temporary"
    mv -Tf "$temporary" "$CURRENT_LINK"
}

rm -rf "$RELEASE_DIR"

install -d \
  -o root \
  -g www-data \
  -m 0755 \
  "$RELEASE_DIR"

tar \
  -xzf "$ARCHIVE" \
  -C "$RELEASE_DIR" \
  --no-same-owner \
  --no-same-permissions

if [[ ! -f "$RELEASE_DIR/index.html" ]]; then
    echo "index.html not found."
    rm -rf "$RELEASE_DIR"
    exit 7
fi

chown -R root:www-data "$RELEASE_DIR"
find "$RELEASE_DIR" -type d -exec chmod 0755 {} \;
find "$RELEASE_DIR" -type f -exec chmod 0644 {} \;

switch_current "$RELEASE_DIR"

if curl \
    --fail \
    --silent \
    --show-error \
    --max-time 15 \
    --resolve "${DOMAIN}:443:127.0.0.1" \
    "https://${DOMAIN}/" \
    >/dev/null
then
    echo "Home health check successful."
else
    echo "Home health check failed."

    if [[ -n "$PREVIOUS_RELEASE" &&
          -d "$PREVIOUS_RELEASE" ]]
    then
        switch_current "$PREVIOUS_RELEASE"
    else
        rm -f "$CURRENT_LINK"
    fi

    exit 10
fi

rm -f "$ARCHIVE"
rm -f "$CHECKSUM"

CURRENT_RELEASE="$(readlink -f "$CURRENT_LINK")"

mapfile -t OLD_RELEASES < <(
    find "$RELEASES_DIR" \
      -mindepth 1 \
      -maxdepth 1 \
      -type d \
      -printf '%T@ %p\n' |
    sort -nr |
    awk '{print $2}' |
    tail -n "+$((KEEP_RELEASES + 1))"
)

for OLD_RELEASE in "${OLD_RELEASES[@]}"; do
    if [[ "$OLD_RELEASE" != "$CURRENT_RELEASE" ]]; then
        rm -rf -- "$OLD_RELEASE"
    fi
done

echo "HOME DEPLOYMENT SUCCESSFUL"
echo "Release: $RELEASE_DIR"
```

```bash
sudo chown root:root /usr/local/sbin/educue-home-deploy
sudo chmod 755 /usr/local/sbin/educue-home-deploy
```

---

# 18. Configure sudoers for Deployment

```bash
sudo visudo -f /etc/sudoers.d/educue-deploy
```

```sudoers
deploy ALL=(root) NOPASSWD: /usr/local/sbin/educue-deploy
deploy ALL=(root) NOPASSWD: /usr/local/sbin/educue-portal-deploy
deploy ALL=(root) NOPASSWD: /usr/local/sbin/educue-home-deploy
```

```bash
sudo chmod 440 /etc/sudoers.d/educue-deploy
sudo visudo -c
```

Expected:

```text
parsed OK
```

---

# 19. Git Branch Model

Use:

```text
feature   development
main      production
```

Normal flow:

```text
feature
   |
   v
CI
   |
   v
Pull Request
   |
   v
main
   |
   v
production deployment
```

Development:

```bash
git switch feature
git pull origin feature

git add .
git commit -m "feat: describe change"
git push origin feature
```

PR:

```text
base: main
compare: feature
```

Do not routinely develop directly on `main`.

---

# 20. GitHub Environment and Secret Model

For **each repository**:

```text
Settings
→ Environments
→ New environment
→ production
```

Where supported/configured, restrict `production` to `main`.

## Common environment variables

```text
PROD_HOST=<VM STATIC PUBLIC IP>
PROD_USER=deploy
```

## Common environment secrets

```text
PROD_SSH_PRIVATE_KEY
PROD_SSH_KNOWN_HOSTS
```

`PROD_HOST` must be the VM static IP, not the Cloudflare hostname.

---

# 21. Backend GitHub Actions Setup

Example backend repo:

```text
ChrisOwuor/educue-api
```

Runtime secrets such as database, JWT, SMTP, M-Pesa and Cloudinary stay **only on the VM** in:

```text
/opt/educue/config/educue.env
```

## 21.1 Backend CI uses temporary PostgreSQL

GitHub CI runs PostgreSQL 17 as a temporary service container.

It is not the production DB and is destroyed after the job.

## 21.2 Backend workflow

Create:

```text
.github/workflows/backend-ci.yml
```

```yaml
name: Educue Backend CI/CD

on:
  push:
    branches:
      - feature
      - main

  pull_request:
    branches:
      - main

  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    name: Test and Build
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:17

        env:
          POSTGRES_DB: educue_test
          POSTGRES_USER: educue_test
          POSTGRES_PASSWORD: educue_test

        ports:
          - 5432:5432

        options: >-
          --health-cmd="pg_isready -U educue_test -d educue_test"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

    env:
      DB_URL: jdbc:postgresql://127.0.0.1:5432/educue_test
      DB_USERNAME: educue_test
      DB_PASSWORD: educue_test

    steps:
      - name: Checkout source
        uses: actions/checkout@v7

      - name: Set up Java 21
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
          cache: maven

      - name: Make Maven wrapper executable
        run: chmod +x mvnw

      - name: Test and build
        run: ./mvnw -B clean verify

      - name: Show test failure reports
        if: failure()
        shell: bash
        run: |
          echo "================================================"
          echo "SUREFIRE TEST FAILURE REPORTS"
          echo "================================================"

          if [ -d target/surefire-reports ]; then
            find target/surefire-reports \
              -type f \
              -name "*.txt" \
              -print \
              -exec cat {} \;

            grep -RniE \
              "Caused by:|Exception|Error creating bean|Failed to configure|Connection refused|Could not resolve placeholder|password authentication failed|Flyway|DataSource" \
              target/surefire-reports \
              || true
          else
            echo "No Surefire reports generated."
          fi

      - name: Upload failed test reports
        if: failure()
        uses: actions/upload-artifact@v7
        with:
          name: test-reports-${{ github.sha }}
          path: target/surefire-reports/
          retention-days: 7
          if-no-files-found: ignore

      - name: Prepare deployment artifact
        if: >
          (github.event_name == 'push' &&
           github.ref == 'refs/heads/main') ||
          (github.event_name == 'workflow_dispatch' &&
           github.ref == 'refs/heads/main')
        shell: bash
        run: |
          set -Eeuo pipefail

          mkdir -p deployment

          JAR="$(
            find target \
              -maxdepth 1 \
              -type f \
              -name '*.jar' \
              ! -name '*sources.jar' \
              ! -name '*javadoc.jar' \
              | head -n 1
          )"

          if [ -z "$JAR" ]; then
            echo "No deployable JAR found."
            exit 1
          fi

          cp "$JAR" deployment/educue.jar

          cd deployment
          sha256sum educue.jar > educue.jar.sha256

          ls -lh

      - name: Upload deployment artifact
        if: >
          (github.event_name == 'push' &&
           github.ref == 'refs/heads/main') ||
          (github.event_name == 'workflow_dispatch' &&
           github.ref == 'refs/heads/main')
        uses: actions/upload-artifact@v7
        with:
          name: educue-backend-${{ github.sha }}
          path: deployment/
          retention-days: 7
          if-no-files-found: error

  deploy-production:
    name: Deploy Production

    needs:
      - build

    if: >
      (github.event_name == 'push' &&
       github.ref == 'refs/heads/main') ||
      (github.event_name == 'workflow_dispatch' &&
       github.ref == 'refs/heads/main')

    runs-on: ubuntu-latest

    environment:
      name: production
      url: https://portal.ebadynamics.co.ke

    concurrency:
      group: educue-backend-production
      cancel-in-progress: false

    steps:
      - name: Download deployment artifact
        uses: actions/download-artifact@v8
        with:
          name: educue-backend-${{ github.sha }}
          path: deployment

      - name: Verify artifact checksum
        shell: bash
        run: |
          set -Eeuo pipefail

          cd deployment
          sha256sum --check educue.jar.sha256
          test -s educue.jar

      - name: Validate deployment configuration
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          PROD_SSH_PRIVATE_KEY: ${{ secrets.PROD_SSH_PRIVATE_KEY }}
          PROD_SSH_KNOWN_HOSTS: ${{ secrets.PROD_SSH_KNOWN_HOSTS }}
        shell: bash
        run: |
          set -Eeuo pipefail

          test -n "$PROD_HOST" || { echo "PROD_HOST missing"; exit 1; }
          test -n "$PROD_USER" || { echo "PROD_USER missing"; exit 1; }
          test -n "$PROD_SSH_PRIVATE_KEY" || { echo "PROD_SSH_PRIVATE_KEY missing"; exit 1; }
          test -n "$PROD_SSH_KNOWN_HOSTS" || { echo "PROD_SSH_KNOWN_HOSTS missing"; exit 1; }

      - name: Configure SSH
        env:
          PROD_SSH_PRIVATE_KEY: ${{ secrets.PROD_SSH_PRIVATE_KEY }}
          PROD_SSH_KNOWN_HOSTS: ${{ secrets.PROD_SSH_KNOWN_HOSTS }}
        shell: bash
        run: |
          set -Eeuo pipefail

          install -d -m 700 "$HOME/.ssh"

          printf '%s\n' "$PROD_SSH_PRIVATE_KEY" \
            > "$HOME/.ssh/id_ed25519"

          chmod 600 "$HOME/.ssh/id_ed25519"

          printf '%s\n' "$PROD_SSH_KNOWN_HOSTS" \
            > "$HOME/.ssh/known_hosts"

          chmod 600 "$HOME/.ssh/known_hosts"

          ssh-keygen -y \
            -f "$HOME/.ssh/id_ed25519" \
            >/dev/null

      - name: Verify production SSH
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
        shell: bash
        run: |
          set -Eeuo pipefail

          ssh \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            -o ConnectTimeout=10 \
            "$PROD_USER@$PROD_HOST" \
            'echo "Production SSH connection successful."'

      - name: Upload application
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          set -Eeuo pipefail

          scp \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            deployment/educue.jar \
            "$PROD_USER@$PROD_HOST:/home/deploy/incoming/$RELEASE_SHA.jar"

      - name: Deploy release
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          set -Eeuo pipefail

          ssh \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            "$PROD_USER@$PROD_HOST" \
            "sudo /usr/local/sbin/educue-deploy '$RELEASE_SHA'"

      - name: Verify deployed release
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          set -Eeuo pipefail

          DEPLOYED="$(
            ssh \
              -i "$HOME/.ssh/id_ed25519" \
              -o BatchMode=yes \
              -o StrictHostKeyChecking=yes \
              "$PROD_USER@$PROD_HOST" \
              "readlink -f /opt/educue/current"
          )"

          echo "$DEPLOYED"
          echo "$DEPLOYED" | grep -q "$RELEASE_SHA"

      - name: Deployment summary
        env:
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          {
            echo "## Educue Backend Deployment"
            echo
            echo "✅ Backend deployment successful"
            echo
            echo "**Commit:** \`$RELEASE_SHA\`"
            echo
            echo "**Production:** https://portal.ebadynamics.co.ke"
          } >> "$GITHUB_STEP_SUMMARY"
```

---

# 22. React Portal GitHub Actions Setup

## 22.1 Repository variables

Add under:

```text
Settings
→ Secrets and variables
→ Actions
→ Variables
```

```text
VITE_API_BASE_URL=/api
VITE_AUTO_LOGOUT_MINUTES=30
VITE_PORTAL_URL=https://portal.ebadynamics.co.ke
VITE_SITE_URL=https://home.ebadynamics.co.ke
```

These are public build-time values.

Never put secrets in `VITE_*`.

## 22.2 `.env.example`

```env
VITE_API_BASE_URL=/api
VITE_AUTO_LOGOUT_MINUTES=30
VITE_PORTAL_URL=https://portal.ebadynamics.co.ke
VITE_SITE_URL=https://home.ebadynamics.co.ke
```

`.gitignore`:

```gitignore
.env
.env.*
!.env.example

dist/
```

## 22.3 Portal workflow

Create:

```text
.github/workflows/portal-ci.yml
```

```yaml
name: Educue Portal CI/CD

on:
  push:
    branches:
      - feature
      - main

  pull_request:
    branches:
      - main

  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    name: Build Portal
    runs-on: ubuntu-latest

    env:
      VITE_API_BASE_URL: ${{ vars.VITE_API_BASE_URL }}
      VITE_AUTO_LOGOUT_MINUTES: ${{ vars.VITE_AUTO_LOGOUT_MINUTES }}
      VITE_PORTAL_URL: ${{ vars.VITE_PORTAL_URL }}
      VITE_SITE_URL: ${{ vars.VITE_SITE_URL }}

    steps:
      - name: Checkout source
        uses: actions/checkout@v7

      - name: Set up Node
        uses: actions/setup-node@v6
        with:
          node-version: "22"
          cache: npm

      - name: Install dependencies
        run: npm ci

      - name: Build portal
        run: npm run build

      - name: Verify build
        shell: bash
        run: |
          set -Eeuo pipefail

          test -d dist
          test -f dist/index.html

          echo "Portal build successful."
          du -sh dist

      - name: Package deployment artifact
        if: >
          (github.event_name == 'push' &&
           github.ref == 'refs/heads/main') ||
          (github.event_name == 'workflow_dispatch' &&
           github.ref == 'refs/heads/main')
        shell: bash
        env:
          RELEASE_SHA: ${{ github.sha }}
        run: |
          set -Eeuo pipefail

          mkdir -p deployment

          ARCHIVE="educue-portal-${RELEASE_SHA}.tar.gz"

          tar \
            -C dist \
            -czf "deployment/$ARCHIVE" \
            .

          cd deployment
          sha256sum "$ARCHIVE" > "${ARCHIVE}.sha256"

          ls -lh

      - name: Upload deployment artifact
        if: >
          (github.event_name == 'push' &&
           github.ref == 'refs/heads/main') ||
          (github.event_name == 'workflow_dispatch' &&
           github.ref == 'refs/heads/main')
        uses: actions/upload-artifact@v7
        with:
          name: educue-portal-${{ github.sha }}
          path: deployment/
          retention-days: 7
          if-no-files-found: error

  deploy-production:
    name: Deploy Portal Production

    needs:
      - build

    if: >
      (github.event_name == 'push' &&
       github.ref == 'refs/heads/main') ||
      (github.event_name == 'workflow_dispatch' &&
       github.ref == 'refs/heads/main')

    runs-on: ubuntu-latest

    environment:
      name: production
      url: https://portal.ebadynamics.co.ke

    concurrency:
      group: educue-portal-production
      cancel-in-progress: false

    steps:
      - name: Download deployment artifact
        uses: actions/download-artifact@v8
        with:
          name: educue-portal-${{ github.sha }}
          path: deployment

      - name: Verify artifact
        shell: bash
        env:
          RELEASE_SHA: ${{ github.sha }}
        run: |
          set -Eeuo pipefail

          cd deployment

          ARCHIVE="educue-portal-${RELEASE_SHA}.tar.gz"

          sha256sum --check "${ARCHIVE}.sha256"
          test -s "$ARCHIVE"

      - name: Validate production configuration
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          PROD_SSH_PRIVATE_KEY: ${{ secrets.PROD_SSH_PRIVATE_KEY }}
          PROD_SSH_KNOWN_HOSTS: ${{ secrets.PROD_SSH_KNOWN_HOSTS }}
        shell: bash
        run: |
          set -Eeuo pipefail

          test -n "$PROD_HOST" || { echo "PROD_HOST missing"; exit 1; }
          test -n "$PROD_USER" || { echo "PROD_USER missing"; exit 1; }
          test -n "$PROD_SSH_PRIVATE_KEY" || { echo "PROD_SSH_PRIVATE_KEY missing"; exit 1; }
          test -n "$PROD_SSH_KNOWN_HOSTS" || { echo "PROD_SSH_KNOWN_HOSTS missing"; exit 1; }

      - name: Configure SSH
        env:
          PROD_SSH_PRIVATE_KEY: ${{ secrets.PROD_SSH_PRIVATE_KEY }}
          PROD_SSH_KNOWN_HOSTS: ${{ secrets.PROD_SSH_KNOWN_HOSTS }}
        shell: bash
        run: |
          set -Eeuo pipefail

          install -d -m 700 "$HOME/.ssh"

          printf '%s\n' "$PROD_SSH_PRIVATE_KEY" \
            > "$HOME/.ssh/id_ed25519"

          chmod 600 "$HOME/.ssh/id_ed25519"

          printf '%s\n' "$PROD_SSH_KNOWN_HOSTS" \
            > "$HOME/.ssh/known_hosts"

          chmod 600 "$HOME/.ssh/known_hosts"

          ssh-keygen -y \
            -f "$HOME/.ssh/id_ed25519" \
            >/dev/null

      - name: Verify production SSH
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
        shell: bash
        run: |
          set -Eeuo pipefail

          ssh \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            -o ConnectTimeout=10 \
            "$PROD_USER@$PROD_HOST" \
            'echo "Portal production SSH successful."'

      - name: Upload portal
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          set -Eeuo pipefail

          ARCHIVE="educue-portal-${RELEASE_SHA}.tar.gz"

          scp \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            "deployment/$ARCHIVE" \
            "deployment/${ARCHIVE}.sha256" \
            "$PROD_USER@$PROD_HOST:/home/deploy/incoming/"

      - name: Deploy portal
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          set -Eeuo pipefail

          ssh \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            "$PROD_USER@$PROD_HOST" \
            "sudo /usr/local/sbin/educue-portal-deploy '$RELEASE_SHA'"

      - name: Verify public portal
        shell: bash
        run: |
          set -Eeuo pipefail

          curl \
            --fail \
            --silent \
            --show-error \
            --location \
            --max-time 20 \
            https://portal.ebadynamics.co.ke/ \
            >/dev/null

          echo "Public portal responded successfully."

      - name: Deployment summary
        env:
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          {
            echo "## Educue Portal Deployment"
            echo
            echo "✅ Portal deployment successful"
            echo
            echo "**Commit:** \`$RELEASE_SHA\`"
            echo
            echo "**Production:** https://portal.ebadynamics.co.ke"
          } >> "$GITHUB_STEP_SUMMARY"
```

---

# 23. Next.js Home Site GitHub Actions Setup

## 23.1 Static export

`next.config.ts`:

```ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "export",

  images: {
    unoptimized: true,
  },
};

export default nextConfig;
```

A successful build should create:

```text
out/
├── index.html
├── 404.html
├── _next/
└── ...
```

## 23.2 Static metadata routes

With `output: "export"`, generated metadata routes must be build-time static.

Generated Open Graph:

```tsx
export const dynamic = "force-static";
```

Generated `robots.ts`:

```ts
export const dynamic = "force-static";
```

Generated `sitemap.ts`:

```ts
export const dynamic = "force-static";
```

Example `robots.ts`:

```ts
import type { MetadataRoute } from "next";
import { school } from "./site-config";

export const dynamic = "force-static";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: school.seo.indexSite ? "/" : undefined,
      disallow: school.seo.indexSite ? ["/api/"] : "/",
    },
    sitemap: `${school.seo.siteUrl}/sitemap.xml`,
    host: school.seo.siteUrl,
  };
}
```

Instead of generating an OG image with code, you can also use:

```text
app/opengraph-image.png
```

## 23.3 Repository variables

```text
NEXT_PUBLIC_SCHOOL_SITE_URL=https://home.ebadynamics.co.ke
NEXT_PUBLIC_EDUCUE_PORTAL_URL=https://portal.ebadynamics.co.ke
NEXT_PUBLIC_EDUCUE_API_URL=https://portal.ebadynamics.co.ke/api
```

These are public browser build-time values.

## 23.4 Home workflow

Create:

```text
.github/workflows/home-ci.yml
```

```yaml
name: Educue Home CI/CD

on:
  push:
    branches:
      - feature
      - main

  pull_request:
    branches:
      - main

  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    name: Build Home Site
    runs-on: ubuntu-latest

    env:
      NEXT_PUBLIC_SCHOOL_SITE_URL: ${{ vars.NEXT_PUBLIC_SCHOOL_SITE_URL }}
      NEXT_PUBLIC_EDUCUE_PORTAL_URL: ${{ vars.NEXT_PUBLIC_EDUCUE_PORTAL_URL }}
      NEXT_PUBLIC_EDUCUE_API_URL: ${{ vars.NEXT_PUBLIC_EDUCUE_API_URL }}

    steps:
      - name: Checkout source
        uses: actions/checkout@v7

      - name: Set up Node
        uses: actions/setup-node@v6
        with:
          node-version: "22"
          cache: npm

      - name: Install dependencies
        run: npm ci

      - name: Build static site
        run: npm run build

      - name: Verify static export
        shell: bash
        run: |
          set -Eeuo pipefail

          echo "Root directory after build:"
          ls -la

          if [ ! -d out ]; then
            echo "ERROR: out/ directory was not generated."
            echo "Ensure next.config.* contains output: 'export'."
            exit 1
          fi

          if [ ! -f out/index.html ]; then
            echo "ERROR: out/index.html was not generated."
            find out -maxdepth 2 -type f | head -50
            exit 1
          fi

          echo "Static export created successfully."
          du -sh out

      - name: Package deployment artifact
        if: >
          (github.event_name == 'push' &&
           github.ref == 'refs/heads/main') ||
          (github.event_name == 'workflow_dispatch' &&
           github.ref == 'refs/heads/main')
        shell: bash
        env:
          RELEASE_SHA: ${{ github.sha }}
        run: |
          set -Eeuo pipefail

          mkdir -p deployment

          ARCHIVE="educue-home-${RELEASE_SHA}.tar.gz"

          tar \
            -C out \
            -czf "deployment/$ARCHIVE" \
            .

          cd deployment
          sha256sum "$ARCHIVE" > "${ARCHIVE}.sha256"

          ls -lh

      - name: Upload deployment artifact
        if: >
          (github.event_name == 'push' &&
           github.ref == 'refs/heads/main') ||
          (github.event_name == 'workflow_dispatch' &&
           github.ref == 'refs/heads/main')
        uses: actions/upload-artifact@v7
        with:
          name: educue-home-${{ github.sha }}
          path: deployment/
          retention-days: 7
          if-no-files-found: error

  deploy-production:
    name: Deploy Home Production

    needs:
      - build

    if: >
      (github.event_name == 'push' &&
       github.ref == 'refs/heads/main') ||
      (github.event_name == 'workflow_dispatch' &&
       github.ref == 'refs/heads/main')

    runs-on: ubuntu-latest

    environment:
      name: production
      url: https://home.ebadynamics.co.ke

    concurrency:
      group: educue-home-production
      cancel-in-progress: false

    steps:
      - name: Download deployment artifact
        uses: actions/download-artifact@v8
        with:
          name: educue-home-${{ github.sha }}
          path: deployment

      - name: Verify artifact
        shell: bash
        env:
          RELEASE_SHA: ${{ github.sha }}
        run: |
          set -Eeuo pipefail

          cd deployment

          ARCHIVE="educue-home-${RELEASE_SHA}.tar.gz"

          sha256sum --check "${ARCHIVE}.sha256"
          test -s "$ARCHIVE"

      - name: Validate production configuration
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          PROD_SSH_PRIVATE_KEY: ${{ secrets.PROD_SSH_PRIVATE_KEY }}
          PROD_SSH_KNOWN_HOSTS: ${{ secrets.PROD_SSH_KNOWN_HOSTS }}
        shell: bash
        run: |
          set -Eeuo pipefail

          test -n "$PROD_HOST" || { echo "PROD_HOST missing"; exit 1; }
          test -n "$PROD_USER" || { echo "PROD_USER missing"; exit 1; }
          test -n "$PROD_SSH_PRIVATE_KEY" || { echo "PROD_SSH_PRIVATE_KEY missing"; exit 1; }
          test -n "$PROD_SSH_KNOWN_HOSTS" || { echo "PROD_SSH_KNOWN_HOSTS missing"; exit 1; }

      - name: Configure SSH
        env:
          PROD_SSH_PRIVATE_KEY: ${{ secrets.PROD_SSH_PRIVATE_KEY }}
          PROD_SSH_KNOWN_HOSTS: ${{ secrets.PROD_SSH_KNOWN_HOSTS }}
        shell: bash
        run: |
          set -Eeuo pipefail

          install -d -m 700 "$HOME/.ssh"

          printf '%s\n' "$PROD_SSH_PRIVATE_KEY" \
            > "$HOME/.ssh/id_ed25519"

          chmod 600 "$HOME/.ssh/id_ed25519"

          printf '%s\n' "$PROD_SSH_KNOWN_HOSTS" \
            > "$HOME/.ssh/known_hosts"

          chmod 600 "$HOME/.ssh/known_hosts"

          ssh-keygen -y \
            -f "$HOME/.ssh/id_ed25519" \
            >/dev/null

      - name: Verify production SSH
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
        shell: bash
        run: |
          set -Eeuo pipefail

          ssh \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            -o ConnectTimeout=10 \
            "$PROD_USER@$PROD_HOST" \
            'echo "Home production SSH successful."'

      - name: Upload home site
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          set -Eeuo pipefail

          ARCHIVE="educue-home-${RELEASE_SHA}.tar.gz"

          scp \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            "deployment/$ARCHIVE" \
            "deployment/${ARCHIVE}.sha256" \
            "$PROD_USER@$PROD_HOST:/home/deploy/incoming/"

      - name: Deploy home site
        env:
          PROD_HOST: ${{ vars.PROD_HOST }}
          PROD_USER: ${{ vars.PROD_USER }}
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          set -Eeuo pipefail

          ssh \
            -i "$HOME/.ssh/id_ed25519" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=yes \
            "$PROD_USER@$PROD_HOST" \
            "sudo /usr/local/sbin/educue-home-deploy '$RELEASE_SHA'"

      - name: Verify public home site
        shell: bash
        run: |
          set -Eeuo pipefail

          curl \
            --fail \
            --silent \
            --show-error \
            --location \
            --max-time 20 \
            https://home.ebadynamics.co.ke/ \
            >/dev/null

          echo "Public home site responded successfully."

      - name: Deployment summary
        env:
          RELEASE_SHA: ${{ github.sha }}
        shell: bash
        run: |
          {
            echo "## Educue Home Deployment"
            echo
            echo "✅ Home site deployment successful"
            echo
            echo "**Commit:** \`$RELEASE_SHA\`"
            echo
            echo "**Production:** https://home.ebadynamics.co.ke"
          } >> "$GITHUB_STEP_SUMMARY"
```

---

# 24. First Deployment Order

Complete server setup first:

```text
1. Ubuntu baseline
2. PostgreSQL 17
3. OpenJDK 21
4. runtime users and directories
5. backend educue.env
6. Nginx HTTP
7. Let's Encrypt
8. final Nginx HTTPS
9. systemd service
10. deploy SSH user/key
11. deployment scripts
12. sudoers
```

Then deploy:

```text
1. Backend
2. React portal
3. Next home
```

For every repository:

```text
feature
   -> CI
   -> PR to main
   -> merge
   -> production deploy
```

---

# 25. Verification Checklist

## PostgreSQL

```bash
sudo systemctl status postgresql --no-pager
sudo ss -lntp | grep 5432
```

## Java

```bash
java -version
```

## Nginx

```bash
sudo nginx -t
sudo systemctl status nginx --no-pager
```

## Certificates

```bash
sudo certbot certificates
sudo certbot renew --dry-run
```

## Backend

```bash
sudo systemctl status educue --no-pager
readlink -f /opt/educue/current

curl \
  http://127.0.0.1:8080/actuator/health/readiness
```

Expected:

```json
{"status":"UP"}
```

API through Nginx/Cloudflare:

```bash
curl -i \
  https://portal.ebadynamics.co.ke/api/auth/csrf
```

Expected:

```text
HTTP/2 200
```

## Portal

```bash
readlink -f /var/www/educue-portal/current
curl -I https://portal.ebadynamics.co.ke
```

## Home

```bash
readlink -f /var/www/educue-home/current
curl -I https://home.ebadynamics.co.ke
```

## Listener check

```bash
sudo ss -lntp |
  egrep ':22|:80|:443|:5432|:8080'
```

Desired:

```text
22    SSH
80    Nginx
443   Nginx
5432  localhost only
8080  127.0.0.1 only
```

---

# 26. Rollback and Release Management

## 26.1 Automatic backend rollback

The backend deploy script remembers the previous `current` target.

If readiness fails:

```text
new release
   |
health fails
   |
current -> previous release
   |
systemctl restart educue
```

### Critical Flyway rule

Application rollback does **not** automatically undo database migrations.

Use backward-compatible **expand/contract** production migrations:

```text
1. Add new structures without removing old ones.
2. Deploy code compatible with old + new.
3. Backfill/migrate data.
4. Move consumers.
5. Remove obsolete structures in a later release.
```

## 26.2 Manual backend rollback

```bash
ls -lt /opt/educue/releases
```

```bash
PREVIOUS_SHA="REPLACE"

sudo ln -sfn \
  "/opt/educue/releases/$PREVIOUS_SHA" \
  /opt/educue/current

sudo systemctl restart educue
```

Verify:

```bash
curl \
  http://127.0.0.1:8080/actuator/health/readiness
```

## 26.3 Portal rollback

```bash
ls -lt /var/www/educue-portal/releases
```

```bash
PREVIOUS_SHA="REPLACE"

sudo ln -sfn \
  "/var/www/educue-portal/releases/$PREVIOUS_SHA" \
  /var/www/educue-portal/current
```

No Nginx restart normally required.

## 26.4 Home rollback

```bash
ls -lt /var/www/educue-home/releases
```

```bash
PREVIOUS_SHA="REPLACE"

sudo ln -sfn \
  "/var/www/educue-home/releases/$PREVIOUS_SHA" \
  /var/www/educue-home/current
```

---

# 27. Semantic Versioning and Git Tags

Keep Git SHA as the exact technical deployment identity.

The Maven project carries the human-readable application version. For the
first final release it is:

```xml
<version>1.0.0</version>
```

The Spring Boot build generates build metadata, so the running backend exposes
the same value at:

```bash
curl http://127.0.0.1:8080/actuator/info
```

Expected shape:

```json
{"build":{"artifact":"educue","name":"educue","time":"...","version":"1.0.0","group":"com.owuor"}}
```

Update the Maven version for each release and use the matching Git tag. The
tag includes the conventional `v`; the Maven value does not.

Use Semantic Versioning for humans:

```text
v1.0.0
v1.0.1
v1.1.0
v2.0.0
```

```text
MAJOR.MINOR.PATCH
```

Tag production:

```bash
git switch main
git pull origin main

git tag \
  -a v1.0.0 \
  -m "Educue production v1.0.0"

git push origin v1.0.0
```

A useful display label is:

```text
v1.1.0-a1b2c3d
```

The deployment scripts keep SHA-only directories for deterministic retries; Git tags provide the human-readable history.

---

# 28. Operational Commands

Backend status:

```bash
sudo systemctl status educue --no-pager
```

Backend logs:

```bash
sudo journalctl \
  -u educue \
  -n 200 \
  --no-pager
```

Live:

```bash
sudo journalctl -u educue -f
```

Restart backend:

```bash
sudo systemctl restart educue
```

Nginx:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Portal Nginx errors:

```bash
sudo tail -f \
  /var/log/nginx/educue-portal.error.log
```

Home errors:

```bash
sudo tail -f \
  /var/log/nginx/educue-home.error.log
```

Current releases:

```bash
readlink -f /opt/educue/current
readlink -f /var/www/educue-portal/current
readlink -f /var/www/educue-home/current
```

PostgreSQL:

```bash
sudo systemctl status postgresql --no-pager
sudo -u postgres psql
```

Disk:

```bash
df -h
du -sh /opt/educue/*
du -sh /var/www/educue-portal/*
du -sh /var/www/educue-home/*
```

Memory:

```bash
free -h
```

---

# 29. Troubleshooting

## 29.1 Backend CI `contextLoads` fails

If Spring tests require PostgreSQL, use the PostgreSQL 17 GitHub service container in the backend workflow.

Never point CI at production PostgreSQL.

## 29.2 Deployment says unhealthy although Spring starts

Correct health URL:

```text
http://127.0.0.1:8080/actuator/health/readiness
```

Not:

```text
/api/actuator/health/readiness
```

Check:

```bash
sudo journalctl \
  -u educue \
  -n 200 \
  --no-pager
```

## 29.3 Readiness returns `403`

Check Spring Security and the custom JWT filter.

Health must be permitted, and a custom filter must not reject tokenless public requests before the authorization rules.

## 29.4 Backend checksum cannot find JAR

The checksum must contain:

```text
<hash>  educue.jar
```

not:

```text
<hash>  deployment/educue.jar
```

The workflow therefore runs:

```bash
cd deployment
sha256sum educue.jar > educue.jar.sha256
```

## 29.5 Nginx `502`

```bash
sudo systemctl status educue --no-pager
sudo ss -lntp | grep 8080
curl http://127.0.0.1:8080/actuator/health/readiness
```

## 29.6 React route refresh returns 404

Portal Nginx must use:

```nginx
try_files $uri $uri/ /index.html;
```

## 29.7 Next build has no `out/`

Ensure:

```ts
output: "export"
```

Then:

```bash
npm run build
```

must create `out/`.

## 29.8 Next export fails `/opengraph-image`

For generated image:

```ts
export const dynamic = "force-static";
```

or use:

```text
app/opengraph-image.png
```

## 29.9 Next export fails `/robots.txt`

In `app/robots.ts`:

```ts
export const dynamic = "force-static";
```

## 29.10 Next export fails `/sitemap.xml`

In `app/sitemap.ts`:

```ts
export const dynamic = "force-static";
```

## 29.11 Certbot fails

```bash
sudo nginx -t

curl -I http://portal.ebadynamics.co.ke
curl -I http://home.ebadynamics.co.ke
```

Temporarily set Cloudflare records to DNS-only.

## 29.12 GitHub SSH fails

Confirm:

```text
PROD_HOST = VM public IP
PROD_USER = deploy
```

Check:

```bash
sudo ls -la /home/deploy/.ssh
sudo ls -ld /home/deploy/incoming
```

## 29.13 Sudo deployment denied

```bash
sudo visudo -c
sudo cat /etc/sudoers.d/educue-deploy
```

## 29.14 Deployment runs from feature

It must not.

Use:

```yaml
if: >
  (github.event_name == 'push' &&
   github.ref == 'refs/heads/main') ||
  (github.event_name == 'workflow_dispatch' &&
   github.ref == 'refs/heads/main')
```

---

# 30. Security Notes

## Backend secrets

Stay on VM:

```text
/opt/educue/config/educue.env
```

Permissions:

```text
root:educue
640
```

## Vite variables

`VITE_*` is browser-visible.

Never put:

```text
database passwords
JWT signing secrets
SMTP passwords
M-Pesa secrets
Cloudinary API secret
private keys
```

inside `VITE_*`.

## Next public values

`NEXT_PUBLIC_*` is browser-visible and must not contain secrets.

## Private listeners

Spring:

```text
127.0.0.1:8080
```

PostgreSQL:

```text
127.0.0.1:5432
```

Only Nginx should handle public application traffic.

## Deployment key

The private deploy key:

- stays on your trusted local machine and GitHub secret storage
- is not committed
- authenticates only `deploy`
- does not provide unrestricted sudo

## Deployment scripts

Keep:

```text
root:root
755
```

for:

```text
/usr/local/sbin/educue-deploy
/usr/local/sbin/educue-portal-deploy
/usr/local/sbin/educue-home-deploy
```

The `deploy` account must not own or edit them.

## Cloudflare

After origin TLS is valid:

```text
SSL/TLS mode: Full (strict)
```

---

# 31. Official Documentation

## Ubuntu

- Ubuntu 24.04 LTS:
  https://documentation.ubuntu.com/release-notes/24.04/
- Java setup:
  https://documentation.ubuntu.com/ubuntu-for-developers/howto/java-setup/
- Package management:
  https://documentation.ubuntu.com/server/how-to/software/package-management/

## PostgreSQL

- PostgreSQL Ubuntu packages:
  https://www.postgresql.org/download/linux/ubuntu/

## Nginx

- Reverse proxy:
  https://nginx.org/en/docs/http/ngx_http_proxy_module.html

## Certbot

- Certbot + Nginx:
  https://certbot.eff.org/instructions?os=snap&ws=nginx

## Cloudflare

- Full (strict):
  https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full-strict/

## Google Cloud IAP

- IAP TCP forwarding:
  https://cloud.google.com/iap/docs/using-tcp-forwarding

## GitHub Actions

- Deployments/environments:
  https://docs.github.com/actions/reference/workflows-and-actions/deployments-and-environments
- Secrets:
  https://docs.github.com/actions/concepts/security/secrets
- Workflow artifacts:
  https://docs.github.com/actions/concepts/workflows-and-actions/workflow-artifacts
- Checkout:
  https://github.com/actions/checkout
- Setup Java:
  https://github.com/actions/setup-java
- Setup Node:
  https://github.com/actions/setup-node
- Upload artifact:
  https://github.com/actions/upload-artifact
- Download artifact:
  https://github.com/actions/download-artifact

Current workflow major versions used:

```text
actions/checkout@v7
actions/setup-java@v5
actions/setup-node@v6
actions/upload-artifact@v7
actions/download-artifact@v8
```

## Vite

- Build:
  https://vite.dev/guide/build
- Environment variables:
  https://vite.dev/guide/env-and-mode

## Next.js

- Static exports:
  https://nextjs.org/docs/app/guides/static-exports
- Robots metadata:
  https://nextjs.org/docs/app/api-reference/file-conventions/metadata/robots
- Open Graph metadata:
  https://nextjs.org/docs/app/api-reference/file-conventions/metadata/opengraph-image

---

# Final Production State

```text
/opt/educue/
├── releases/
│   ├── <backend-sha>/
│   │   └── educue.jar
│   └── ...
├── current -> releases/<backend-sha>
├── config/
│   └── educue.env
├── storage/
├── logs/
└── scripts/

/var/www/educue-portal/
├── releases/
│   ├── <portal-sha>/
│   │   ├── index.html
│   │   └── assets/
│   └── ...
└── current -> releases/<portal-sha>

/var/www/educue-home/
├── releases/
│   ├── <home-sha>/
│   │   ├── index.html
│   │   ├── 404.html
│   │   └── _next/
│   └── ...
└── current -> releases/<home-sha>

/home/deploy/
└── incoming/

/etc/nginx/sites-available/
├── educue-portal
└── educue-home

/etc/systemd/system/
└── educue.service

/usr/local/sbin/
├── educue-deploy
├── educue-portal-deploy
└── educue-home-deploy
```

Request paths:

```text
https://portal.ebadynamics.co.ke
    -> Cloudflare
    -> Nginx
    -> React static files

https://portal.ebadynamics.co.ke/api/*
    -> Cloudflare
    -> Nginx
    -> Spring Boot 127.0.0.1:8080
    -> PostgreSQL 127.0.0.1:5432

https://home.ebadynamics.co.ke
    -> Cloudflare
    -> Nginx
    -> Next.js static export
```

---

**End of Educue Deployment Guide**
