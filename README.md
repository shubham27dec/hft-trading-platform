# HFT-Inspired Trading Platform

A Java-based trading platform built to demonstrate distributed systems concepts — designed for low-latency order execution with a clean split between a normal Spring Boot stack and a microsecond-latency fast path.

Real market data from Alpaca WebSocket. Paper trading execution via Alpaca API.

---

## Architecture Overview

Two distinct zones:

**Normal Stack** (Spring Boot + Kafka + Redis) — all user-facing and non-latency-critical services
**Fast Path** (LMAX Disruptor + Chronicle + Aeron) — microsecond-latency execution core, no Spring, no GC

---

## Distributed Systems Concepts Demonstrated

| Concept | Technology | Where |
|---|---|---|
| Lock-free queue | LMAX Disruptor 4.x | Fast path — ~100ns per event, zero GC |
| Distributed messaging | Apache Kafka | Inter-service async, fan-out, replay |
| Cache — LRU eviction | Redis | Position cache — evict oldest accessed |
| Cache — LFU eviction | Redis | Hot symbol cache — AAPL/TSLA stay warm |
| Cache — TTL eviction | Redis | Quote cache — stale quotes auto-expire at 500ms |
| Off-heap cache | Chronicle Map | Fast path — no serialization, no GC |
| Write-Ahead Log | Chronicle Queue | Every order state before ack, crash recovery |
| WAL (distributed) | Kafka append-only topic | SEC Rule 17a-4 audit compliance |
| Bloom Filter | Guava | Order ID dedup + symbol watchlist fast-reject |
| Heartbeat / watchdog | Custom Aeron thread | 3 missed beats → halt bit → zero orders |
| CAP — CP | Pre-Trade Risk | Partition = halt, never bypass risk check |
| CAP — AP | Market Data feed | Serve stale tick, never block the pipeline |
| Multiple nodes | Docker + Kubernetes | 2 replicas per service, independent scaling |
| Inter-process comms | Aeron IPC | Nanosecond-latency between fast path stages |

---

## Tech Stack

| Concern | Technology |
|---|---|
| Services framework | Spring Boot 3.x |
| Fast path messaging | LMAX Disruptor 4.x |
| Inter-service messaging | Apache Kafka |
| Distributed cache | Redis |
| Off-heap cache | Chronicle Map |
| Write-Ahead Log | Chronicle Queue |
| Inter-process comms | Aeron |
| Bloom Filter | Guava |
| Database | PostgreSQL |
| Market data + execution | Alpaca Java SDK |
| Containerization | Docker + Kubernetes (Minikube) |
| Monitoring | Prometheus + Grafana |
| Build | Maven multi-module |
| Java version | Java 21 |

---

## Services

| Service | Stack | Responsibility |
|---|---|---|
| `order-entry-service` | Spring Boot REST | Accept orders from UI, validate, publish to Kafka |
| `position-service` | Spring Boot + Redis | Track live positions and P&L |
| `risk-dashboard-service` | Spring Boot + Kafka | Real-time risk monitoring |
| `notification-service` | Spring Boot + WebSocket | Push fills and alerts to UI |
| `audit-service` | Spring Boot + Kafka + PostgreSQL | Regulatory trail, trade history |
| `execution-engine` | Pure Java (no Spring) | Disruptor pipeline + Chronicle + Aeron — fast path |
| `hft-core` | Java library | Shared models, Kafka event schemas |

---

## Testing

| Layer | Tool |
|---|---|
| Unit | JUnit 5 + Mockito |
| API | Karate (BDD-style REST tests) |
| Integration | Testcontainers (real Kafka, Redis, PostgreSQL in Docker) |
| E2E | Manual full-stack run |

CI/CD: GitHub Actions — build → unit → API → integration → OWASP CVE scan → SonarCloud.

---

## Running Locally

```bash
# Start all services
docker-compose up

# UI
open http://localhost:3000   # Grafana dashboards
open http://localhost:8080   # Trader UI
```

Requires Docker Desktop. No cloud account needed — everything runs locally.

---

## Demo Scenarios

**CAP Theorem (partition simulation)**
```java
PartitionSimulator.simulatePartition();  // halt bit fires, zero orders sent
PartitionSimulator.restore();            // resumes
```

**WAL Recovery (crash + replay)**
Kill the execution engine mid-trade → restart → Chronicle Queue replays all unacknowledged transitions → nothing lost.
