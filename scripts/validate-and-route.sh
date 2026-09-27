#!/bin/bash
set -u

BASE_DIR="/Users/prathams/Desktop/Bank_POC"
FAILED_DIR="$BASE_DIR/failed"
PROCESSING_DIR="$BASE_DIR/processing"
COMPLETED_DIR="$BASE_DIR/completed"
LOG_DIR="$BASE_DIR/logs"
LOG_FILE="$LOG_DIR/validate-and-route.log"
CHECKSUM_FILE="$LOG_DIR/checksums.txt"

# Absolute maximum size before we outright reject it (e.g. 50MB)
MAX_FILE_SIZE=$((50 * 1024 * 1024))
# Limit configured by the bank for a single batch payload
MAX_ROWS_PER_PART=300

FILE_PATH=$1
CHECKSUM=${2:-$(shasum -a 256 "$FILE_PATH" | awk '{print $1}')}
FILE_NAME=$(basename "$FILE_PATH")

touch "$CHECKSUM_FILE"

log() {
    local level=$1
    local msg=$2
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    echo "{\"timestamp\":\"$timestamp\",\"level\":\"$level\",\"component\":\"validate-and-route\",\"message\":\"$msg\"}" >> "$LOG_FILE"
}

fail_file() {
    local reason=$1
    log "ERROR" "Validation failed for $FILE_NAME: $reason"
    mv "$FILE_PATH" "$FAILED_DIR/"
    echo "$reason" > "$FAILED_DIR/${FILE_NAME}.reason"
    ln -sf "$FAILED_DIR/$FILE_NAME" "$FAILED_DIR/link_$FILE_NAME"
    exit 1
}

# Reject files that look like they were manually split by the user
if [[ "$FILE_NAME" =~ _PART ]]; then
    fail_file "Manual multi-part uploads are not supported. Please upload the full single CSV file."
fi

# 1. Duplicate check
if grep -q "$CHECKSUM" "$CHECKSUM_FILE"; then
    fail_file "Duplicate file detected based on checksum."
fi
echo "$CHECKSUM $FILE_NAME" >> "$CHECKSUM_FILE"

# 2. Size check
FILE_SIZE=$(stat -c%s "$FILE_PATH" 2>/dev/null || stat -f%z "$FILE_PATH")
if [ "$FILE_SIZE" -gt "$MAX_FILE_SIZE" ]; then
    fail_file "File size ($FILE_SIZE bytes) exceeds absolute maximum allowed ($MAX_FILE_SIZE bytes)."
fi

# 3. CSV Structure check (Header check)
HEADER=$(head -n 1 "$FILE_PATH" | tr -d '\r')
EXPECTED_HEADER="transaction_id,sender_account,sender_ifsc,sender_name,receiver_account,receiver_ifsc,receiver_name,amount,payment_mode,narration"
if [ "$HEADER" != "$EXPECTED_HEADER" ]; then
    fail_file "Invalid CSV header."
fi

# 4. Column count check (sample first data row)
FIRST_DATA_ROW=$(sed '2q;d' "$FILE_PATH")
if [ -n "$FIRST_DATA_ROW" ]; then
    COL_COUNT=$(echo "$FIRST_DATA_ROW" | awk -F, '{print NF}')
    if [ "$COL_COUNT" -ne 10 ]; then
        fail_file "Invalid column count. Expected 10, got $COL_COUNT."
    fi
fi

# 5. Auto-Splitter Logic
TOTAL_LINES=$(wc -l < "$FILE_PATH" | tr -d ' ')
DATA_LINES=$((TOTAL_LINES - 1))
BATCH_ID=$(echo "$FILE_NAME" | sed 's/\.csv//')

if [ "$DATA_LINES" -gt "$MAX_ROWS_PER_PART" ]; then
    TOTAL_PARTS=$(( (DATA_LINES + MAX_ROWS_PER_PART - 1) / MAX_ROWS_PER_PART ))
    log "INFO" "File $FILE_NAME has $DATA_LINES rows. Auto-splitting into $TOTAL_PARTS parts (Max $MAX_ROWS_PER_PART per part)."
    
    # Extract data without header
    tail -n +2 "$FILE_PATH" > "$PROCESSING_DIR/temp_data.csv"
    
    # Split into chunks
    split -l "$MAX_ROWS_PER_PART" "$PROCESSING_DIR/temp_data.csv" "$PROCESSING_DIR/temp_chunk_"
    
    PART=1
    FILE_PATHS_JSON="["
    
    # Loop through generated chunks in alphabetical order
    for CHUNK in $(ls "$PROCESSING_DIR"/temp_chunk_* | sort); do
        PART_FILE="$PROCESSING_DIR/${BATCH_ID}_PART${PART}of${TOTAL_PARTS}.csv"
        echo "$HEADER" > "$PART_FILE"
        cat "$CHUNK" >> "$PART_FILE"
        rm "$CHUNK"
        
        FILE_PATHS_JSON+="\"$PART_FILE\""
        if [ $PART -lt $TOTAL_PARTS ]; then
            FILE_PATHS_JSON+=", "
        fi
        PART=$((PART + 1))
    done
    FILE_PATHS_JSON+="]"
    rm "$PROCESSING_DIR/temp_data.csv"
    
    # Call Java API with the split parts
    log "INFO" "Routing multi-part batch $BATCH_ID to API."
    curl -s -X POST "http://localhost:8080/api/v1/batches/ingest" \
         -H "Content-Type: application/json" \
         -d "{\"batchId\": \"$BATCH_ID\", \"filePaths\": $FILE_PATHS_JSON}" || log "WARN" "API call failed for $BATCH_ID"
         
    # Move original massive file out of processing queue
    mv "$FILE_PATH" "$COMPLETED_DIR/${FILE_NAME}.original"
else
    log "INFO" "File $FILE_NAME has $DATA_LINES rows (Under limit of $MAX_ROWS_PER_PART). Routing directly to API."
    
    FILE_PATHS_JSON="[\"$PROCESSING_DIR/$FILE_NAME\"]"
    curl -s -X POST "http://localhost:8080/api/v1/batches/ingest" \
         -H "Content-Type: application/json" \
         -d "{\"batchId\": \"$BATCH_ID\", \"filePaths\": $FILE_PATHS_JSON}" || log "WARN" "API call failed for $BATCH_ID"
fi
