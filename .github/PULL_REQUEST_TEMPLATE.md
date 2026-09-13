## Summary

Describe the user/product/engineering outcome of this PR.

## Architecture and boundaries

Check applicable items; explain intentional exceptions in the notes below.

- [ ] New functionality lives in an explicit domain/feature module, or this PR does not add a new domain.
- [ ] Cross-domain access uses narrow services/contracts/events rather than another domain's repositories as the normal integration path.
- [ ] This change does not casually add dependencies to an already-large service; any required orchestration growth is justified or decomposed.
- [ ] No new circular domain dependency or shared/common dumping-ground logic was introduced.
- [ ] Important architectural invariants gained regression/architecture tests where practical.

## Security and tenancy

- [ ] Tenant-owned data is scoped/ownership-validated before exposure.
- [ ] Backend authorization remains authoritative; frontend checks are UX only.
- [ ] Search/analytics/automation/AI/reporting paths do not fetch broad unauthorized data and filter it afterward.
- [ ] Secrets/provider-internal identifiers are not exposed or logged.

## Data and API contracts

- [ ] Schema changes use a new append-only Flyway migration; no applied migration was rewritten.
- [ ] Public API changes use explicit DTOs/validation and update affected client types/API tests in the same PR.
- [ ] Potentially unbounded reads are paginated/bounded.
- [ ] Concurrency/idempotency/locking was considered for retryable or competing mutations.

## Validation

- [ ] Focused tests cover the important happy/failure paths.
- [ ] Cross-tenant/authorization regressions are tested when relevant.
- [ ] Frontend loading/empty/error/success states are covered when relevant.
- [ ] Manual smoke testing is documented below when browser/provider/storage behavior cannot be fully automated.
- [ ] Applicable CI/security/static-analysis gates are expected to pass before merge.

## Documentation

- [ ] Only the document that owns a changed fact was updated; no new duplicate checkpoint/progress/handoff/status mirror was introduced.
- [ ] `CHECKPOINT.md`, `HANDOFF.md`, architecture/engineering guides, focused docs, or Wiki pages were updated only when their owned information actually changed.

## Notes / intentional exceptions / manual verification

N/A unless needed.
