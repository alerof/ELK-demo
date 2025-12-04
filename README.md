# ELK Stack Demo 

## 🎯 Two Main Features

This project demonstrates **two main features**:

| # | Feature | Property | Config File | Default |
|---|---------|----------|-------------|---------|
| 1 | **Send Events to Elasticsearch** | `elastic.client.enabled` | `application.properties` | `false` |
| 2 | **Monitor Logs** | `produce.logs` | `application.properties` | `true` |

---

## 🚀 Running the Solution

### Prerequisites

- Docker and Docker Compose installed
- Java 21+ (for local development)
- Maven 3.6+

### Step 1: Build Spring Boot Application

```bash
./mvnw clean package -DskipTests
```

This creates `target/elk-demo.jar`.

### Step 2: Start the ELK Stack

```bash
docker-compose down && docker-compose up
```

This starts: Elasticsearch (9200), Logstash (5044), Filebeat, Kibana (5601)

---

## ▶️ Run Feature 1: Send Events to Elasticsearch

**Direct Elasticsearch client operations (CRUD on `events2` index)**

```bash
# Run with ES client enabled
java -jar target/elk-demo.jar --elastic.client.enabled=true --produce.logs=false
```

Or via Maven:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--elastic.client.enabled=true --produce.logs=false"
```

**What happens:**
- Application creates/updates Event documents in Elasticsearch
- Uses `HighLevelElasticClient` or `LowLevelElasticClient`
- Data stored in index: `events2`

**Verify:** http://localhost:9200/events2/_search

**Postman Collection:** Import `JAMP-module-6-elk.postman_collection.json` for ready-to-use ES requests:
- CRUD operations (Save, Get, Update, Delete events)
- Search queries (match, term, bool, aggregations)
- Health check and mapping endpoints

---

## ▶️ Run Feature 2: Monitor Logs (ELK Pipeline)

**Centralized logging via Filebeat → Logstash → Elasticsearch → Kibana**

```bash
# Run with log production enabled (default)
java -jar target/elk-demo.jar --produce.logs=true --elastic.client.enabled=false
```

Or simply:
```bash
./mvnw spring-boot:run
```

**What happens:**
- Application generates logs every 5 seconds for 30 minutes
- Logs written to `./logs/m6-elk.log`
- Filebeat ships logs to Logstash
- Logstash parses (Grok) and sends to Elasticsearch
- View logs in Kibana

**Verify:** http://localhost:5601 → Discover → `filebeat-*`

---

## ▶️ Run Both Features Together

```bash
java -jar target/elk-demo.jar --produce.logs=true --elastic.client.enabled=true
```

---

### Access the Services

| Service | URL | Description |
|---------|-----|-------------|
| Kibana | http://localhost:5601 | Log visualization (Feature 2) |
| Elasticsearch | http://localhost:9200 | REST API (Feature 1) |
| Logstash API | http://localhost:9600 | Monitoring |

---

## 📊 Feature 2: View Logs in Kibana

### Step-by-Step Guide (Kibana 8.x)

#### 1. Open Kibana
```
http://localhost:5601
```
> **Note:** Kibana takes 30-60 seconds to fully initialize after `docker-compose up`

#### 2. Create Data View (First Time Only)

1. Click **☰** (hamburger menu) → **Stack Management**
2. Click **Data Views** (under Kibana section)
3. Click **Create data view**
4. Configure:
   - **Name:** `filebeat-logs`
   - **Index pattern:** `filebeat-*`
   - **Timestamp field:** `@timestamp`
5. Click **Save data view to Kibana**

#### 3. View Application Logs

1. Click **☰** → **Discover**
2. Select `filebeat-logs` data view from dropdown (top-left)
3. Set time range: **Last 15 minutes** (top-right calendar icon)
4. You should see logs from the Spring Boot application

#### 4. Explore Log Fields

Logs are parsed by Logstash Grok patterns. Available fields:

| Field | Example | Description |
|-------|---------|-------------|
| `message` | Full log line | Original log message |
| `app_name` | `m6-elk` | Application name |
| `app_version` | `0.1.0-SNAPSHOT` | Application version |
| `log-level` | `INFO` | Log level |
| `COMPONENT_NAME` | `c.e.y.h.elk.LogProducer` | Java class |
| `MESSAGE` | `GET Event by id: 5` | Parsed message |
| `UUID` | `abc-123-...` | Correlation ID |
| `tags` | `["failed"]` | Custom tags |

#### 5. Filter Logs (KQL Examples)

Type in the search bar:

```
# Show logs with custom field
AUTHOR.keyword: "HELEN"

# Show failed operations
tags: "failed"

# Show only INFO logs from the app
log-level: "INFO" and app_name: "m6-elk"

# Show logs with Event in message
message: *Event*

# Combine filters
app_name: "m6-elk" and log-level: "INFO" and message: *Event*
```

#### 6. Add Columns to View

1. Hover over a field name in left sidebar
2. Click **+** to add as column
3. Recommended columns: `@timestamp`, `log-level`, `COMPONENT_NAME`, `MESSAGE`

### Troubleshooting

| Problem | Solution |
|---------|----------|
| Kibana not accessible | Wait 30-60 seconds after `docker-compose up` for initialization |
| No logs visible | Check time range (top-right), try "Last 1 hour" |
| Data view not found | Wait 1-2 minutes for first logs, then refresh |
| Fields not parsed | Check Logstash is running: `docker-compose ps` |
| Empty MESSAGE field | Log format may not match Grok pattern |
| Container not starting | Check logs: `docker-compose logs <service>` |

---

## 🔍 Feature 1: Elasticsearch CRUD Operations

### Using Postman Collection

Import `JAMP-module-6-elk.postman_collection.json` for ready-to-use requests:

| Operation | Method | Endpoint |
|-----------|--------|----------|
| Create Index | PUT | `/events2` |
| Add Document | POST | `/events2/_doc` |
| Get Document | GET | `/events2/_doc/{id}` |
| Update Document | PUT | `/events2/_doc/{id}` |
| Delete Document | DELETE | `/events2/_doc/{id}` |
| Search All | POST | `/events2/_search` |
| Search with Query | POST | `/events2/_search` |

### Example Search Query

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "eventType": "workshop" } }
      ],
      "filter": [
        { "range": { "date": { "gte": "2021-01-01" } } }
      ]
    }
  }
}
```

---

## 📈 Data Flow Summary

1. **Log Generation**: Spring Boot application generates structured logs using Log4j2
2. **Log Writing**: Logs are written to `./logs/m6-elk.log` file
3. **Log Shipping**: Filebeat monitors the log file and ships events to Logstash
4. **Log Processing**: Logstash applies Grok patterns and enriches data
5. **Indexing**: Elasticsearch stores logs for search and analysis
6. **Visualization**: Kibana provides dashboards and search capabilities

---

## 🔐 Security Considerations

1. **Elasticsearch**: Configure authentication in production
2. **Network**: Use Docker network isolation
3. **Log Sanitization**: Avoid logging sensitive data (PII, credentials)

---

## 📚 References

### Spring Boot Logging
- Spring Boot supports structured logging with formats like **Elastic Common Schema (ECS)**, **GELF**, and **Logstash JSON**
- Configure via `logging.structured.format.console` or `logging.structured.format.file` properties
- MDC (Mapped Diagnostic Context) values are automatically included in JSON output

### Elasticsearch Java Client (8.x)
- **Elasticsearch Java Client**: New fluent API with type-safe builders (replaces deprecated High-Level REST Client)
- **Low-Level REST Client**: Direct HTTP requests for maximum flexibility
- Both support bulk operations for efficient indexing

### Filebeat
- Lightweight log shipper with minimal resource usage
- Supports multiple input types (log, filestream, container)
- Handles backpressure and provides delivery guarantees

---

## 🧪 Testing

### Unit Tests

Run the Elasticsearch client tests:

```bash
# Low-level client tests
./mvnw test -Dtest=LowLevelElasticClientTest

# High-level client tests
./mvnw test -Dtest=HighLevelElasticClientTest
```

### Integration Testing

1. Start the ELK stack
2. Run the Spring Boot application
3. Verify logs appear in Kibana
