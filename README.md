# Bank Payment Processing POC — V1

## Overview

An on-premise domestic bulk-payment processing proof-of-concept demonstrating an end-to-end payment pipeline suitable for enterprise banking environments. Built to showcase how an enterprise technology company's platform can ingest, process, and monitor large-scale payment files while providing AI-ready structured data for downstream analysis.

### Architecture Flow

```
CSV Upload → SFTP → /incoming → Bash Scripts → Spring Boot Payment Processor
    → PostgreSQL → NPCI Mock Service → Structured Logs
    → Fluent Bit → Loki → Grafana → Company AI Engine
```

### Key Components

| Component              | Technology                  | Port  |
|------------------------|----------------------------|-------|
| Payment Processor      | Java 17 + Spring Boot 3.1  | 8080  |
| NPCI Mock Service      | Java 17 + Spring Boot 3.1  | 8081  |
| Database               | PostgreSQL 15+             | 5432  |
| Log Collector          | Fluent Bit                 | —     |
| Log Aggregator         | Loki                       | 3100  |
| Dashboard              | Grafana                    | 3000  |

---

## Project Structure

```
Bank_POC/
├── payment-processor/          # Core Spring Boot payment processing app
│   ├── pom.xml
│   └── src/main/java/com/bankpoc/processor/
│       ├── config/             # App, async, JPA configuration
│       ├── controller/         # REST APIs (Batch, Transaction, AI, Health)
│       ├── dto/                # Data transfer objects
│       ├── exception/          # Exception handling
│       ├── model/entity/       # JPA entities
│       ├── model/enums/        # Status enums
│       ├── repository/         # Spring Data JPA repositories
│       ├── service/            # Business logic & orchestration
│       └── util/               # Checksum, idempotency, trace utilities
├── npci-mock-service/          # NPCI payment gateway simulator
│   ├── pom.xml
│   └── src/main/java/com/bankpoc/npcimock/
│       ├── config/             # Simulation configuration
│       ├── controller/         # Payment & simulation APIs
│       ├── exception/          # Error handling
│       ├── model/              # Request/response models
│       └── service/            # Simulation engine
├── scripts/                    # Bash scripts for file operations
│   ├── setup-directories.sh    # Creates directory structure
│   ├── file-watcher.sh         # Monitors /incoming for new files
│   ├── validate-and-route.sh   # Validates and routes files
│   ├── cleanup.sh              # Archives old batches
│   └── generate-test-data.sh   # Generates test CSV files
├── sql/
│   └── schema.sql              # PostgreSQL schema
├── sample-data/
│   ├── good/                   # Valid payment batches
│   └── problematic/            # Files designed to trigger failures
├── config/
│   ├── fluent-bit/             # Fluent Bit log collection config
│   ├── loki/                   # Loki log aggregation config
│   └── grafana/                # Grafana dashboards & datasources
└── README.md
```

---

## Prerequisites

- **Java 17** (JDK)
- **Maven 3.8+**
- **PostgreSQL 15+**
- **Fluent Bit** (optional, for log pipeline)
- **Loki** (optional, for log aggregation)
- **Grafana** (optional, for dashboards)

---

## Quick Start

### 1. Database Setup

```bash
# Connect to PostgreSQL
sudo -u postgres psql

# Create database and user
CREATE DATABASE bankpoc;
CREATE USER bankpoc_user WITH PASSWORD 'bankpoc_pass';
GRANT ALL PRIVILEGES ON DATABASE bankpoc TO bankpoc_user;
\c bankpoc
GRANT ALL ON SCHEMA public TO bankpoc_user;
\q

# Run the schema
psql -U bankpoc_user -d bankpoc -f sql/schema.sql
```

### 2. Setup Directories

```bash
chmod +x scripts/*.sh
sudo ./scripts/setup-directories.sh
```

This creates `/incoming`, `/processing`, `/completed`, `/failed`, `/archive`, and `/logs` directories with appropriate permissions. It also creates:
- **Hard link demonstration**: File safety during processing moves
- **Soft link**: `/latest-completed` → `/completed` for quick access

### 3. Build & Run NPCI Mock Service

```bash
cd npci-mock-service
mvn clean package -DskipTests
java -jar target/npci-mock-service-0.0.1-SNAPSHOT.jar
```

The NPCI mock starts on **port 8081**. Verify: `curl http://localhost:8081/api/npci/v1/health`

### 4. Build & Run Payment Processor

```bash
cd payment-processor
mvn clean package -DskipTests
java -jar target/payment-processor-0.0.1-SNAPSHOT.jar
```

The payment processor starts on **port 8080**. Verify: `curl http://localhost:8080/api/v1/health/`

### 5. Start File Watcher

```bash
./scripts/file-watcher.sh &
```

### 6. Process a Batch

#### Option A: Copy sample files to /incoming (simulates SFTP upload)

```bash
# Single-file batch
cp sample-data/good/BATCH-20260926-002.csv /incoming/

# Multi-part batch (all 3 parts)
cp sample-data/good/BATCH-20260926-001_PART1of3.csv /incoming/
cp sample-data/good/BATCH-20260926-001_PART2of3.csv /incoming/
cp sample-data/good/BATCH-20260926-001_PART3of3.csv /incoming/
```

#### Option B: Direct API call

```bash
curl -X POST http://localhost:8080/api/v1/batches/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH-20260926-002",
    "filePaths": ["/processing/BATCH-20260926-002.csv"]
  }'
```

### 7. Monitor Progress

```bash
# Check batch status
curl http://localhost:8080/api/v1/batches/BATCH-20260926-002

# List all batches
curl http://localhost:8080/api/v1/batches/

# View transactions for a batch
curl http://localhost:8080/api/v1/batches/BATCH-20260926-002/transactions
```

---

## API Reference

### Payment Processor APIs (Port 8080)

#### Batch Management

| Method | Endpoint                                 | Description                    |
|--------|------------------------------------------|--------------------------------|
| POST   | `/api/v1/batches/ingest`                 | Ingest and process a batch     |
| GET    | `/api/v1/batches/{batchId}`              | Get batch status               |
| GET    | `/api/v1/batches/`                       | List all batches               |
| GET    | `/api/v1/batches/{batchId}/transactions` | List transactions in a batch   |

#### Transaction Management

| Method | Endpoint                              | Description              |
|--------|---------------------------------------|--------------------------|
| GET    | `/api/v1/transactions/{transactionId}`| Get transaction status   |

#### AI Integration (Company AI Engine Interface)

| Method | Endpoint                                | Description                                  |
|--------|-----------------------------------------|----------------------------------------------|
| GET    | `/api/v1/ai/batches/summary?from=&to=`  | Batch summaries with failure breakdowns      |
| GET    | `/api/v1/ai/batches/{batchId}/analysis`  | Deep batch analysis for AI consumption       |
| GET    | `/api/v1/ai/failures/patterns`           | Aggregated failure pattern data              |
| GET    | `/api/v1/ai/failures/details?batchId=`   | Detailed per-transaction failure info        |
| GET    | `/api/v1/ai/system/context`              | System health and processing metadata        |
| GET    | `/api/v1/ai/export/batch/{batchId}`      | Complete batch export for LLM context window |

#### Health

| Method | Endpoint               | Description       |
|--------|------------------------|-------------------|
| GET    | `/api/v1/health/`      | System health     |

### NPCI Mock Service APIs (Port 8081)

#### Payment Processing

| Method | Endpoint                        | Description          |
|--------|---------------------------------|----------------------|
| POST   | `/api/npci/v1/payments/process` | Process a payment    |
| GET    | `/api/npci/v1/health`           | Mock service health  |

#### Failure Simulation

| Method | Endpoint                              | Description                    |
|--------|---------------------------------------|--------------------------------|
| GET    | `/api/npci/v1/simulation/config`      | View current failure config    |
| PUT    | `/api/npci/v1/simulation/config`      | Update failure configuration   |
| POST   | `/api/npci/v1/simulation/reset`       | Reset to defaults              |
| POST   | `/api/npci/v1/simulation/chaos`       | Enable chaos mode (30% fail)   |
| POST   | `/api/npci/v1/simulation/unavailable` | Make NPCI return 503 for all   |
| POST   | `/api/npci/v1/simulation/recover`     | Disable all failure injection  |
| GET    | `/api/npci/v1/simulation/stats`       | View simulation statistics     |

---

## Failure Injection & Testing

### Built-in Failure Scenarios

#### 1. Oversized File
```bash
cp sample-data/problematic/BATCH-20260926-003_oversized.csv /incoming/
# → Rejected by validate-and-route.sh, moved to /failed with reason file
```

#### 2. Missing File Part
```bash
cp sample-data/problematic/BATCH-20260926-004_PART1of2.csv /incoming/
# → Waits indefinitely for PART2; never assembled
```

#### 3. Invalid Account Data
```bash
# Copy to /processing and ingest directly
cp sample-data/problematic/BATCH-20260926-005.csv /processing/
curl -X POST http://localhost:8080/api/v1/batches/ingest \
  -H "Content-Type: application/json" \
  -d '{"batchId":"BATCH-20260926-005","filePaths":["/processing/BATCH-20260926-005.csv"]}'
# → Some transactions fail with VALIDATION category
```

#### 4. Duplicate Transactions
```bash
cp sample-data/problematic/BATCH-20260926-006.csv /processing/
curl -X POST http://localhost:8080/api/v1/batches/ingest \
  -H "Content-Type: application/json" \
  -d '{"batchId":"BATCH-20260926-006","filePaths":["/processing/BATCH-20260926-006.csv"]}'
# → Duplicates detected via idempotency key
```

#### 5. NPCI Failures (Account Not Found, Timeout, etc.)
```bash
cp sample-data/problematic/BATCH-20260926-007.csv /processing/
curl -X POST http://localhost:8080/api/v1/batches/ingest \
  -H "Content-Type: application/json" \
  -d '{"batchId":"BATCH-20260926-007","filePaths":["/processing/BATCH-20260926-007.csv"]}'
# → Transactions with specific account/amount patterns trigger NPCI failures
```

#### 6. Enable Chaos Mode (Random 30% Failures)
```bash
curl -X POST http://localhost:8081/api/npci/v1/simulation/chaos
# Process any batch — ~30% of transactions will randomly fail
```

#### 7. NPCI Service Unavailable
```bash
curl -X POST http://localhost:8081/api/npci/v1/simulation/unavailable
# All NPCI calls return 503
# To recover:
curl -X POST http://localhost:8081/api/npci/v1/simulation/recover
```

#### 8. Custom Account-Level Failure
```bash
curl -X POST http://localhost:8081/api/npci/v1/simulation/account-failure \
  -H "Content-Type: application/json" \
  -d '{"account":"2000000001","reason":"ACCOUNT_CLOSED"}'
```

---

## Sample Data

### Good Batches

| File | Description |
|------|-------------|
| `good/BATCH-20260926-001_PART1of3.csv` | Part 1 of 3: 234 valid transactions |
| `good/BATCH-20260926-001_PART2of3.csv` | Part 2 of 3: 233 valid transactions |
| `good/BATCH-20260926-001_PART3of3.csv` | Part 3 of 3: 233 valid transactions |
| `good/BATCH-20260926-002.csv` | Single-file batch: 50 valid salary payments |

**Total: 700 transactions across the multi-part batch + 50 in single batch**

### Problematic Batches

| File | Failure Type |
|------|-------------|
| `problematic/BATCH-20260926-003_oversized.csv` | File exceeds size limit |
| `problematic/BATCH-20260926-004_PART1of2.csv` | Missing PART2 (incomplete batch) |
| `problematic/BATCH-20260926-005.csv` | Invalid accounts, negative amounts, bad IFSCs |
| `problematic/BATCH-20260926-006.csv` | Duplicate transaction IDs |
| `problematic/BATCH-20260926-007.csv` | Triggers NPCI-side failures (timeout, not found) |

### CSV Format

```csv
transaction_id,sender_account,sender_ifsc,sender_name,receiver_account,receiver_ifsc,receiver_name,amount,payment_mode,narration
TXN-20260926-001-0001,1000000001,HDFC0000001,Amit Kumar,2000000001,ICIC0000001,Rahul Singh,500.00,NEFT,Salary-Sep2026
```

---

## Logging & Observability

### Structured Logging

All logs use JSON format via Logback + Logstash Encoder, including MDC context:

```json
{
  "timestamp": "2026-09-26T22:00:00.000",
  "level": "INFO",
  "logger": "com.bankpoc.processor.service.PaymentOrchestrator",
  "message": "Starting processing for batch BATCH-20260926-001",
  "batchId": "BATCH-20260926-001",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "transactionId": "TXN-20260926-001-0042",
  "thread": "payment-executor-1"
}
```

### Log Locations

| Service            | Log File                                                   |
|--------------------|------------------------------------------------------------|
| Payment Processor  | `logs/payment-processor/application.log`                   |
| Payment Processor  | `logs/payment-processor/error.log` (errors only)           |
| NPCI Mock          | `logs/npci-mock/application.log`                           |
| File Watcher       | `/logs/file-watcher.log`                                   |

### Setting Up the Log Pipeline

#### Install & Configure Fluent Bit
```bash
# Install Fluent Bit (Ubuntu)
curl https://raw.githubusercontent.com/fluent/fluent-bit/master/install.sh | sh

# Copy configuration
sudo cp config/fluent-bit/fluent-bit.conf /etc/fluent-bit/
sudo cp config/fluent-bit/parsers.conf /etc/fluent-bit/

# Start Fluent Bit
sudo systemctl start fluent-bit
```

#### Install & Configure Loki
```bash
# Download Loki binary
curl -O -L "https://github.com/grafana/loki/releases/latest/download/loki-linux-amd64.zip"
unzip loki-linux-amd64.zip

# Start Loki with our config
./loki-linux-amd64 -config.file=config/loki/loki-config.yaml &
```

#### Install & Configure Grafana
```bash
# Install Grafana (Ubuntu)
sudo apt-get install -y grafana

# Copy provisioning configs
sudo cp -r config/grafana/provisioning/* /etc/grafana/provisioning/
sudo mkdir -p /var/lib/grafana/dashboards
sudo cp config/grafana/dashboards/*.json /var/lib/grafana/dashboards/

# Start Grafana
sudo systemctl start grafana-server
# Access: http://localhost:3000 (admin/admin)
```

### Grafana Dashboard

The pre-configured dashboard (`payment-processing-dashboard.json`) includes:

- **Batch Status Overview**: Total, processing, completed, failed counts
- **Transaction Success/Failure Rate**: Real-time pie chart
- **Failure Categories Breakdown**: VALIDATION, BUSINESS, INFRASTRUCTURE, TIMEOUT
- **Processing Time per Batch**: Performance monitoring
- **Recent Errors**: Live log stream from Loki
- **NPCI Response Codes**: Distribution chart

---

## AI Integration Interface

The system exposes structured data endpoints designed for easy consumption by an enterprise AI engine. These are NOT the AI engine itself — they provide the data interface.

### Example: Get Batch Analysis for AI

```bash
curl http://localhost:8080/api/v1/ai/batches/BATCH-20260926-001/analysis
```

Response:
```json
{
  "requestTimestamp": "2026-09-26T22:00:00",
  "dataFreshness": "REALTIME",
  "recordCount": 1,
  "data": {
    "batchId": "BATCH-20260926-001",
    "status": "PARTIAL_SUCCESS",
    "totalTransactions": 700,
    "successCount": 685,
    "failureCount": 15,
    "totalAmount": 87500000.00,
    "failureBreakdown": {
      "VALIDATION": 5,
      "BUSINESS": 7,
      "TIMEOUT": 3
    }
  }
}
```

### Example: Export Complete Batch for LLM Context

```bash
curl http://localhost:8080/api/v1/ai/export/batch/BATCH-20260926-001
```

This returns a single JSON blob containing the batch summary, all failure details, and processing timeline — designed to fit into an LLM context window for analysis.

---

## Design Decisions & Assumptions

### Architecture Choices

1. **Separate NPCI Mock Service**: Runs as an independent Spring Boot app to realistically simulate a remote payment gateway with network delays and failures.

2. **Idempotency**: Every transaction gets a deterministic idempotency key (SHA-256 hash of txnId + accounts + amount). Prevents double-payment on retry.

3. **Batch State Machine**: `PENDING → ASSEMBLING → VALIDATING → PROCESSING → COMPLETED/PARTIAL_SUCCESS/FAILED`. All transitions are audited.

4. **Error Classification**: Failures are categorized into VALIDATION, BUSINESS, INFRASTRUCTURE, TIMEOUT, and DUPLICATE — enabling targeted analysis and different recovery strategies.

5. **Hard Links**: Used when moving files from `/incoming` to `/processing` — creates a hard link first for data safety, then removes the original. If the move fails, the data still exists via the hard link.

6. **Soft Links**: `/latest-completed` symlink always points to the `/completed` directory for convenient access to recent results.

7. **No Docker/K8s**: All components run natively on the host OS as requested.

### Assumptions

- PostgreSQL is running locally on default port 5432
- The system processes payments sequentially within a batch (parallel processing can be added later)
- CSV files are expected in the documented format with headers
- File sizes are limited to 10MB per part (configurable)
- The NPCI mock introduces realistic delays (100-2000ms per transaction)
- All monetary amounts use `BigDecimal` to avoid floating-point precision issues
- `ddl-auto: validate` is used — schema must be created manually first

### Production Evolution Path

| V1 (Current)         | Production Target                       |
|----------------------|-----------------------------------------|
| NPCI Mock            | Real NPCI/UPI API integration           |
| Local PostgreSQL     | HA PostgreSQL cluster or Oracle         |
| Sequential processing| Parallel batch processing with partitioning |
| Bash file watcher    | Spring Integration or Apache Camel      |
| Basic retry          | Saga pattern with compensating transactions |
| JSON file logs       | Enterprise log aggregation (ELK/Splunk) |
| Mock AI interface    | Company's production AI engine          |
| No auth              | OAuth2/JWT + mTLS                       |

---

## Troubleshooting

### Payment Processor won't start
```bash
# Check PostgreSQL is running
pg_isready -h localhost -p 5432

# Check database exists
psql -U bankpoc_user -d bankpoc -c "SELECT 1;"

# Check schema was applied
psql -U bankpoc_user -d bankpoc -c "\dt"
```

### NPCI Mock not responding
```bash
curl http://localhost:8081/api/npci/v1/health
# If unavailable mode is on:
curl -X POST http://localhost:8081/api/npci/v1/simulation/recover
```

### Batch stuck in PROCESSING
```bash
# Check transaction states
curl http://localhost:8080/api/v1/batches/{batchId}/transactions

# Check logs
tail -f logs/payment-processor/application.log | jq .
```

### CSV parsing errors
- Ensure the CSV has the exact header row: `transaction_id,sender_account,sender_ifsc,sender_name,receiver_account,receiver_ifsc,receiver_name,amount,payment_mode,narration`
- Check for BOM characters in the file
- Ensure no trailing commas

### Viewing structured logs
```bash
# Pretty-print JSON logs
tail -f logs/payment-processor/application.log | jq .

# Filter by batch
tail -f logs/payment-processor/application.log | jq 'select(.batchId == "BATCH-20260926-001")'

# Filter errors only
tail -f logs/payment-processor/error.log | jq .
```

---

## Testing Checklist

- [ ] PostgreSQL schema created successfully
- [ ] NPCI Mock starts on port 8081
- [ ] Payment Processor starts on port 8080
- [ ] Single-file batch (BATCH-20260926-002) processes successfully
- [ ] Multi-part batch (BATCH-20260926-001) assembles and processes all 700 transactions
- [ ] Invalid account batch (BATCH-20260926-005) produces VALIDATION failures
- [ ] NPCI failure batch (BATCH-20260926-007) produces BUSINESS/TIMEOUT failures
- [ ] Chaos mode produces random failures across transactions
- [ ] AI endpoints return structured data
- [ ] Logs are written in JSON format
- [ ] Batch status transitions are recorded in audit_log table

---

## License

Internal POC — Not for production use without proper security hardening, compliance review, and real payment gateway integration.
