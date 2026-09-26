#!/bin/bash
set -euo pipefail

BASE_DIR="/Users/prathams/Desktop/Bank_POC"

echo "Setting up directories for Bank POC..."

mkdir -p "$BASE_DIR/incoming"
mkdir -p "$BASE_DIR/processing"
mkdir -p "$BASE_DIR/completed"
mkdir -p "$BASE_DIR/failed"
mkdir -p "$BASE_DIR/archive"
mkdir -p "$BASE_DIR/logs"
mkdir -p "$BASE_DIR/logs/payment-processor"
mkdir -p "$BASE_DIR/logs/npci-mock"

chmod 770 "$BASE_DIR/incoming"
chmod 770 "$BASE_DIR/processing"
chmod 770 "$BASE_DIR/completed"
chmod 770 "$BASE_DIR/failed"
chmod 770 "$BASE_DIR/archive"
chmod 770 "$BASE_DIR/logs"

ln -sf "$BASE_DIR/completed" "$BASE_DIR/latest-completed"

YEAR=$(date +%Y)
MONTH=$(date +%m)
DAY=$(date +%d)
mkdir -p "$BASE_DIR/archive/$YEAR/$MONTH/$DAY"

echo "Directory structure created successfully."
