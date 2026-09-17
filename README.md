# 📚 PrintXchange – College Stationery Shop Management System

**PrintXchange** is a web platform for college stationery shops. Students upload
PDFs to order prints (with the price calculated automatically from the page
count), track their order from *Pending* to *Ready for Pickup*, and buy/sell
used items with other students. The shop admin gets a live print queue with
one-click status updates and a daily revenue dashboard.

## ✨ Features

**For students**
- Place print orders: PDF upload, B&W/Color, copies, double-sided, notes to the shop
- Automatic pricing from the PDF's page count — the exact amount is shown before pickup
- Pickup code (`ORD-XXXXXXXX`) to show at the counter
- Track order status; cancel while still pending
- Exchange marketplace: post items with photo & price, search/filter, contact sellers
- Manage your own listings: mark sold, remove, see interest counts

**For the shop admin**
- Work queue: active orders oldest-first, filter by status, find by pickup code
- One-click status advance (Pending → Printing → Ready → Picked Up)
- In-browser PDF preview and download
- Dashboard: pending/printing/ready counts, today's orders and revenue

**Security**
- Public registration creates student accounts only; the admin account is seeded
  from environment variables
- BCrypt passwords, CSRF protection, per-role route protection, upload validation
- Optional college-email-domain restriction for registration

## 🛠 Tech Stack

Spring Boot 3.5 (Java 17) · Spring Security · Spring Data JPA · Thymeleaf ·
MySQL (H2 for local dev) · PDFBox · Maven · Docker

Uploaded files are stored **in the database**, so the app runs fine on hosts
with ephemeral filesystems (Render, Fly.io, …).

## 🚀 Run locally (no MySQL needed)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Open http://localhost:8081. A default admin is seeded:
`admin@printxchange.local` / `changeme123` (change via env vars below).

## ⚙️ Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/printxchange…` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / empty | DB credentials |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@printxchange.local` / `changeme123` | Seeded admin login |
| `ALLOWED_EMAIL_DOMAIN` | empty (any) | Restrict registration, e.g. `mycollege.edu` |
| `PRICE_BW_PER_PAGE` / `PRICE_COLOR_PER_PAGE` | `2.00` / `10.00` | Print rates (₹) |
| `PORT` | `8081` | HTTP port |

## 🐳 Docker

```bash
docker build -t printxchange .
docker run -p 8081:8081 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... printxchange
```

## ☁️ Free deployment (Render + Aiven)

1. **Database:** create a free MySQL service on [Aiven](https://aiven.io) and note
   the host/port/user/password (`defaultdb` database).
2. **App:** create a free Web Service on [Render](https://render.com) from this
   GitHub repo. Runtime: Docker. Set the env vars above —
   `DB_URL=jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED`, plus a strong
   `ADMIN_PASSWORD`.
3. Health check path: `/actuator/health`.

Note: Render's free tier sleeps after ~15 minutes of inactivity; the first
request afterwards takes ~30–50 s.

## 🚢 CI/CD deployment to AWS (EC2 + Docker Hub)

Pushes to `main` run a GitHub Actions pipeline (`.github/workflows/ci.yml`)
that tests the app, builds/pushes a Docker image, then deploys it to an EC2
instance over SSH:

1. **Build and Test** — runs `./mvnw clean test` on every push and PR.
2. **Build and Push Docker Image** — builds the image from the `Dockerfile`
   and pushes `printxchange:<commit-sha>` and `printxchange:latest` to Docker Hub.
3. **Deploy to EC2** — SSHes into the EC2 instance, runs
   `docker compose pull printxchange && docker compose up -d printxchange`
   in `~/printxchange`, and polls the container's health check until it
   reports healthy.

### One-time EC2 setup

1. Launch an Ubuntu EC2 instance, open inbound ports `22` and `80` (or `8081`)
   in its security group.
2. Copy `scripts/setup-ec2.sh` to the instance and run it:
   ```bash
   scp scripts/setup-ec2.sh ubuntu@<EC2_HOST>:~
   ssh ubuntu@<EC2_HOST> "chmod +x setup-ec2.sh && ./setup-ec2.sh"
   ```
   This installs Docker/Docker Compose, writes a production
   `~/printxchange/docker-compose.yaml` (MySQL + app, image pulled from Docker
   Hub), and creates `~/printxchange/.env.template`.
3. On the instance, fill in real values and start the stack once manually:
   ```bash
   cp ~/printxchange/.env.template ~/printxchange/.env
   nano ~/printxchange/.env
   cd ~/printxchange && docker compose up -d
   ```

### GitHub repository secrets

| Secret | Purpose |
|---|---|
| `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` | Push images to Docker Hub |
| `EC2_HOST` | Public IP/DNS of the EC2 instance |
| `EC2_USERNAME` | SSH user (e.g. `ubuntu`) |
| `EC2_SSH_PRIVATE_KEY` | Private key matching the instance's key pair |

Once these are set, every push to `main` automatically redeploys the app.
