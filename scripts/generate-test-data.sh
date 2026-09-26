#!/bin/bash
set -e

# Mock data script for testing

BASE_DIR="/Users/prathams/Desktop/Bank_POC"
mkdir -p "$BASE_DIR/incoming"

SCENARIO=${1:-happy-path}
DATE=$(date +%Y%m%d)

echo "Generating test data for scenario: $SCENARIO"

HEADER="transaction_id,sender_account,sender_ifsc,sender_name,receiver_account,receiver_ifsc,receiver_name,amount,payment_mode,narration"

case "$SCENARIO" in
    happy-path)
        FILE="$BASE_DIR/incoming/BATCH-${DATE}-001.csv"
        echo "$HEADER" > "$FILE"
        for i in {1..50}; do
            printf "TXN-%s-001-%04d,1234567890,HDFC0001234,John Doe,0987654321,ICIC0000123,Jane Smith,%d.00,NEFT,Salary Disbursement\n" "$DATE" "$i" "$((500 + i * 10))" >> "$FILE"
        done
        echo "Created $FILE"
        ;;
    multi-part)
        for part in {1..3}; do
            FILE="$BASE_DIR/incoming/BATCH-${DATE}-002_PART${part}of3.csv"
            echo "$HEADER" > "$FILE"
            for i in {1..100}; do
                printf "TXN-%s-002-%04d,1234567890,SBIN0001234,Test User,0987654321,UTIB0000123,Another User,%d.00,IMPS,Transfer\n" "$DATE" "$(( (part-1)*100 + i ))" "$((1000 + i))" >> "$FILE"
            done
            echo "Created $FILE"
        done
        ;;
    oversized)
        FILE="$BASE_DIR/incoming/BATCH-${DATE}-003_oversized.csv"
        echo "$HEADER" > "$FILE"
        # Generate a large file (mocked as large)
        for i in {1..800}; do
            printf "TXN-%s-003-%04d,1234567890,HDFC0001234,John Doe,0987654321,ICIC0000123,Jane Smith,%d.00,NEFT,Large File Test\n" "$DATE" "$i" "100" >> "$FILE"
        done
        echo "Created $FILE (Needs MAX_FILE_SIZE config adjustment to fail)"
        ;;
    invalid-accounts)
        FILE="$BASE_DIR/incoming/BATCH-${DATE}-005.csv"
        echo "$HEADER" > "$FILE"
        printf "TXN-%s-005-0001,123,HDFC0001234,Short Acc,0987654321,ICIC0000123,Valid Recv,500.00,NEFT,Test\n" "$DATE" >> "$FILE"
        printf "TXN-%s-005-0002,1234567890,INVALIDIFSC,Valid Acc,0987654321,ICIC0000123,Valid Recv,500.00,NEFT,Test\n" "$DATE" >> "$FILE"
        echo "Created $FILE with invalid data"
        ;;
    *)
        echo "Unknown scenario: $SCENARIO"
        echo "Available: happy-path, multi-part, oversized, invalid-accounts"
        exit 1
        ;;
esac

echo "Done."
