#!/bin/bash
set -u

BASE_DIR="/Users/prathams/Desktop/Bank_POC"
COMPLETED_DIR="$BASE_DIR/completed"
PROCESSING_DIR="$BASE_DIR/processing"
ARCHIVE_DIR="$BASE_DIR/archive"
LOG_FILE="$BASE_DIR/logs/cleanup.log"

log() {
    local level=$1
    local msg=$2
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    echo "{\"timestamp\":\"$timestamp\",\"level\":\"$level\",\"component\":\"cleanup\",\"message\":\"$msg\"}" >> "$LOG_FILE"
}

log "INFO" "Starting cleanup job"

# Archive completed batches older than 24 hours
if [ "$(uname)" = "Darwin" ]; then
    # macOS find uses different flags
    COMPLETED_FILES=$(find "$COMPLETED_DIR" -type f -mtime +1 -name "*.csv")
else
    COMPLETED_FILES=$(find "$COMPLETED_DIR" -type f -mtime +1 -name "*.csv")
fi

if [ -n "$COMPLETED_FILES" ]; then
    for file in $COMPLETED_FILES; do
        FILE_DATE=$(date -r "$file" +"%Y/%m/%d" 2>/dev/null || date -d "@$(stat -c %Y "$file")" +"%Y/%m/%d")
        DEST_DIR="$ARCHIVE_DIR/$FILE_DATE"
        mkdir -p "$DEST_DIR"
        mv "$file" "$DEST_DIR/"
        log "INFO" "Archived $file to $DEST_DIR"
    done
else
    log "INFO" "No completed files to archive."
fi

# Clean up stale processing files older than 48 hours
if [ "$(uname)" = "Darwin" ]; then
    STALE_FILES=$(find "$PROCESSING_DIR" -type f -mtime +2 -name "*.csv")
else
    STALE_FILES=$(find "$PROCESSING_DIR" -type f -mtime +2 -name "*.csv")
fi

if [ -n "$STALE_FILES" ]; then
    for file in $STALE_FILES; do
        rm -f "$file"
        log "WARN" "Removed stale processing file: $file"
    done
else
    log "INFO" "No stale files to remove."
fi

# Remove empty directories in archive
find "$ARCHIVE_DIR" -type d -empty -delete 2>/dev/null || true

log "INFO" "Cleanup job finished"
