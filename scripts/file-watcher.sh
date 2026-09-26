#!/bin/bash
set -u

BASE_DIR="/Users/prathams/Desktop/Bank_POC"
INCOMING_DIR="$BASE_DIR/incoming"
PROCESSING_DIR="$BASE_DIR/processing"
LOG_DIR="$BASE_DIR/logs"
LOG_FILE="$LOG_DIR/file-watcher.log"
POLL_INTERVAL=5
PID_FILE="$LOG_DIR/file-watcher.pid"
SCRIPTS_DIR="$BASE_DIR/scripts"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null; then
        echo "File watcher is already running with PID $PID"
        exit 1
    fi
fi

echo $$ > "$PID_FILE"

log() {
    local level=$1
    local msg=$2
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    echo "{\"timestamp\":\"$timestamp\",\"level\":\"$level\",\"component\":\"file-watcher\",\"message\":\"$msg\"}" >> "$LOG_FILE"
}

log "INFO" "Starting file watcher. Polling $INCOMING_DIR every $POLL_INTERVAL seconds."

process_file() {
    local file_path=$1
    local file_name=$(basename "$file_path")
    
    # Wait for file to stabilize
    local size1=$(stat -c%s "$file_path" 2>/dev/null || stat -f%z "$file_path" 2>/dev/null)
    sleep 2
    local size2=$(stat -c%s "$file_path" 2>/dev/null || stat -f%z "$file_path" 2>/dev/null)
    
    if [ "$size1" != "$size2" ]; then
        log "WARN" "File $file_name is still being written to. Will check next cycle."
        return
    fi
    
    # Validating filename format
    if [[ ! "$file_name" =~ ^BATCH-[0-9]{8}-[0-9]+(_PART[0-9]+of[0-9]+)?\.csv$ ]]; then
        log "WARN" "Invalid filename format: $file_name"
        mv "$file_path" "$BASE_DIR/failed/"
        echo "Invalid filename format" > "$BASE_DIR/failed/${file_name}.reason"
        return
    fi
    
    local checksum=$(shasum -a 256 "$file_path" | awk '{print $1}')
    log "INFO" "Checksum for $file_name: $checksum"
    
    local processing_path="$PROCESSING_DIR/$file_name"
    ln "$file_path" "$processing_path" 2>/dev/null || cp "$file_path" "$processing_path"
    rm -f "$file_path"
    
    log "INFO" "Moved $file_name to processing."
    
    if [ -x "$SCRIPTS_DIR/validate-and-route.sh" ]; then
        "$SCRIPTS_DIR/validate-and-route.sh" "$processing_path" "$checksum" &
    else
        log "ERROR" "validate-and-route.sh not found or not executable."
    fi
}

trap "rm -f $PID_FILE; exit" INT TERM EXIT

while true; do
    for file in "$INCOMING_DIR"/*.csv; do
        if [ -f "$file" ]; then
            process_file "$file"
        fi
    done
    sleep "$POLL_INTERVAL"
done
