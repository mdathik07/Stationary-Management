#!/bin/bash

# EC2 Setup Script for PrintXchange
# Run this once on a fresh EC2 instance (Ubuntu) to set up Docker and the
# deployment environment that the GitHub Actions CI/CD pipeline deploys into.

set -e

# Run every apt/dpkg step non-interactively so the script never blocks on a
# debconf prompt (e.g. "restart services?" or "keep local config?") when run
# over a non-interactive SSH session.
export DEBIAN_FRONTEND=noninteractive
export NEEDRESTART_MODE=a

echo "=========================================="
echo "Setting up EC2 for PrintXchange"
echo "=========================================="

# Update system
echo "Updating system packages..."
sudo -E apt-get update
sudo -E apt-get upgrade -y -o Dpkg::Options::="--force-confold"

# Install Docker
echo "Installing Docker..."
sudo -E apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Add Docker's official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker packages
sudo -E apt-get update
sudo -E apt-get install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-compose-plugin

# Add current user to docker group
sudo usermod -aG docker ${USER}

# Install monitoring tools
echo "Installing monitoring tools..."
sudo -E apt-get install -y htop curl wget

# Create deployment directory — this is where the CI/CD pipeline `cd`s into
echo "Creating deployment directory..."
mkdir -p ~/printxchange
cd ~/printxchange

# Create the production docker-compose.yaml (pulls the image from Docker Hub;
# it does NOT build from source on the server)
echo "Writing production docker-compose.yaml..."
cat > ~/printxchange/docker-compose.yaml << 'EOF'
services:

  mysql:
    image: mysql:8.0
    container_name: printxchange-mysql
    restart: unless-stopped

    environment:
      MYSQL_DATABASE: printxchange
      MYSQL_USER: printxchange
      MYSQL_PASSWORD: ${DB_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      TZ: UTC

    volumes:
      - printxchange-mysql-data:/var/lib/mysql

    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_PASSWORD}"]
      interval: 5s
      timeout: 5s
      retries: 10


  printxchange:
    image: ${DOCKERHUB_USERNAME}/printxchange:latest
    container_name: printxchange
    restart: unless-stopped

    depends_on:
      mysql:
        condition: service_healthy

    environment:
      DB_URL: "jdbc:mysql://mysql:3306/printxchange?useSSL=false&serverTimezone=UTC"
      DB_USERNAME: printxchange
      DB_PASSWORD: ${DB_PASSWORD}
      ADMIN_EMAIL: ${ADMIN_EMAIL}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD}
      ADMIN_NAME: ${ADMIN_NAME:-Shop Admin}

    ports:
      - "80:8081"


volumes:
  printxchange-mysql-data:
EOF

# Create environment file template
echo "Creating environment file template..."
cat > ~/printxchange/.env.template << 'EOF'
# Docker Hub account the CI/CD pipeline pushes images to
DOCKERHUB_USERNAME=your-dockerhub-username

# Database credentials
DB_PASSWORD=change_me_to_a_strong_password

# Seeded admin account
ADMIN_EMAIL=admin@yourcollege.edu
ADMIN_PASSWORD=change_me_to_a_strong_password
ADMIN_NAME=Shop Admin
EOF

echo ""
echo "IMPORTANT: copy .env.template to .env and fill in real values:"
echo "  cp ~/printxchange/.env.template ~/printxchange/.env"
echo "  nano ~/printxchange/.env"

# Set up log rotation
echo "Setting up log rotation..."
sudo mkdir -p /opt/logs
sudo chown -R ${USER}:${USER} /opt/logs
cat | sudo tee /etc/logrotate.d/printxchange << 'EOF'
/opt/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    notifempty
    create 0640 1000 1000
    sharedscripts
    postrotate
        docker kill -s HUP printxchange 2>/dev/null || true
    endscript
}
EOF

# Set up automatic security updates (installed with sane non-interactive
# defaults; skip dpkg-reconfigure, which opens an interactive debconf prompt
# that would hang a non-interactive SSH session)
echo "Setting up automatic security updates..."
sudo -E apt-get install -y unattended-upgrades

# Enable Docker service
echo "Enabling Docker service..."
sudo systemctl enable docker
sudo systemctl start docker

# Verify installation
echo "Verifying installations..."
docker --version
docker compose version

echo ""
echo "=========================================="
echo "✓ EC2 setup completed successfully!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Log out and back in (or run 'newgrp docker') so the docker group applies"
echo "2. Fill in ~/printxchange/.env from the template (see above)"
echo "3. Load env vars and start the stack once manually:"
echo "     cd ~/printxchange && set -a && source .env && set +a"
echo "     docker compose up -d"
echo "4. Point the EC2_HOST / EC2_USERNAME / EC2_SSH_PRIVATE_KEY GitHub secrets"
echo "   at this instance so the CI/CD pipeline can deploy to it"
echo "5. Monitor logs:"
echo "   docker logs -f printxchange"
echo ""
