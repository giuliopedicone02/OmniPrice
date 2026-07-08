<div align="center">

# OmniPrice

**Sistema distribuito per il tracciamento dei prezzi su più piattaforme e‑commerce**

[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-42b883?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-messaging-FF6600?logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

*Progetto per il corso di **Ingegneria dei Sistemi Distribuiti** — Università degli Studi di Catania*

</div>

---

## Indice

- [Panoramica](#panoramica)
- [Demo](#demo)
- [Funzionalità](#funzionalità)
- [Architettura](#architettura)
- [Design pattern implementati](#design-pattern-implementati)
- [Stack tecnologico](#stack-tecnologico)
- [Avvio rapido](#avvio-rapido)
- [Configurazione](#configurazione)
- [Account demo](#account-demo)
- [API principali](#api-principali)
- [Struttura del progetto](#struttura-del-progetto)
- [Testing](#testing)
- [Autori](#autori)
- [Licenza](#licenza)

---

## Panoramica

**OmniPrice** è un'applicazione client‑server basata su architettura a microservizi che permette agli utenti di:

- **cercare prodotti** interrogando in parallelo diverse piattaforme e‑commerce (simulate da microservizi *mock* alimentati da dataset JSON);
- **confrontare i prezzi** in tempo reale e individuare il migliore;
- **consultare lo storico** delle variazioni di prezzo di ogni prodotto;
- **impostare alert personalizzati** che si attivano quando il prezzo scende sotto una soglia scelta.

L'obiettivo didattico è realizzare una comunicazione **robusta, asincrona e sicura** tra nodi distribuiti, affrontando problemi reali — latenza di rete, fallimento dei servizi esterni, concorrenza, coordinamento — attraverso l'applicazione rigorosa dei **design pattern** dei sistemi distribuiti.

I requisiti funzionali e non funzionali sono documentati in [`REQUIREMENTS.md`](REQUIREMENTS.md).

---

## Demo

> Interfaccia sviluppata in Vue 3 + Tailwind CSS. Le immagini sono catturate dall'applicazione in esecuzione.

### Login & registrazione
Autenticazione con hashing delle password (BCrypt) e rilascio di token JWT.

![Login](docs/screenshots/01-login.png)

### Ricerca prodotti
Interrogazione **parallela** di 4 store via `CompletableFuture`, con aggregazione dei risultati in un unico DTO (Remote Facade).

![Ricerca](docs/screenshots/02-search.png)

### Dettaglio prodotto & storico prezzi
Confronto tra store e grafico dello storico prezzi caricato **on‑demand** (pattern Serialized LOB).

![Dettaglio prodotto](docs/screenshots/03-product.png)

### Gestione alert
Alert personalizzati per utente; quelli scattati vengono elaborati tramite pipeline asincrona su RabbitMQ.

![Alert](docs/screenshots/04-alerts.png)

---

## Funzionalità

| Area | Descrizione |
|------|-------------|
| 🔐 **Autenticazione** | Login/registrazione con password hashing (BCrypt) e JWT stateless |
| 👥 **Ruoli** | Controllo accessi basato sui ruoli (`STANDARD`, `PREMIUM`, `ADMIN`) |
| 🔍 **Ricerca multi‑store** | Aggregazione parallela e resiliente dei prezzi da 4 store |
| 📈 **Storico prezzi** | Serie storica per prodotto/store, caricata su richiesta |
| 🔔 **Alert di prezzo** | Notifiche asincrone quando il prezzo scende sotto la soglia |
| 🛡️ **Resilienza** | Circuit Breaker, Retry e Timeout su ogni chiamata di store |
| 🧭 **Coordinamento** | Cluster con elezione del leader, quorum ed epoche (Generation Clock) |
| 📊 **Pannello admin** | Endpoint di monitoraggio di cluster, worker pool ed heartbeat |

---

## Architettura

### Vista dei componenti

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'Segoe UI, Roboto, sans-serif','lineColor':'#64748b','edgeLabelBackground':'#f8fafc'}}}%%
flowchart TB
    FE["🖥️ Frontend — Vue 3 SPA<br/>Vite · Pinia · Axios"]:::frontend

    subgraph BE["🍃 Backend — Spring Boot"]
        direction TB
        SEC["🔒 RateLimit → JWT Filter<br/>→ Reference Monitor (AOP)"]:::security
        P["📦 Product<br/>Remote Facade"]:::data
        A["🔔 Alert<br/>Session State"]:::data
        H["📈 PriceHistory<br/>Serialized LOB"]:::data
        AD["🛠️ Admin<br/>Cluster status"]:::admin
        STORES["🏪 Store microservices (mock)<br/>STORE001 · STORE002 · STORE003 · STORE004"]:::resilience
        SCHED["⏱️ Scheduler<br/>price check / batch"]:::msg
        PROD["📤 AlertProducer"]:::msg
        CONS["📥 AlertConsumer<br/>Idempotent Receiver"]:::msg
        CLUSTER["🧭 Cluster Coordinator<br/>Leader-Followers · Majority Quorum<br/>Heart Beat · Generation Clock"]:::cluster
    end

    RMQ[["🐇 RabbitMQ"]]:::broker
    DB[("🗄️ H2 in-memory")]:::db

    FE -->|"HTTP/JSON + JWT"| SEC
    SEC --> P & A & H & AD
    P -->|"CompletableFuture<br/>+ CircuitBreaker / Retry / Timeout"| STORES
    SCHED --> PROD --> RMQ --> CONS
    CONS --> DB
    A --> DB
    AD --> CLUSTER

    classDef frontend fill:#ede9fe,stroke:#7c3aed,stroke-width:2px,color:#3b0764;
    classDef security fill:#fee2e2,stroke:#dc2626,stroke-width:2px,color:#7f1d1d;
    classDef data fill:#ccfbf1,stroke:#0d9488,stroke-width:2px,color:#134e4a;
    classDef admin fill:#dbeafe,stroke:#2563eb,stroke-width:2px,color:#1e3a8a;
    classDef resilience fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#78350f;
    classDef msg fill:#ffedd5,stroke:#ea580c,stroke-width:2px,color:#7c2d12;
    classDef cluster fill:#f3e8ff,stroke:#9333ea,stroke-width:2px,color:#581c87;
    classDef broker fill:#ffe4cc,stroke:#ea580c,stroke-width:2px,color:#7c2d12;
    classDef db fill:#e2e8f0,stroke:#475569,stroke-width:2px,color:#1e293b;
    style BE fill:#f0fdf4,stroke:#16a34a,stroke-width:2px,color:#14532d;
```

> Il sistema funziona anche in **modalità degradata** senza RabbitMQ (`omniprice.rabbitmq.enabled=false`): in tal caso la pipeline di notifiche viene eseguita in‑process.

### Pipeline di notifica di calo prezzo (asincrona)

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'Segoe UI, Roboto, sans-serif','actorBkg':'#d1fae5','actorBorder':'#10b981','actorTextColor':'#064e3b','actorLineColor':'#10b981','signalColor':'#334155','signalTextColor':'#334155','noteBkgColor':'#fef9c3','noteBorderColor':'#eab308','noteTextColor':'#713f12','labelBoxBkgColor':'#ede9fe','labelBoxBorderColor':'#7c3aed','labelTextColor':'#3b0764','sequenceNumberColor':'#ffffff'}}}%%
sequenceDiagram
    autonumber
    participant S as ⏱️ PriceCheckScheduler
    participant Pr as 📤 AlertProducer
    participant Q as 🐇 RabbitMQ
    participant C as 📥 AlertConsumer
    participant DB as 🗄️ Database

    S->>S: verifica alert attivi (epoca N)
    Note over S: prezzo < soglia?
    S->>Pr: publishAlertTriggered
    Pr->>Q: ALERT_TRIGGERED (messageId)
    Q-->>C: consegna asincrona
    alt messageId già processato
        C-->>C: scarta (Idempotent Receiver)
    else nuovo messaggio
        C->>DB: registra messaggio + aggiorna alert
    end
```

### Ciclo di vita di un nodo del cluster

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'Segoe UI, Roboto, sans-serif','lineColor':'#64748b'}}}%%
stateDiagram-v2
    [*] --> Candidate
    Candidate --> Leader: vince l'elezione (quorum)
    Candidate --> Follower: eletto un altro nodo
    Follower --> Candidate: timeout heartbeat del leader
    Leader --> Follower: epoca superata (leader zombie)
    Leader --> [*]: guasto
    Follower --> [*]: guasto

    classDef leader fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#14532d
    classDef follower fill:#dbeafe,stroke:#2563eb,stroke-width:2px,color:#1e3a8a
    classDef candidate fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#78350f

    class Leader leader
    class Follower follower
    class Candidate candidate
```

---

## Design pattern implementati

Ogni pattern corrisponde a un argomento del corso di Ingegneria dei Sistemi Distribuiti (Prof. E. Tramontana).

### Sicurezza e controllo accessi
| Pattern | Implementazione |
|---------|-----------------|
| **Authentication & Hashing** | `SecurityConfig` (BCrypt), `AuthController`, `UserDetailsServiceImpl` |
| **Token (JWT)** | `JwtUtil`, `JwtAuthFilter` |
| **Role‑Based Access Control** | Ruoli nei claim JWT + `@RequiresRole` |
| **Reference Monitor (AOP)** | `SecurityAspect` intercetta ogni metodo protetto |
| **Rate Limiting / Authenticator** | `LoginRateLimitFilter` (Resilience4J) davanti al login |

### Interfaccia e trasferimento dati
| Pattern | Implementazione |
|---------|-----------------|
| **Remote Facade + DTO** | `ProductController`/`ProductService` aggregano i microservizi in DTO leggeri |
| **Serialized LOB** | `PriceHistoryController` — storico serializzato e trasferito on‑demand |
| **Session State** | `AlertController`/`AlertService` — stato degli alert per utente |

### Asincronia e resilienza
| Pattern | Implementazione |
|---------|-----------------|
| **CompletableFuture** | `StoreService` interroga i 4 store in parallelo |
| **Circuit Breaker / Retry / Timeout** | `StoreService` con Resilience4J (istanze isolate per store) |

### Messaggistica asincrona (RabbitMQ)
| Pattern | Implementazione |
|---------|-----------------|
| **Request Pipeline** | `PriceCheckScheduler` → `AlertProducer` → RabbitMQ → `AlertConsumer` |
| **Idempotent Receiver** | `AlertConsumer` + `ProcessedMessage` (deduplicazione per `messageId`) |
| **Request Batch** | `BatchWriteScheduler` raggruppa le scritture in un unico messaggio |

### Coordinamento dei nodi distribuiti
| Pattern | Implementazione |
|---------|-----------------|
| **Leader‑Followers** | `ClusterCoordinator` (consenso) e `WorkerPool` (concorrenza a thread) |
| **Majority Quorum** | Scritture confermate solo con la maggioranza dei nodi |
| **Heart Beat** | `HeartBeatService` per la failure detection |
| **Generation Clock** | `GenerationClockService` — epoche per neutralizzare il *leader zombie* |

---

## Stack tecnologico

| Livello | Tecnologie |
|---------|-----------|
| **Frontend** | Vue 3, Vite, Vue Router, Pinia, Axios, Chart.js, Tailwind CSS |
| **Backend** | Java 17, Spring Boot, Spring Security, Spring AMQP, Spring AOP |
| **Resilienza** | Resilience4J (Circuit Breaker, Retry, Rate Limiter) |
| **Messaging** | RabbitMQ |
| **Persistenza** | H2 (in‑memory), Spring Data JPA |
| **Sicurezza** | JWT (jjwt), BCrypt |
| **Build & tooling** | Maven Wrapper, npm, Docker Compose |

> Le dipendenze runtime sono dichiarate in `backend/omniprice/pom.xml` (Maven) e `frontend/package.json` (npm).

---

## Avvio rapido

### Prerequisiti
- **Java 17+**
- **Node.js 18+** e npm
- **Docker** (opzionale, per RabbitMQ)

### 1. Clona il repository
```bash
git clone https://github.com/giuliopedicone02/OmniPrice.git
cd OmniPrice
```

### 2. (Opzionale) Avvia RabbitMQ
```bash
docker compose up -d
# Management UI: http://localhost:15672  (guest / guest)
```

### 3. Backend
```bash
cd backend/omniprice
./mvnw spring-boot:run
# con RabbitMQ attivo:
./mvnw spring-boot:run -Dspring-boot.run.arguments=--omniprice.rabbitmq.enabled=true
```
Il backend è disponibile su **http://localhost:8080/api**.

### 4. Frontend
```bash
cd frontend
npm install
npm run dev
```
Il frontend è disponibile su **http://localhost:5173**.

### Avvio con un solo comando
Dalla root del progetto:
```bash
./script.sh   # avvia backend e frontend insieme
```

---

## Configurazione

I parametri principali si trovano in `backend/omniprice/src/main/resources/application.properties` e sono sovrascrivibili da riga di comando o variabili d'ambiente. Vedi [`.env.example`](.env.example) per l'elenco completo.

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `omniprice.jwt.secret` | *(dev)* | Chiave di firma JWT (Base64). **Da cambiare in produzione.** |
| `omniprice.jwt.expiration` | `86400000` | Durata token (ms) |
| `omniprice.rabbitmq.enabled` | `false` | Abilita la pipeline RabbitMQ |
| `omniprice.store.timeout-seconds` | `3` | Timeout per chiamata store |
| `omniprice.ratelimit.login.limit-for-period` | `5` | Tentativi di login per IP nel periodo |
| `omniprice.scheduler.price-check-cron` | `0 */5 * * * *` | Frequenza controllo prezzi |
| `omniprice.scheduler.batch-write-cron` | `0 */2 * * * *` | Frequenza scrittura batch |

> ⚠️ La chiave JWT presente nel repository è solo per lo sviluppo locale: sostituiscila con un segreto proprio prima di qualsiasi deploy.

---

## Account demo

Utenti creati automaticamente all'avvio (`DataInitializer`):

| Email | Password | Ruolo |
|-------|----------|-------|
| `mario@example.com` | `PasswordSuperSicura123!` | STANDARD |
| `laura@example.com` | `Password123!` | PREMIUM |
| `admin@example.com` | `Admin1234!` | ADMIN |

---

## API principali

Base URL: `http://localhost:8080/api`

| Metodo | Endpoint | Ruolo | Descrizione |
|--------|----------|-------|-------------|
| `POST` | `/auth/register` | pubblico | Registrazione utente |
| `POST` | `/auth/login` | pubblico | Login → token JWT |
| `GET`  | `/products/search?q=` | autenticato | Ricerca prodotti multi‑store |
| `GET`  | `/products/{id}` | autenticato | Dettaglio prodotto |
| `GET`  | `/price-history/{id}` | autenticato | Storico prezzi (Serialized LOB) |
| `GET`  | `/alerts` | autenticato | Alert dell'utente |
| `POST` | `/alerts` | autenticato | Crea alert |
| `DELETE` | `/alerts/{id}` | autenticato | Elimina alert |
| `GET`  | `/admin/cluster` | ADMIN | Stato del cluster |
| `POST` | `/admin/cluster/write` | ADMIN | Scrittura con quorum |
| `POST` | `/admin/cluster/fail/{nodeId}` | ADMIN | Simula guasto nodo → elezione |
| `GET`  | `/admin/status` | ADMIN | Worker pool + heartbeat |

Esempio:
```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"mario@example.com","password":"PasswordSuperSicura123!"}' | jq -r .token)

# Ricerca
curl -s "http://localhost:8080/api/products/search?q=laptop" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Struttura del progetto

```
OmniPrice/
├── backend/omniprice/          # Applicazione Spring Boot
│   └── src/main/java/com/unict/dmi/omniprice/
│       ├── annotation/         # @RequiresRole
│       ├── aspect/             # SecurityAspect (Reference Monitor)
│       ├── cluster/            # ClusterCoordinator, ClusterNode (consenso)
│       ├── config/             # Security, RabbitMQ, Async, DataInitializer
│       ├── controller/         # Remote Facade (REST)
│       ├── distributed/        # HeartBeat, GenerationClock, WorkerPool
│       ├── dto/                # Data Transfer Object
│       ├── messaging/          # Producer/Consumer RabbitMQ
│       ├── scheduler/          # PriceCheck, BatchWrite
│       ├── security/           # JWT, filtri, rate limiting
│       └── service/            # Logica di dominio
├── frontend/                   # SPA Vue 3
│   └── src/{components,router,services,store}
├── dataset/                    # Dataset JSON (cataloghi, prezzi, storico)
├── docs/screenshots/           # Immagini della demo
├── docker-compose.yml          # RabbitMQ
└── script.sh                   # Avvio combinato backend + frontend
```

---

## Testing

```bash
cd backend/omniprice
./mvnw test
```
La suite include i test del `ClusterCoordinator` (elezione del leader, quorum, rifiuto del leader zombie tramite Generation Clock).

---

## Autori

- **Giulio Pedicone** — [@giuliopedicone02](https://github.com/giuliopedicone02)
- **Francesco Prospero Antonio Virzì**

Progetto realizzato per il corso di *Ingegneria dei Sistemi Distribuiti*, Corso di Laurea Magistrale in Informatica, Università degli Studi di Catania.

---

## Licenza

Distribuito con licenza **MIT**. Vedi il file [`LICENSE`](LICENSE) per i dettagli.
