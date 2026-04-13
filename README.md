# OmniPrice 🏷️
> **Sistema Distribuito di Tracciamento Prezzi**

## 📝 Descrizione del Progetto

**OmniPrice** è un'applicazione distribuita client-server basata su un'architettura a microservizi. Il sistema consente agli utenti di cercare prodotti attraverso diverse piattaforme di e-commerce (simulate tramite microservizi mock alimentati da dataset JSON), visualizzare lo storico delle variazioni di prezzo e impostare alert personalizzati che si attivano in caso di ribassi.

L'obiettivo primario di questo progetto didattico/sperimentale è implementare una gestione robusta, asincrona e sicura della comunicazione tra nodi distribuiti. Affronta e risolve problematiche reali come la latenza di rete, il fallimento dei servizi esterni e la concorrenza, applicando rigorosamente i design pattern architetturali e comportamentali.

---

## 🏗️ Architettura e Tecnologie

Il sistema è suddiviso in un frontend reattivo e un backend a microservizi, supportato da un broker di messaggistica per le operazioni asincrone.

* **Frontend:** Single Page Application (SPA)
* **Backend:** Java con framework Spring Boot
* **Gestione Asincrona e Resilienza:** Java `CompletableFuture`, Resilience4J
* **Messaging:** RabbitMQ
* **Sicurezza:** JWT (JSON Web Token)
* **Gestione Dati:** Dataset fittizi basati su file JSON creati ad hoc

---

## 🧩 Design Pattern e Funzionalità Implementate

Il progetto fa un uso estensivo di pattern architetturali per garantire scalabilità, sicurezza e tolleranza ai guasti.

### 🛡️ Sicurezza e Controllo Accessi
* **Autenticazione e Hashing:** Gestione sicura del processo di login tramite hashing crittografico delle password.
* **JWT e RBAC (Role-Based Access Control):** Emissione e validazione di token JWT per la gestione delle sessioni, con controllo degli accessi strettamente basato sui ruoli dell'utente.
* **Reference Monitor (AOP):** Utilizzo di Spring AOP (Aspect-Oriented Programming) per intercettare e validare in modo centralizzato tutte le richieste in ingresso.

### 🌐 Gestione Interfaccia e Trasferimento Dati
* **Remote Facade e DTO:** Implementazione di un API Gateway che aggrega i dati provenienti dai vari microservizi sottostanti, restituendo al client solo Data Transfer Object (DTO) leggeri e ottimizzati.
* **Serialized LOB:** Ottimizzazione del payload di rete trasferendo i dati pesanti (come l'intero storico dei prezzi di un prodotto) solo a seguito di una richiesta esplicita dell'utente.
* **Session State:** Gestione temporanea e mantenimento dello stato durante le fasi di configurazione e personalizzazione degli alert da parte dell'utente.

### ⚡ Asincronia e Resilienza
* **CompletableFuture:** Esecuzione di interrogazioni parallele e non bloccanti verso i microservizi degli store, riducendo drasticamente i tempi di risposta globali.
* **Pattern di Resilienza (Resilience4J):**
    * *Circuit Breaker:* Prevenzione di sovraccarichi su servizi già in errore.
    * *Timeout:* Interruzione delle richieste che superano le soglie di latenza accettabili.
    * *Retry:* Ripetizione automatica delle richieste fallite per errori temporanei di rete.

### 📬 Messaggistica Asincrona (RabbitMQ)
* **Request Pipeline:** Elaborazione strutturata e asincrona delle notifiche di calo di prezzo attraverso una pipeline di code e worker.
* **Idempotent Receiver:** Meccanismo di sicurezza per scartare eventuali messaggi duplicati garantendo coerenza nei dati.
* **Request Batch:** Ottimizzazione delle performance di I/O raggruppando le scritture sul database in lotti (batch).

### ⚙️ Coordinamento dei Nodi Distribuiti
* **Leader-Followers:** Gestione del carico di lavoro tramite un pool di worker coordinato da un nodo leader centrale.
* **Heart Beat:** Monitoraggio continuo della salute e della disponibilità dei nodi worker per prevenire colli di bottiglia o blocchi del sistema.
* **Generation Clock:** Versionamento e marcatura temporale per la prevenzione di aggiornamenti obsoleti o fuori sequenza in ambiente concorrente.

---

## 🚀 Setup e Avvio Locale

*(Questa sezione può essere personalizzata con i comandi effettivi del tuo progetto)*

**Prerequisiti:**
* Java 17+
* Maven
* Node.js e npm (per il frontend)
* RabbitMQ (eseguibile localmente o tramite Docker)

