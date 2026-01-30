#!/bin/bash

# PRoot & Alpine Rootfs Download Script for Astral Node.js Runtime
# This script downloads PRoot binary and Alpine Linux rootfs

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ASSETS_DIR="$SCRIPT_DIR"

echo "================================"
echo "Astral Node.js Runtime Setup"
echo "================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# PRoot version and URLs
PROOT_VERSION="5.3.1"
PROOT_ARM64_URL="https://raw.githubusercontent.com/foxytouxxx/freeroot/main/proot-aarch64"

# Rootfs version (Ubuntu Focal 20.04 - KAIST Mirror)
UBUNTU_URL="http://ftp.kaist.ac.kr/ubuntu-cdimage/ubuntu-base/releases/20.04/release/ubuntu-base-20.04.5-base-arm64.tar.gz"
ALPINE_URL=$UBUNTU_URL # Keep variable name for compatibility if needed elsewhere

echo -e "${YELLOW}[1/4] Downloading PRoot binary...${NC}"
if [ -f "$ASSETS_DIR/proot-arm64" ]; then
    echo -e "${GREEN}✓ PRoot binary already exists${NC}"
else
    wget -O "$ASSETS_DIR/proot-arm64" "$PROOT_ARM64_URL" || {
        echo -e "${RED}✗ Failed to download PRoot binary${NC}"
        echo "Please download manually from: https://github.com/termux/proot/releases"
        exit 1
    }
    chmod +x "$ASSETS_DIR/proot-arm64"
    echo -e "${GREEN}✓ PRoot binary downloaded${NC}"
fi

echo ""
echo -e "${YELLOW}[2/4] Downloading Alpine Linux rootfs...${NC}"
if [ -f "$ASSETS_DIR/alpine-minirootfs.tar.gz" ]; then
    echo -e "${GREEN}✓ Alpine rootfs already downloaded${NC}"
else
    wget -O "$ASSETS_DIR/alpine-minirootfs.tar.gz" "$ALPINE_URL" || {
        echo -e "${RED}✗ Failed to download Alpine rootfs${NC}"
        echo "Please download manually from: https://alpinelinux.org/downloads/"
        exit 1
    }
    echo -e "${GREEN}✓ Alpine rootfs downloaded${NC}"
fi

echo ""
echo -e "${YELLOW}[3/4] Extracting and recompressing to tar.xz...${NC}"
if [ -f "$ASSETS_DIR/alpine-rootfs.tar.xz" ]; then
    echo -e "${GREEN}✓ alpine-rootfs.tar.xz already exists${NC}"
else
    TMP_DIR=$(mktemp -d)
    echo "Extracting to temp dir: $TMP_DIR"
    
    tar -xzf "$ASSETS_DIR/alpine-minirootfs.tar.gz" -C "$TMP_DIR"
    
    echo "Recompressing with xz..."
    tar -cJf "$ASSETS_DIR/alpine-rootfs.tar.xz" -C "$TMP_DIR" .
    
    rm -rf "$TMP_DIR"
    echo -e "${GREEN}✓ Rootfs recompressed to tar.xz${NC}"
fi

echo ""
echo -e "${YELLOW}[4/4] Cleanup...${NC}"
if [ -f "$ASSETS_DIR/alpine-minirootfs.tar.gz" ]; then
    rm "$ASSETS_DIR/alpine-minirootfs.tar.gz"
    echo -e "${GREEN}✓ Removed temporary tar.gz file${NC}"
fi

echo ""
echo "================================"
echo -e "${GREEN}Setup Complete!${NC}"
echo "================================"
echo ""
echo "Files created:"
echo "  - proot-arm64 ($(du -h "$ASSETS_DIR/proot-arm64" | cut -f1))"
echo "  - alpine-rootfs.tar.xz ($(du -h "$ASSETS_DIR/alpine-rootfs.tar.xz" | cut -f1))"
echo ""
echo "You can now build the APK with:"
echo "  cd ../../../../../../.."
echo "  ./gradlew assembleDebug"
echo ""
