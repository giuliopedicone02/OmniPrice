# Dataset OmniPrice - Documentazione

## 📋 Panoramica

Questo dataset JSON contiene i dati simulati per il progetto **OmniPrice**, un sistema distribuito di tracciamento prezzi multi-store. I file JSON sono strutturati per supportare un'applicazione reale con microservizi, gestione utenti, alert personalizzati e storico dei prezzi.

---

## 📁 Struttura dei File

### 1. **product-catalog.json**
Catalogo completo dei prodotti disponibili nel sistema.

**Struttura di un prodotto:**
```json
{
  "id": "PROD001",
  "name": "Laptop Dell XPS 13",
  "category": "Elettronica",
  "subcategory": "Computer Portatili",
  "description": "...",
  "specs": {
    "processor": "Intel Core i7-1360P",
    "ram": "16GB DDR5",
    "storage": "512GB NVMe SSD",
    ...
  },
  "imageUrl": "https://...",
  "releaseDate": "2024-01-15"
}
```

**Contenuto:** 12 prodotti diversificati tra:
- Laptop/Computer (PROD001, PROD005)
- Smartphone (PROD002)
- Audio (PROD003)
- TV/Display (PROD004)
- Videogiochi (PROD006)
- Tablet (PROD007)
- Fotografia (PROD008)
- Droni (PROD009)
- Periferiche (PROD010, PROD011)
- Archiviazione (PROD012)

---

### 2. **store-prices.json**
Prezzi attuali dei prodotti presso 4 store mock, con dettagli di disponibilità e valutazioni.

**4 Store Mock:**
- `STORE001`: TechMart
- `STORE002`: ElectroWorld
- `STORE003`: DigitalHub
- `STORE004`: MegaStore

**Struttura di un prezzo:**
```json
{
  "productId": "PROD001",
  "productName": "Laptop Dell XPS 13",
  "price": 1299.99,           // Prezzo base
  "discount": 0,              // Sconto percentuale
  "finalPrice": 1299.99,      // Prezzo finale
  "availability": "in_stock", // in_stock, low_stock, limited_stock, on_order
  "estimatedDelivery": "2-3 giorni",
  "rating": 4.7,
  "reviews": 245
}
```

**Caratteristiche:**
- Prezzi variabili per store
- Sconti differenziati
- Stato disponibilità realistica
- Tempi di consegna variabili
- Rating e numero di recensioni per tracciare la credibilità

---

### 3. **price-history.json**
Storico temporale dei prezzi per i ultimi 30 giorni (12-10 marzo → 10 aprile 2026).

**Struttura:**
```json
{
  "productId": "PROD001",
  "productName": "Laptop Dell XPS 13",
  "records": [
    {
      "date": "2026-03-12",
      "storeId": "STORE001",
      "storeName": "TechMart",
      "price": 1349.99,
      "discount": 0,
      "finalPrice": 1349.99
    },
    ...
  ]
}
```

**Utilizzi:**
- Generare grafici di trend dei prezzi
- Calcolare il prezzo medio nel tempo
- Identificare pattern di sconto stagionali
- Mostrare il "miglior prezzo storico" all'utente
- Trend analysis per algoritmi di predizione

---

### 4. **user-data.json**
Dati utenti, preferenze, alert personalizzati, cronologia ricerche e acquisti.

**Struttura Utente:**
```json
{
  "userId": "USR001",
  "username": "mario.rossi",
  "email": "mario.rossi@example.com",
  "passwordHash": "$2b$12$...",
  "role": "Standard",
  "registrationDate": "2025-06-15T10:30:00Z",
  "preferences": {
    "currency": "EUR",
    "notifications": {
      "emailAlerts": true,
      "pushNotifications": false,
      "priceDropThreshold": 10
    },
    "favoriteStores": ["STORE001", "STORE003"],
    "displayOptions": {
      "itemsPerPage": 10,
      "theme": "light"
    }
  },
  "watchlist": ["PROD001", "PROD002"],
  "priceAlerts": [
    {
      "alertId": "ALERT001",
      "productId": "PROD001",
      "targetPrice": 1250.00,
      "status": "active",     // active, triggered, expired
      "stores": ["STORE001"]
    }
  ],
  "searchHistory": [...],
  "purchaseHistory": [...]
}
```

**Utenti Mock (4 utenti):**
- **USR001** (mario.rossi): Standard user con 3 alert attivi
- **USR002** (laura.bianchi): Premium user con 4 alert (incluso uno triggered)
- **USR003** (giuseppe.neri): Admin user (accesso completo)
- **USR004** (elena.russo): Standard user (preferenze anglofone)

**Ruoli RBAC:**
- `Standard`: Accesso base, max 5 alert
- `Premium`: Funzioni avanzate, max 25 alert, previsioni prezzi
- `Admin`: Accesso completo al sistema

---

## 🎯 Casi d'Uso per il Progetto OmniPrice

### 1. **Ricerca Prodotti (Aggregazione Multi-Store)**
```
Frontend → API Gateway → CompletableFuture.allOf(store1, store2, store3, store4)
         → Aggregazione risultati da store-prices.json
         → DTO ritornato al cliente
```
- Usa: `product-catalog.json` + `store-prices.json`

### 2. **Visualizzazione Storico Prezzi (Serialized LOB)**
```
Utente clicca su "Grafico Storico"
→ Server carica dati da price-history.json SOLO SU RICHIESTA
→ Frontend disegna il grafico con trend
```
- Usa: `price-history.json`

### 3. **Creazione Alert (Memento Pattern)**
```
Utente configura alert per prodotto
→ Session state salvato lato server
→ RabbitMQ processa evento di calo prezzo
→ Notifica inviata se prezzo < targetPrice
```
- Usa: `user-data.json` (priceAlerts section)

### 4. **Circuit Breaker e Retry**
```
Se STORE002 non risponde:
→ Timeout dopo 5 secondi
→ Retry automatico
→ Se fallisce 3 volte → Circuit Breaker apre
→ Response con dati da STORE001, STORE003, STORE004
```

### 5. **Leader-Followers + Idempotent Receiver**
```
Worker legge store-prices.json ogni 5 minuti
→ Invia aggiornamenti a RabbitMQ
→ Database riceve messaggi DUPLICATI (rete) ma li scarta
→ Eventualmente uno solo viene salvato in price-history.json
```

### 6. **RBAC e Reference Monitor (AOP)**
```
USR001 (Standard) → tenta di esportare dati
  ❌ Permesso DENIED (non ha permission EXPORT_DATA)

USR002 (Premium) → tenta di esportare dati
  ✅ Permesso GRANTED (ha permission EXPORT_DATA)
  
AOP intercetta e valida prima di raggiungere il controller
```

---

## 🔐 Sicurezza

### Password Hashing
Le password sono già hashate con **bcrypt** ($2b$12$...). Esempi:
- `mario.rossi`: hashata
- `laura.bianchi`: hashata
- `giuseppe.neri`: hashata
- `elena.russo`: hashata

In produzione, verificare con:
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
boolean matches = encoder.matches("plain_password", "$2b$12$...");
```

### JWT Token
Ogni login genera un JWT contenente:
- `sub`: userId
- `role`: ruolo utente
- `exp`: data scadenza (es. 1 ora)

Esempio payload:
```json
{
  "sub": "USR001",
  "role": "Standard",
  "exp": 1712761200,
  "iat": 1712757600
}
```

---

## 📊 Statistiche Dataset

| Metrica | Valore |
|---------|--------|
| **Prodotti** | 12 |
| **Store** | 4 |
| **Utenti** | 4 |
| **Ruoli RBAC** | 3 (Standard, Premium, Admin) |
| **Alert Totali** | 10 |
| **Record Storico** | ~60 (5 date × 4 store) |
| **Giorni Storico** | 30 (12 mar - 10 apr 2026) |
| **Intervallo Prezzo Prodotti** | €85 - €4700 |

---

## 🚀 Come Usare il Dataset

### Nel Backend (Spring Boot)

```java
// 1. Carica il catalogo all'avvio
@PostConstruct
public void loadProductCatalog() {
    productCatalog = objectMapper.readValue(
        new File("data/product-catalog.json"), 
        ProductCatalog.class
    );
}

// 2. Simula una query ai microservizi
@GetMapping("/api/search")
public CompletableFuture<SearchResponse> searchProducts(String query) {
    return CompletableFuture.allOf(
        queryStore(STORE001),
        queryStore(STORE002),
        queryStore(STORE003),
        queryStore(STORE004)
    ).thenApply(v -> aggregateResults());
}

// 3. Simula alert trigger
@Scheduled(fixedRate = 300000) // ogni 5 minuti
public void checkPriceAlerts() {
    // Leggi store-prices.json
    // Confronta con price-alerts in user-data.json
    // Se trigger, pubblica su RabbitMQ
}
```

### Nel Frontend (React/Vue)

```javascript
// 1. Recupera lista prodotti
const products = await fetch('/api/products').then(r => r.json());

// 2. Ricerca con aggregazione multi-store
const results = await fetch('/api/search?q=laptop').then(r => r.json());
// Riceve DTO come:
// {
//   productId: "PROD001",
//   lowestPrice: 1241.59 (STORE002),
//   allPrices: { STORE001: 1299.99, STORE002: 1241.59, ... },
//   avgPrice: 1267.26,
//   availability: "in_stock"
// }

// 3. Carica storico prezzi on-demand (Serialized LOB)
const history = await fetch('/api/products/PROD001/history')
  .then(r => r.json());
// Disegna grafico con Chart.js
```

---

## ⚙️ Configurazione Microservizi Mock

Quando implementate i microservizi mock degli store, potete:

1. **Leggerli da file JSON (statico)**
   ```java
   storeService.getPrices() → legge store-prices.json
   ```

2. **Simulare latenza**
   ```java
   Thread.sleep(random(100, 5000)); // 100ms - 5s
   ```

3. **Simulare failure**
   ```java
   if (Random.nextInt(10) == 0) throw new TimeoutException();
   ```

4. **Simulare dati dinamici**
   ```java
   float jitter = Random.nextFloat() * 0.05; // ±5%
   return basePrice * (1 + jitter);
   ```

---

## 📝 Note Importanti

1. **Le date sono fittive**: Tutti i timestamp sono nel 2026 per coerenza
2. **I prezzi sono realistici**: Basati su prezzi effettivi di mercato (aprile 2026)
3. **Gli store sono mock**: Non sono veri store, servono solo per testing
4. **I dati sono coerenti**: Lo storico è costruito coerentemente con i prezzi attuali
5. **Gli alert sono realistici**: Alcuni sono "triggered" per testare notifiche

---

## 🔄 Flusso Completo di Esempio

**Scenario**: Mario cerca un laptop e crea un alert

1. **Login**
   - Input: mario.rossi / password
   - Lookup in `user-data.json` → USR001
   - Hash match → genera JWT
   - Response: token + ruolo "Standard"

2. **Ricerca "laptop"**
   - Query → `/api/search?q=laptop`
   - Server interroga 4 store in parallelo (CompletableFuture)
   - Legge da `store-prices.json`
   - Filtra: name contiene "laptop" → PROD001, PROD005
   - Aggrega: miglior prezzo, disponibilità, rating
   - DTO ritornato: 2 prodotti

3. **Visualizza dettagli PROD001**
   - Click su "Storico Prezzi"
   - Server legge `price-history.json` per PROD001
   - Ritorna 13 record nei 30 giorni
   - Frontend disegna grafico con Chart.js

4. **Crea Alert**
   - Mario: "Avvisami se scende sotto €1250"
   - Server crea ALERT011 in `user-data.json`
   - RabbitMQ iscrive il worker

5. **Trigger Alert (dopo 2 giorni)**
   - Worker legge store-prices.json
   - PROD001 da STORE002 = €1241.59
   - Confronta con ALERT003 targetPrice = €1250
   - Trigger! → Pubblica su RabbitMQ
   - Pipeline: Genera testo → Recupera email → Invia email
   - Marca alert come "triggered"
   - Mario riceve email

---

**Buon lavoro con il progetto OmniPrice!** 🚀
