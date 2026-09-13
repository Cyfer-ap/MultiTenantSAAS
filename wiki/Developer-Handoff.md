# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Product Experience & Work Management Enrichment**

The next implementation slice is **Global Search**.

## Read first

Inside the Wiki:

1. [[Architecture]]
2. [[Authorization]]
3. [[Security-and-Authentication]]
4. [[Roadmap]]
5. [[Testing-and-CI]]

Inside the repository:

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `HANDOFF.md`
4. `guides/current_architecture.md`
5. `guides/ENGINEERING_STANDARDS.md`
6. the focused guide for the domain being changed

## Engineering rule from this point forward

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Global Search should be the first new domain implemented under this standard.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- protected authorization permissions remain non-delegable
- public APIs expose DTOs rather than persistence entities
- applied Flyway migrations remain append-only
- provider secrets remain server-side
- retryable/concurrent flows consider idempotency and locking
- new search/analytics/automation/AI paths must filter through tenant and authorization boundaries before exposing results

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the current product-enrichment phase. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and the repository `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.
