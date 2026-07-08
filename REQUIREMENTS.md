# Requisiti — OmniPrice

Documento dei requisiti del sistema. Le dipendenze software (librerie) sono
invece dichiarate nei rispettivi manifest di build:
[`backend/omniprice/pom.xml`](backend/omniprice/pom.xml) (Maven) e
[`frontend/package.json`](frontend/package.json) (npm).

---

## 1. Requisiti funzionali (RF)

| ID | Requisito | Priorità |
|----|-----------|----------|
| **RF‑1** | L'utente può registrarsi fornendo nome, email e password. | Alta |
| **RF‑2** | L'utente può autenticarsi ricevendo un token JWT valido. | Alta |
| **RF‑3** | Il sistema assegna a ogni utente un ruolo (`STANDARD`, `PREMIUM`, `ADMIN`). | Alta |
| **RF‑4** | L'utente autenticato può cercare prodotti per nome/parola chiave. | Alta |
| **RF‑5** | Il sistema interroga in parallelo tutti gli store e aggrega i prezzi. | Alta |
| **RF‑6** | Il sistema evidenzia il miglior prezzo tra gli store per ogni prodotto. | Media |
| **RF‑7** | L'utente può visualizzare lo storico dei prezzi di un prodotto su richiesta. | Media |
| **RF‑8** | L'utente può creare un alert con una soglia di prezzo per un prodotto. | Alta |
| **RF‑9** | L'utente può elencare ed eliminare i propri alert. | Media |
| **RF‑10** | Il sistema verifica periodicamente i prezzi e attiva gli alert al di sotto della soglia. | Alta |
| **RF‑11** | Un utente `ADMIN` può monitorare lo stato del cluster, del worker pool e degli heartbeat. | Media |
| **RF‑12** | Un utente `ADMIN` può simulare guasti dei nodi e osservare la ri‑elezione del leader. | Bassa |

## 2. Requisiti non funzionali (RNF)

| ID | Categoria | Requisito |
|----|-----------|-----------|
| **RNF‑1** | Sicurezza | Le password sono memorizzate solo come hash (BCrypt), mai in chiaro. |
| **RNF‑2** | Sicurezza | Ogni endpoint protetto richiede un JWT valido e il ruolo adeguato (RBAC + Reference Monitor). |
| **RNF‑3** | Sicurezza | L'endpoint di login è protetto da rate limiting per IP contro attacchi brute‑force/DoS. |
| **RNF‑4** | Prestazioni | L'interrogazione degli store avviene in parallelo per ridurre la latenza complessiva. |
| **RNF‑5** | Resilienza | Il guasto o la lentezza di un singolo store non blocca l'intera ricerca (Circuit Breaker, Timeout, Retry). |
| **RNF‑6** | Affidabilità | Le notifiche duplicate non producono effetti collaterali (Idempotent Receiver). |
| **RNF‑7** | Scalabilità | Le scritture di aggiornamento prezzo sono raggruppate in batch per ridurre l'I/O. |
| **RNF‑8** | Consistenza | Le scritture del cluster sono confermate solo con la maggioranza dei nodi (Majority Quorum). |
| **RNF‑9** | Consistenza | Gli aggiornamenti obsoleti provenienti da un leader superato vengono respinti (Generation Clock). |
| **RNF‑10** | Disponibilità | Il fallimento del leader innesca una nuova elezione tramite failure detection (Heart Beat). |
| **RNF‑11** | Portabilità | Il sistema è avviabile in locale con Docker (RabbitMQ) e funziona anche in modalità degradata senza broker. |
| **RNF‑12** | Manutenibilità | Ogni design pattern è isolato in un componente dedicato e documentato nel codice. |

## 3. Vincoli tecnologici

- **Backend:** Java 17, Spring Boot.
- **Frontend:** Vue 3 (SPA).
- **Messaging:** RabbitMQ (opzionale a runtime).
- **Persistenza:** database relazionale (H2 in‑memory in ambiente di sviluppo).
- **Dataset:** file JSON fittizi che simulano i cataloghi e i prezzi degli store.

## 4. Attori

| Attore | Descrizione |
|--------|-------------|
| **Utente STANDARD/PREMIUM** | Cerca prodotti, consulta storico, gestisce i propri alert. |
| **Utente ADMIN** | Come sopra, più accesso agli endpoint di monitoraggio e gestione del cluster. |
| **Scheduler di sistema** | Processo interno che controlla i prezzi e alimenta la pipeline di notifiche. |
