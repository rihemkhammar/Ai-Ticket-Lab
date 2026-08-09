# AI Ticket Lab — Agentic Edition

Full-stack maintenance ticket management system augmented with AI: structured GPT analysis, retrieval-augmented generation (RAG), multi-agent orchestration, and human-in-the-loop (HITL) validation — built to explore safe, production-minded LLM integration patterns end to end.

> **Stack:** Java 25 · Spring Boot 3.5 · Spring AI · **LangGraph4j** · **OpenRouter** (chat) · **Hugging Face** (embeddings + reranking) · PostgreSQL + pgvector · React 19 · Vite · Tailwind · JWT · Docker · Flyway

---

## Overview

Most "AI ticket assistant" demos stop at a single prompt-and-response call. This project goes further: it's a layered system where each capability is added deliberately, with its own safety guarantees, tests, and failure modes — closer to how an LLM feature would actually be shipped in production.

The system lets a technician create and review maintenance tickets, and get AI assistance at increasing levels of autonomy:

1. **Direct AI review** — a structured, schema-validated analysis of a single ticket
2. **RAG-grounded review** — the same analysis, but backed by retrieved evidence from a knowledge base instead of the model's own memory
3. **Agentic investigation** — a read-only agent that calls backend tools to gather context before synthesizing a recommendation
4. **Human-in-the-loop review** — the agent's draft pauses at a persistent checkpoint until a human approves, rejects, or requests a revision
5. **Multi-ticket triage pipeline** — a compiled **LangGraph4j** `StateGraph` that classifies, orders, dispatches, and routes an entire queue of tickets through the pipeline above

At every layer, the model's output is **never trusted blindly**: backend validators enforce structural rules regardless of what the prompt asked for.

---

## Architecture

```
┌──────────────────┐        HTTP/JWT        ┌────────────────────────────────────┐
│  React Frontend   │  ───────────────────►  │        Spring Boot Backend         │
│  (Vite + Tailwind)│  ◄───────────────────  │                                     │
└──────────────────┘                        │  ┌───────────────────────────────┐  │
                                             │  │ AI Review   (Spring AI Chat)  │  │
                                             │  │ RAG Review  (pgvector + hybrid │  │
                                             │  │              search + rerank) │  │
                                             │  │ Agent        (tool calling)   │  │
                                             │  │ HITL         (checkpoints)    │  │
                                             │  │ Triage       (LangGraph4j)    │  │
                                             │  └───────────────────────────────┘  │
                                             └───────────────┬─────────────────────┘
                                                              │ JPA / Flyway
                                                              ▼
                                                   ┌─────────────────────┐
                                                   │ PostgreSQL + pgvector│
                                                   └─────────────────────┘
                                                              │
                                              ┌───────────────┴───────────────┐
                                              ▼                                ▼
                                   ┌─────────────────────┐        ┌─────────────────────┐
                                   │  OpenRouter (chat)   │        │ Hugging Face (embed  │
                                   │  gpt-oss-20b         │        │  + rerank)            │
                                   └─────────────────────┘        └─────────────────────┘
```

**Backend package layout** (`com.genai.java.spring.*`):

| Package | Responsibility |
|---|---|
| `ticket`, `auth`, `user` | Core domain: tickets, JWT auth, user accounts |
| `aireview` | Baseline structured AI review + prompt-injection defense advisors |
| `rag` | Knowledge base, chunking, embeddings, pgvector storage, hybrid retrieval, reranking, evidence-grounded review |
| `agent` | Read-only tool-calling agent (ticket lookup, evidence retrieval, prior reviews, recommendation boundaries) |
| `hitl` | Human-in-the-loop checkpoints, decision endpoints, revision/retry logic |
| `triage` | Multi-ticket pipeline orchestrated as a **LangGraph4j** `StateGraph` |
| `observability` | Agent run tracing (tool calls, statuses) exposed to the frontend |
| `shared` | Cross-cutting advisors and utilities |

**Frontend** (React 19 + Vite + Tailwind): technician dashboard, ticket detail with tabs for AI/RAG/Agent review, HITL review panel (pending / revision / finalized / rejected states), knowledge article browser, and a triage pipeline view with per-ticket step tracking.

---

## Key capabilities

### 1. Structured AI review with prompt-injection defense
Every ticket analysis returns a schema-validated object (`summary`, `confidence`, `limitations`, `needsHumanReview`). Generation runs through **OpenRouter** against `openai/gpt-oss-20b` (an open-weight model, routed rather than called directly against a single vendor). The system prompt explicitly treats ticket text as **untrusted input** — the model is instructed never to follow instructions embedded in a ticket's title or description. Backend advisors (`StructuralValidationAdvisor`, `HumanReviewSafetyAdvisor`) re-verify these constraints independently of the prompt, so a model that "disobeys" still can't produce an unsafe result: the review is stored as `FAILED` instead.

### 2. Retrieval-Augmented Generation (RAG)
A small knowledge base (maintenance articles on conveyors, motors, pumps, sensors, safety) is chunked using a **locally embedded HuggingFace tokenizer** (token-accurate, no network calls at chunking time). Chunks are embedded via the **Hugging Face inference router** (`sentence-transformers/all-MiniLM-L6-v2`, 384 dimensions) and indexed in **pgvector**. Retrieval combines:
- vector similarity search (cosine distance, dedicated IVFFlat index)
- full-text search (PostgreSQL `tsvector`/GIN)
- fused via **Reciprocal Rank Fusion (RRF)**
- refined with **cross-encoder reranking** via Hugging Face (`cross-encoder/ms-marco-MiniLM-L-6-v2`), with automatic fallback to the hybrid order if the reranking call fails
- **neighbor stitching**, so a retrieved chunk is padded with its immediate neighbors to reduce context loss at chunk boundaries

Every evidence reference the model cites is validated against the chunks actually retrieved — the model cannot invent a source.

### 3. Read-only agentic investigation
A tool-calling agent gathers context before producing a recommendation, using deterministic backend orchestration (not free-form model autonomy):

- `TicketLookupTool` — ticket details
- `TicketEvidenceTool` — RAG evidence retrieval
- `PreviousAiReviewTool` — prior AI reviews for the ticket
- `TicketRecommendationBoundaryTool` — explicit allowed/forbidden actions

The agent can inspect, summarize, and draft a recommendation — it can **never** close a ticket, change its status, or claim a maintenance action was performed. Any output making such a claim is rejected and the run is stored as `FAILED`. Every tool call is traced (name, input, output, status) and shown in the UI — without exposing the model's internal reasoning.

### 4. Human-in-the-loop (HITL) review
Before finalization, the agent's draft pauses at a **persistent checkpoint** (`agent_review_checkpoint`), reloadable after a refresh or backend restart. A human reviewer can:

- **Approve** → run becomes `FINALIZED`
- **Reject** → run becomes `REJECTED`
- **Request revision** → a new draft and checkpoint are created (one revision cycle allowed; a malformed revised draft triggers a single repair attempt before failing)

Approval never mutates the ticket — `HumanReviewDecisionService` has no dependency on `TicketService`, making ticket mutation structurally impossible from this layer, not just policy-forbidden.

### 5. Multi-agent triage pipeline (LangGraph4j)
Instead of hand-written control flow, the ticket triage pipeline is modeled as a compiled **LangGraph4j `StateGraph`**:

```
START → Classify → Order → Dispatch ─┬─ (critical/high) → Investigate → Review ─┐
                                      └─ (else) ─────────────→ Review ───────────┤
                                                                                  ▼
                                                                    Rules → HITL checkpoint
                                                                                  │
                                                            (queue non-empty) ────┘── loop to Dispatch
                                                                          (queue empty) → END
```

Each node (`ClassifyTicketsNode`, `OrderQueueNode`, `DispatchNextTicketNode`, `InvestigationNode`, `ReviewNode`, `RulesNode`, `HitlCheckpointNode`) is a plain, independently unit-testable Spring bean; `TriageGraphConfig` only wires them together. This reproduces a previously hand-written orchestration loop exactly, but replaces it with an explicit, inspectable graph — including conditional branching (skip investigation for low-severity tickets) and a queue-processing loop.

---

## Safety principles applied throughout

- **Untrusted input everywhere** — ticket text, retrieved documents, and (future) attachments are always treated as data, never as instructions.
- **Backend validation over prompt compliance** — every layer re-checks the model's structural claims (`needsHumanReview`, non-empty `limitations`, evidence references that actually exist) rather than trusting the prompt to be followed.
- **No silent autonomy** — the agent and the triage pipeline can recommend, draft, and investigate, but structurally cannot close tickets, approve reviews, or claim actions were performed.
- **Traceability without leaking reasoning** — tool calls and pipeline steps are logged and shown to users; internal model reasoning (chain-of-thought) is not exposed.
- **Deterministic tests without live model calls** — the AI provider is mocked throughout the test suite; safety behavior is verified against fixed inputs/outputs, not live API responses.

---

## Tech stack

**Backend**
- Java 25, Spring Boot 3.5, Spring Data JPA, Spring Security (JWT)
- Spring AI (`ChatClient`, structured output, Advisors, OpenAI-compatible starter pointed at **OpenRouter**)
- **LangGraph4j** — `StateGraph` orchestration for the triage pipeline
- PostgreSQL + **pgvector** (cosine similarity, IVFFlat index, hybrid full-text search)
- **Hugging Face** inference router — embeddings (`sentence-transformers/all-MiniLM-L6-v2`) and cross-encoder reranking (`ms-marco-MiniLM-L-6-v2`)
- HuggingFace `tokenizers` (local, embedded tokenizer for chunk sizing — no network call)
- Flyway migrations, Maven

**Frontend**
- React 19, Vite, React Router, Tailwind CSS
- Axios, react-toastify
- D3 / topojson (data visualization)

**Infrastructure**
- Docker / Docker Compose (PostgreSQL + pgvector)
- OpenRouter (`openai/gpt-oss-20b`) for chat generation
- Hugging Face inference router for embeddings and reranking

---

## Getting started

### Prerequisites
- JDK 25 ([jdk.java.net/25](https://jdk.java.net/25/))
- Node.js / npm
- Docker

### 1. Clone
```bash
git clone <this-repo-url>
cd Ai-Ticket-Lab
```

### 2. Start PostgreSQL (with pgvector)
```bash
docker compose up -d
```

### 3. Configure API keys
```bash
export OPENAI_API_KEY=your-openrouter-api-key      # used against OpenRouter's OpenAI-compatible API
export EMBEDDING_API_KEY=your-huggingface-api-key  # used for embeddings + reranking via Hugging Face
```

### 4. Run the backend
```bash
cd backend
mvn spring-boot:run
```

### 5. Index the knowledge base (required for RAG)
```bash
curl -X POST http://localhost:8080/api/articles/index
```
(or click **Index Articles** from the Knowledge Articles page in the frontend)

### 6. Run the frontend
```bash
cd frontend
npm install
npm run dev
```

The frontend proxies `/api` to `http://localhost:8080` (see `vite.config.js`).

### Test credentials
| Username | Password | Role |
|---|---|---|
| `demo_technician` | `pass123` | TECHNICIAN |

---

## Testing

```bash
./mvnw test
```

The suite covers structured review validation, prompt-injection resistance, chunking/embedding/retrieval correctness, RAG evidence validation, agent tool behavior and forbidden-claim rejection, HITL state transitions and non-mutation guarantees, and the triage graph's routing logic — all against a mocked `ChatClient`, with no live calls to OpenRouter or Hugging Face.

---

## Known limitations

- No production authentication hardening, reviewer roles, or permissions
- No real maintenance-action execution or ticket-status mutation from any AI/agent layer (by design)
- Only one HITL revision cycle per run
- Reranking depends on an external HuggingFace API (no local reranker model)
- No MCP integration, no observability dashboard, no production deployment tooling

---

## Why this project

This started as a structured learning path (single AI call → prompt safety → RAG → tool-calling agent → HITL → multi-agent orchestration) and became a reasonably complete reference for **shipping LLM features with guardrails that don't depend on the model behaving well** — every trust boundary is enforced in code, not just in the prompt.