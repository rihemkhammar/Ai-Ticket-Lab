# AI Maintenance Ticket Review System

Application Full-Stack de gestion de tickets de maintenance avec analyse assistée par Intelligence Artificielle.

Le projet combine **Spring Boot**, **Spring AI**, **PostgreSQL** et **React** afin de permettre l'analyse automatique de tickets de maintenance via un modèle GPT.

**Projet de formation Full-Stack & IA**
*React + Spring Boot + Spring AI (aligné avec le parcours Udemy)*

---

## 🚀 Technologies Utilisées

### Backend

* Java 25
* Spring Boot 3.5.15
* Spring Data JPA
* Spring AI 1.1.7
* Flyway
* PostgreSQL
* Maven

### Frontend

* React
* Vite
* Axios

### Infrastructure

* Docker
* Docker Compose
* PostgreSQL

### Intelligence Artificielle

* OpenAI API
* GPT-4o-mini
* Spring AI ChatClient (structured output + Advisors)
---
## 🗺️ Roadmap des Stories

## 📖 Story S1 — AI-TRAIN-M1: GPT Review Foundation


**Objectif :** Créer la base de l'analyse IA de tickets de maintenance.




#### 📋 Roadmap du Milestone

Le développement est découpé en trois phases.

**Phase 0 — Setup du Projet**

**Phase 1 — Première Intégration IA**



**Phase 2 — Analyse Structurée**




---

## 🗄️ Database Migrations (Flyway)

Le projet utilise Flyway pour gérer les migrations SQL de la base de données.

***📦 V1 — V1__create_tickets.sql***
```SQL
CREATE TABLE tickets (
    id          BIGSERIAL    PRIMARY KEY,
    created_by  UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title       VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    status      VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

***📦 V2__create_ai_review_table.sql***
```SQL
CREATE TABLE ai_review (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    prompt_version VARCHAR(100) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    result_json TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```
---
## 📖  Story S2 — AI-TRAIN-M2: Prompt Quality and Safety

**Objectif :** Améliorer la qualité du prompt, défendre contre le prompt injection, et rendre les limitations IA visibles.


#### 📋 Roadmap du Milestone

Le développement est découpé en 4 phases.

**Phase 1 — Prompt Engineering**


**Phase 2 — Prompt Hacking & Defense**


**Phase 3 — Learning Notes**


**Phase 4 — LLM Limitations & Human Review**

---

## 🗄️ Database Migrations (Flyway)
***📦 V3__seed_tickets.sql***

***📦 V4__seed_prompt_injection_ticket.sql***

---

## 🏗️ Architecture

```text
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│                 │  HTTP   │                 │ Spring  │                 │
│  React Frontend │ ──────► │  Spring Boot    │   AI   │   OpenAI GPT    │
│  (Vite)         │ ◄────── │  Backend        │ ──────► │   (gpt-4o-mini) │
│                 │         │                 │ ◄────── │                 │
└─────────────────┘         └────────┬────────┘         └─────────────────┘
                                     │
                                     │ JPA / Flyway
                                     ▼
                            ┌─────────────────┐
                            │                 │
                            │   PostgreSQL    │
                            │   (Docker)      │
                            │                 │
                            └─────────────────┘
```

---
 
## 📦 Structure Backend
 
```
## 📦 Structure Backend

com.genai.java.spring
├── GenaiJavaSpringApplication.java
├── ticket
│   ├── Ticket.java
│   ├── TicketStatus.java
│   ├── TicketRepository.java
│   ├── TicketService.java
│   └── TicketController.java
├── aireview
│   ├── AiReview.java
│   ├── AiReviewStatus.java
│   ├── AiReviewRepository.java
│   ├── AiReviewService.java
│   ├── AiReviewController.java
│   ├── AiReviewParsingException.java
│   ├── AiReviewProviderException.java
│   ├── advisor
│   │   ├── AiReviewAdvisor.java
│   │   ├── AiReviewAdvisorChain.java
│   │   ├── AiReviewContext.java
│   │   ├── HumanReviewSafetyAdvisor.java
│   │   ├── PromptInjectionDefenseAdvisor.java
│   │   ├── StructuralValidationAdvisor.java
│   │   └── SystemPromptAdvisor.java
│   ├── dto
│   │   ├── TicketAiReviewResponse.java
│   │   ├── AiReviewApiResponse.java
│   │   └── ErrorResponse.java
│   └── prompt
│       └── TicketReviewPromptBuilder.java
├── auth
│   └── (...)
├── user
│   └── (...)
├── exception
│   └── (...)
└── config
    └── ChatClientConfig.java
```

 

## ▶️ Démarrage du Projet

### 1. Cloner le projet

```bash
git clone https://github.com/AutoApp-Solutions-Training-Org/Rihem-Training.git
cd ai-maintenance-ticket-review-system
```

### 2. Installer les dépendances Frontend

```bash
cd frontend
npm install
```

### 3. Démarrer PostgreSQL avec Docker

```bash
docker compose down
docker compose up -d
```
##  Configuration OpenAI API Key

Before running the backend, set your OpenAI API key:

```bash
export OPENAI_API_KEY=your-api-key-
```
### 4. Démarrer le Backend

```bash
mvn spring-boot:run
```

### 5. Démarrer le Frontend

```bash
cd frontend
npm run dev
```

---
### Connexion Frontend → Backend
 
Le frontend utilise un **proxy Vite** pour se connecter au backend :
 
```javascript
// vite.config.js
server: {
  proxy: {
    "/api": "http://localhost:8080"
  }
}
```

---
## 🔐 Authentification

L'application utilise une authentification par **JWT (JSON Web Token)**.

### Utilisateurs de test

| Username | Password | Rôle |
|----------|----------|------|
| `demo_technician` | `pass123` | `TECHNICIAN` |

---
 
## 🔌 API Endpoints
 
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/tickets` | Liste tous les tickets |
| `GET` | `/api/tickets/{ticketId}` | Détail d'un ticket |
| `POST` | `/api/tickets/{ticketId}/ai-review/basic` | Lancer une analyse IA |
 
 ---








