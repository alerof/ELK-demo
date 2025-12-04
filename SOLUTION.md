# ELK Stack Demo - Solution Overview

## 📋 Project Overview

This project demonstrates a complete **ELK Stack** (Elasticsearch, Logstash, Kibana) implementation with a **Java Spring Boot** application.

---

## 🎯 Two Main Features

| # | Feature | Property | Default | Description |
|---|---------|----------|---------|-------------|
| 1 | **Send Events to Elasticsearch** | `elastic.client.enabled` | `false` | Direct CRUD operations via REST client |
| 2 | **Monitor Logs** | `produce.logs` | `true` | ELK Pipeline: Filebeat → Logstash → ES → Kibana |

---

## 🏗️ Architecture

```
┌──────────────────┐                      ┌──────────────────┐
│  Spring Boot App │ ───── REST API ───── │  Elasticsearch   │  ← Feature 1
│  (ES Client)     │      Port 9200       │  (Index: events2)│
└──────────────────┘                      └──────────────────┘


┌──────────────┐    ┌──────────┐    ┌──────────┐    ┌───────────────┐    ┌────────┐
│ Spring Boot  │───▶│ Log File │───▶│ Filebeat │───▶│   Logstash    │───▶│   ES   │  ← Feature 2
│ (Log4j2)     │    │ m6-elk   │    │          │    │ (Grok Parse)  │    │        │
└──────────────┘    └──────────┘    └──────────┘    └───────────────┘    └────┬───┘
                                                                              │
                                                                              ▼
                                                                       ┌──────────┐
                                                                       │  Kibana  │
                                                                       │  (View)  │
                                                                       └──────────┘
```

---

## 🛠️ Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **Java** | 21 | Runtime environment (LTS) |
| **Spring Boot** | 3.5.3 | Java application framework |
| **Elasticsearch** | 8.17.0 | Distributed search and analytics engine |
| **Logstash** | 8.17.0 | Log processing pipeline |
| **Kibana** | 8.17.0 | Data visualization dashboard |
| **Filebeat** | 8.17.0 | Lightweight log shipper |
| **ES Java Client** | 8.17.0 | Elasticsearch Java API |
| **Docker** | - | Container orchestration |

---

## 📁 Key Components

| Component | File | Description |
|-----------|------|-------------|
| **Spring Boot App** | `src/main/java/.../Application.java` | Entry point |
| **Log Producer** | `src/main/java/.../LogProducer.java` | Generates logs |
| **ES Clients** | `HighLevelElasticClient` / `LowLevelElasticClient` | Elasticsearch operations |
| **Event Model** | `Event.java` | Business data (index: `events2`) |
| **Docker Stack** | `docker-compose.yml` | ELK orchestration |
| **Logstash Pipeline** | `logstash.conf` | Log parsing (Grok) |

---

## 📝 Quick Reference

**Configuration File:**
- `application.properties` - Features 1 & 2

**Ports:**
- `9200` - Elasticsearch
- `5601` - Kibana
- `5044` - Logstash (Beats input)

**ES Indexes:**
- `events2` - Business events (Feature 1)
- `filebeat-*` - Application logs (Feature 2)

---

## 📚 Details

For build instructions, running commands, and detailed configuration:

👉 **See [README.md](README.md)**
