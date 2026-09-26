#!/bin/bash
set -u

BASE_DIR="/Users/prathams/Desktop/Bank_POC"
FAILED_DIR="$BASE_DIR/failed"
PROCESSING_DIR="$BASE_DIR/processing"
LOG_DIR="$BASE_DIR/logs"
LOG_FILE="$LOG_DIR/validate-and-route.log"
CHECKSUM_FILE="$LOG_DIR/checksums.txt"
MAX_FILE_SIZE=$((10 * 1024 * 1024)) # 10MB

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

# 1. Duplicate check
if grep -q "$CHECKSUM" "$CHECKSUM_FILE"; then
    fail_file "Duplicate file detected based on checksum."
fi
echo "$CHECKSUM $FILE_NAME" >> "$CHECKSUM_FILE"

# 2. Size check
FILE_SIZE=$(stat -c%s "$FILE_PATH" 2>/dev/null || stat -f%z "$FILE_PATH")
if [ "$FILE_SIZE" -gt "$MAX_FILE_SIZE" ]; then
    fail_file "File size ($FILE_SIZE) exceeds maximum allowed ($MAX_FILE_SIZE)."
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

# 5. Route to API
if [[ "$FILE_NAME" =~ _PART([0-9]+)of([0-9]+)\.csv$ ]]; then
    PART_NUM="${BASH_REMATCH[1]}"
    TOTAL_PARTS="${BASH_REMATCH[2]}"
    BATCH_ID=$(echo "$FILE_NAME" | sed 's/_PART.*//')
    
    log "INFO" "Multi-part batch detected. Batch: $BATCH_ID, Part: $PART_NUM of $TOTAL_PARTS"
    
    # Check if all parts exist
    PARTS_FOUND=$(ls -1 "$PROCESSING_DIR/${BATCH_ID}"_PART*of${TOTAL_PARTS}.csv 2>/dev/null | wc -l | tr -d ' ')
    
    if [ "$PARTS_FOUND" -eq "$TOTAL_PARTS" ]; then
        log "INFO" "All parts found for batch $BATCH_ID. Calling API."
        
        FILE_PATHS_JSON="["
        for (( i=1; i<=$TOTAL_PARTS; i++ )); do
            PART_FILE_NAME="${BATCH_ID}_PART${i}of${TOTAL_PARTS}.csv"
            FILE_PATHS_JSON+="\"$PROCESSING_DIR/$PART_FILE_NAME\""
            if [ $i -lt $TOTAL_PARTS ]; then
                FILE_PATHS_JSON+=", "
            fi
        done
        FILE_PATHS_JSON+="]"

        curl -s -X POST "http://localhost:8080/api/v1/batches/ingest" \
             -H "Content-Type: application/json" \
             -d "{\"batchId\": \"$BATCH_ID\", \"filePaths\": $FILE_PATHS_JSON}" || log "WARN" "API call failed for $BATCH_ID"
    else
        log "INFO" "Waiting for remaining parts of batch $BATCH_ID ($PARTS_FOUND/$TOTAL_PARTS found)."
    fi
else
    BATCH_ID=$(echo "$FILE_NAME" | sed 's/\.csv//')
    log "INFO" "Single-part batch detected. Batch: $BATCH_ID. Calling API."
    # Call API here (mock)
    curl -s -X POST "http://localhost:8080/api/v1/batches/ingest" \
         -H "Content-Type: application/json" \
         -d "{\"batchId\": \"$BATCH_ID\", \"filePaths\": [\"$PROCESSING_DIR/$FILE_NAME\"]}" || log "WARN" "API call failed for $BATCH_ID"
fi
