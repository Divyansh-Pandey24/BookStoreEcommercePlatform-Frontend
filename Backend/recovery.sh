#!/bin/bash

# ==============================================================================
# BOOKNEST PRODUCTION "ONE-CLICK" RECOVERY SCRIPT
# This script restores the entire ecosystem (DBs, Books, and Services)
# ==============================================================================

echo "🚀 Starting BookNest Production Recovery..."

# 1. Ensure latest Production Config is present
echo "📥 Downloading latest production config..."
curl -O https://raw.githubusercontent.com/Divyansh-Pandey24/BookStoreEcommercePlatformBackend/main/Backend/docker-compose.prod.yml
curl -O https://raw.githubusercontent.com/Divyansh-Pandey24/BookStoreEcommercePlatformBackend/main/Backend/seed_200_books.sql

# 2. Cleanup existing "Zombie" containers
echo "🧹 Cleaning up old containers..."
sudo docker rm -f $(sudo docker ps -aq) 2>/dev/null || true

# 3. Start the Core Infrastructure (MySQL & Redis)
echo "🔋 Starting Infrastructure (MySQL, Redis)..."
sudo docker compose -f docker-compose.prod.yml up -d mysql redis-stack
echo "⏳ Waiting for MySQL to be healthy..."
sleep 20

# 4. Create all Microservice Databases
echo "🗄️ Creating Microservice Databases..."
sudo docker exec -i booknest-mysql mysql -u root -p1234 -e "
CREATE DATABASE IF NOT EXISTS booknest_auth;
CREATE DATABASE IF NOT EXISTS booknest_book;
CREATE DATABASE IF NOT EXISTS booknest_order;
CREATE DATABASE IF NOT EXISTS booknest_cart;
CREATE DATABASE IF NOT EXISTS booknest_wallet;
CREATE DATABASE IF NOT EXISTS booknest_notification;
CREATE DATABASE IF NOT EXISTS booknest_review;"

# 5. Create Table and Inject 200 Books
echo "📚 Seeding 200-Book Catalog..."
sudo docker exec -i booknest-mysql mysql -u root -p1234 booknest_book -e "CREATE TABLE IF NOT EXISTS books (book_id BIGINT PRIMARY KEY, title VARCHAR(255), author VARCHAR(255), description TEXT, price DOUBLE, stock INT, genre VARCHAR(255), featured BOOLEAN, cover_image_url VARCHAR(255));"
sudo docker exec -i booknest-mysql mysql -u root -p1234 booknest_book < seed_200_books.sql

# 6. Start the Microservices Engine
echo "⚙️ Starting Microservices..."
sudo docker compose -f docker-compose.prod.yml up -d

echo "✅ RECOVERY COMPLETE! Wait 60 seconds for everything to warm up."
