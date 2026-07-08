# Contribuire a OmniPrice

Grazie per l'interesse verso il progetto! Queste linee guida aiutano a mantenere
il codice coerente e facile da recensire.

## Prerequisiti di sviluppo

- Java 17+
- Node.js 18+ e npm
- Docker (per RabbitMQ, opzionale)

## Setup

```bash
git clone https://github.com/giuliopedicone02/OmniPrice.git
cd OmniPrice
docker compose up -d                     # RabbitMQ (opzionale)
(cd backend/omniprice && ./mvnw spring-boot:run)
(cd frontend && npm install && npm run dev)
```

## Flusso di lavoro

1. Crea un branch dedicato a partire da `main`:
   ```bash
   git checkout -b feat/nome-funzionalita
   ```
2. Applica le modifiche mantenendo lo stile del codice circostante.
3. Verifica che i test passino:
   ```bash
   cd backend/omniprice && ./mvnw test
   ```
4. Assicurati che il frontend compili:
   ```bash
   cd frontend && npm run build
   ```
5. Apri una Pull Request verso `main` descrivendo cosa cambia e perché.

## Convenzioni

- **Commit**: messaggi brevi e descrittivi, preferibilmente in stile
  [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`,
  `docs:`, `refactor:`, `test:`).
- **Backend**: rispetta la suddivisione in package esistente
  (`controller`, `service`, `dto`, `messaging`, `cluster`, `distributed`, …).
  Ogni design pattern è documentato nei commenti della classe corrispondente.
- **Frontend**: componenti Vue 3 con Composition API; stile con classi Tailwind.
- **Sicurezza**: non committare segreti reali. La chiave JWT nel repository è
  solo per lo sviluppo locale.

## Segnalare bug

Apri una issue includendo: passi per riprodurre, comportamento atteso vs.
osservato, log rilevanti e ambiente (SO, versioni di Java/Node).
