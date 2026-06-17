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

---

## 📖 Story S1 — AI-TRAIN-M1: GPT Review Foundation

### Objectif

Créer une application permettant à un utilisateur de soumettre un ticket de maintenance et de demander une analyse automatique par IA.

Cette story constitue un environnement d'apprentissage destiné à comprendre l'intégration de l'IA dans une application Java moderne avant l'implémentation de fonctionnalités avancées prévues dans les futures versions du projet.

### Fonctionnalités Utilisateur

L'utilisateur peut :

* Consulter la liste des tickets de maintenance
* Afficher le détail d'un ticket
* Lancer une analyse IA via le bouton **Run AI Review**
* Recevoir une réponse structurée générée par GPT
* Consulter l'historique des analyses réalisées

---

## 📋 Roadmap du Milestone

Le développement est découpé en trois phases.

### Phase 0 — Setup du Projet

* Initialisation Spring Boot
* Initialisation React + Vite
* Configuration PostgreSQL
* Mise en place Docker
* Configuration Flyway

### Phase 1 — Première Intégration IA

* Création des tickets
* Appel à OpenAI via Spring AI
* Génération d'une analyse simple

### Phase 2 — Analyse Structurée

* Structured Output
* Validation des réponses IA
* Advisors Spring AI
* Gestion avancée des erreurs
* Persistance des résultats


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

## 📸 Capture d'écran

### AI Ticket Lab






