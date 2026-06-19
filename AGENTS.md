# Review Guidance for Codex Agents

## Project Context

This repository is a training mini app for Story S1: AI-TRAIN-M1, GPT Review Foundation.

The main purpose of the story is to help the developer reach, learn, and apply AI integration patterns in a simple Spring Boot + React app. Reviews should therefore focus first on whether the app demonstrates the intended AI learning flow:

1. Load or create maintenance ticket data.
2. Send ticket data to GPT through Spring AI.
3. Request structured JSON output.
4. Parse the AI response into a typed backend shape.
5. Validate the AI output.
6. Store the AI review result.
7. Display the stored result or a clean error in React.

## Review Principle

Do not treat every extension as a story violation.

If an added feature is outside the story scope but does not block the S1 AI flow, classify it as an extension or scope note. Examples include authentication, extra dashboard UI, or additional ticket management actions when they do not prevent the required AI review demo.

Treat something as a real gap only when it breaks, weakens, or obscures the story's learning objective or acceptance criteria.

## Severity Guide

Use these severity labels in story reviews:

- Critical: Blocks the core S1 AI review behavior or contradicts a required structured AI contract.
- High: Breaks required acceptance proof, reproducibility, persistence, validation, clean error handling, or clearly obscures the required Spring AI/OpenAI path.
- Medium: Deviates from the written contract but the app can still demonstrate the core AI learning flow.
- Light: Extra scope, polish issue, naming mismatch, documentation gap, or non-blocking extension.

## S1-Specific Review Rules

- Authentication is not required by S1, but if it works and does not block the ticket review flow after login, classify it as Light extension/scope drift rather than a blocking gap.
- Extra providers or direct HTTP client experiments should be flagged when they make it unclear whether the S1 AI review uses Spring AI OpenAI as required.
- Seed data matters because S1 expects a reproducible demo with the named sample tickets. Missing seed tickets should be treated as a real gap unless the README clearly documents an accepted manual setup path.
- The confidence field must match the story contract. S1 expects `LOW`, `MEDIUM`, or `HIGH`; numeric confidence is a structured-output contract mismatch.
- Exact API path differences such as `/tickets` versus `/api/tickets` are usually Medium unless the reviewer is doing strict endpoint-contract grading.
- Missing-ticket errors should return clean `404` responses with a user-safe message.
- Automated tests must not call real OpenAI. Prefer mocked AI behavior for validator and service tests.

## Preferred Gap Table Format

When producing a final S1 review, use this table structure:

| Gap ID | Severity | Area | Source requirement | Files / methods | Problem | Why it violates story/design | Required fix | How to verify |
|---|---|---|---|---|---|---|---|---|

For the `Required fix` column, write junior-friendly steps rather than a dense paragraph.

## Review Tone

Be fair and practical. The story is a learning milestone, not a production audit. Reward working AI flow and explain fixes in a way a junior developer can execute.
