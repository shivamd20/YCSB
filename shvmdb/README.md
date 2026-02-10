# ShvmDB YCSB Binding

This binding benchmarks [shvm-db](https://github.com/shivamd20/shvm-db), a DynamoDB-compatible database built on Cloudflare Durable Objects + SQLite.

## Properties

| Property | Default | Description |
|---|---|---|
| `shvmdb.endpoint` | `http://localhost:8787` | HTTP endpoint for shvm-db |
| `shvmdb.table` | `usertable` | DynamoDB table name |
| `shvmdb.debug` | `false` | Enable debug logging |

## Quick Start

1. **Start shvm-db** locally:
   ```bash
   cd /path/to/shvm-db
   npm start  # runs wrangler dev
   ```

2. **Build the binding**:
   ```bash
   cd /path/to/ycsb
   mvn -pl site.ycsb:shvmdb-binding -am clean package -DskipTests
   ```

3. **Run YCSB**:
   ```bash
   # Load phase
   bin/ycsb.sh load shvmdb -P workloads/workloada -p shvmdb.endpoint=http://localhost:8787

   # Run phase
   bin/ycsb.sh run shvmdb -P workloads/workloada -p shvmdb.endpoint=http://localhost:8787
   ```

## Supported Operations

| Operation | DynamoDB API | Status |
|---|---|---|
| Insert | PutItem | ✅ |
| Read | GetItem | ✅ |
| Update | PutItem (upsert) | ✅ |
| Delete | DeleteItem | ✅ |
| Scan | Query/Scan | ❌ (MVP) |

## Key Mapping

YCSB keys are mapped as both PK and SK in DynamoDB format:
- `PK` = YCSB record key
- `SK` = YCSB record key (same, for single-item operations)
- Fields are stored as DynamoDB `S` (string) attributes
