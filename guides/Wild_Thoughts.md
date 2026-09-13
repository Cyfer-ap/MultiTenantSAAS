# Wild Thoughts

> **Purpose:** A living product-idea vault for MultiTenantSAAS.
>
> This is **not a roadmap or commitment**. It records completed ideas, partial foundations, useful product gaps, differentiated experiments, and ideas worth resisting.
>
> Snapshot: **2026-09-13, post-PR #126**.

## Status legend

- ✅ **Built** — implemented at application level and no longer a future idea.
- 🟡 **Partial** — meaningful foundation exists, but the broader product idea is not complete.
- ⬜ **Open** — not meaningfully implemented yet.
- 🧪 **Experiment** — intentionally unusual/differentiated; validate before committing.
- ⏸ **Deferred** — valuable, but deliberately not in the current product-enrichment phase.
- ❌ **Resist** — complexity or product behavior we should avoid unless a real requirement changes the decision.

---

# 1. Product Direction — Updated

The platform has already moved well beyond the original idea of "a project-management app with tenant isolation."

The strongest long-term direction remains:

> **A secure organizational operating platform where people, work, communication, time, permissions, integrations, services, and knowledge can be composed differently for different organizations.**

The important change in this snapshot is that most of the **platform foundation** is no longer hypothetical. The next period should emphasize **usefulness, discoverability, work-management depth, personalization, and product character** before returning to operational hardening.

## 1.1 Platform core status

| Capability | Status | Current state |
|---|---|---|
| Tenant isolation | ✅ | Shared-schema tenant boundaries are enforced throughout the application. |
| Tenant/system-admin separation | ✅ | Separate tenant and system-admin identity/control planes. |
| Authentication/session lifecycle | ✅ | JWT/browser sessions, refresh handling, password reset/change, lockout, logout/logout-all. |
| Email-first workspace discovery | ✅ | Users discover available workspaces without typing tenant UUIDs. |
| Invitations and user lifecycle | ✅ | Tenant invitations and user administration exist. |
| Organization hierarchy | ✅ | Organizational units, assignments and relationship-aware authorization exist. |
| Permission-oriented authorization | ✅ | Roles, permissions, assignments, validity and scoped evaluation exist. |
| Explain Access | ✅ | Enforcement evaluator can explain granted/denied access with stable reasoning. |
| Authorization delegation | ✅ | Bounded one-level delegation, provenance, expiry/revocation and source revalidation. |
| Audit | ✅ | Tenant/platform audit surfaces exist. |
| Subscription/entitlements/quotas | ✅ | Lifecycle restrictions, plans, quotas and API usage limits exist. |
| External billing | ✅ | Stripe/Razorpay adapters, hosted checkout, webhook lifecycle, cancellation/reconciliation and managed catalogs exist. |
| API keys | ✅ | Tenant API keys and external API metering/plan limits exist. |
| Outbound webhooks | ✅ | Tenant-configurable signed durable webhooks, history and replay exist. |
| Enterprise OIDC SSO | ✅ | Tenant IdP configuration, verification, secure runtime, optional/required policy and admin UX exist. |
| Durable notifications | ✅ | In-app + email records, preferences, retry/backoff and notification UI exist. |
| Object storage attachments | ✅ | S3/R2-compatible task/comment attachments with hardened lifecycle exist. |
| Task collaboration | ✅ | Comments, mentions, replies, pins, activity and deep links exist. |
| Projects/tasks basics | ✅ | Assignee, status, priority, due date, lifecycle and sorting/filtering foundations exist. |
| Generic files/knowledge workspace | 🟡 | Attachments exist; general file browser, docs and knowledge base do not. |
| General asynchronous job platform | 🟡 | Notifications/webhooks/billing contain durable async patterns; no single generic job product exists. |
| Tenant configuration/personalization | 🟡 | Settings foundations exist, but timezone/locale/branding/user personalization are not a complete product layer. |

## 1.2 What should *not* be mistaken for differentiation

These remain useful features, but by themselves they are now table stakes in serious work-management products:

- Kanban/list/calendar views
- global search
- command palette
- dashboards
- templates
- forms
- custom fields
- workflow automation
- AI summaries
- AI meeting notes
- AI agents
- enterprise search
- routine task routing

We should still build the ones that make the product usable. The differentiators should sit **on top of** these fundamentals.

---

# 2. Core Product Features Still Missing

The infrastructure core is strong. The remaining **core** gaps are mainly product-core and work-management capabilities.

## 2.1 Multi-workspace identity experience — 🟡

Email-first discovery exists, but the ideal experience is still broader:

- post-login workspace switcher,
- one smooth identity experience across memberships,
- tenant-aware cache reset on switch,
- workspace favorites/recent workspaces,
- optional workspace slugs,
- eventually custom domains.

This is a high-value UX gap because users should think in workspace names, never UUIDs.

## 2.2 Global search + command palette — ⬜

A universal `Ctrl/Cmd + K` should search authorized:

- projects,
- tasks,
- users,
- organizational units,
- comments,
- attachments,
- audit/admin destinations where permitted.

It should also execute actions:

- create task,
- create project,
- invite user,
- open subscription,
- switch workspace,
- jump to a person/project,
- run saved views.

Search and commands should share one mental model.

## 2.3 Personal work hub / My Work — ⬜

One place answering:

> **What needs my attention?**

Include:

- assigned to me,
- due/overdue,
- mentioned me,
- waiting for my response,
- recently viewed,
- favorites,
- items I am blocking,
- later: approvals/reviews/tickets.

This is more important to daily usability than another admin screen.

## 2.4 Better task/work views — 🟡

Already built:

- status,
- priority,
- due date,
- assignee,
- comments/replies/mentions,
- pins,
- attachments,
- activity history.

Still open:

- Kanban,
- calendar view,
- timeline/Gantt,
- saved views,
- labels,
- subtasks,
- dependencies,
- recurring tasks,
- watchers/followers,
- bulk actions,
- quick-edit interactions,
- milestones/sprints where useful.

## 2.5 Role/capability-aware dashboard — 🟡

Different users should receive different useful home screens:

- individual contributor: my work, deadlines, mentions, recent context;
- manager: workload, blockers, overdue/unassigned work, team signals;
- tenant admin: people, invitations, quota, authorization/security summary;
- system admin: platform tenants, subscriptions and operational visibility.

Widgets should be permission-aware rather than tied only to coarse role names.

## 2.6 Saved views, filters, favorites and recents — ⬜

Users should be able to preserve how they work:

- "My urgent work"
- "Backend tasks"
- "Due this week"
- "Waiting on customer"
- favorite projects
- recent items

This is a relatively small feature family with disproportionately large UX value.

## 2.7 Calendar / everything-time view — ⬜

Start with tasks and project dates before attempting a full meeting platform:

- task due dates,
- project deadlines,
- milestones,
- reminders,
- later events/resources/leave.

Timezone-aware rendering should be designed correctly from the beginning.

## 2.8 Project/task templates — ⬜

Create reusable structures for onboarding, releases, research, audits, client work, etc.

Templates become much more powerful after subtasks/dependencies/custom fields exist.

## 2.9 Custom fields + forms — ⬜

Moderate tenant customization without replacing real domain models.

Potential field types:

- text,
- number,
- date,
- boolean,
- select/multi-select,
- user,
- project,
- asset later.

Forms can then create structured tasks/requests.

## 2.10 Workflow/approval automation — ⬜

A reusable engine for:

- trigger,
- condition,
- action,
- approval,
- escalation,
- reminder,
- audit/explanation.

Do not start with an over-general BPMN clone. Begin with understandable product workflows.

## 2.11 Knowledge/documents beyond attachments — 🟡

Attachments exist, but the platform still lacks a first-class knowledge layer:

- documents/articles,
- owners,
- review dates,
- version history,
- search,
- links to projects/tasks/decisions,
- later policy acknowledgment.

## 2.12 Product analytics/reporting — 🟡

Operational and billing visibility exist, but user-facing work analytics are still shallow.

Useful initial metrics:

- work completed over time,
- overdue work,
- workload distribution,
- cycle time,
- project health,
- collaboration/activity trends,
- explainable drill-down.

Avoid opaque "productivity scores."

## 2.13 Import/export and bulk productivity — ⬜

A practical SaaS core feature family that the old vault under-emphasized:

- CSV import/export,
- bulk task update,
- bulk assignment/status changes,
- safe preview before import,
- downloadable error rows,
- permission-aware export.

## 2.14 Onboarding and contextual help — ⬜

Role-aware onboarding, good empty states, contextual help and sample/demo data matter when the platform becomes broad.

## 2.15 MFA/passkeys and richer session/device controls — ⏸

Still a meaningful security-core gap, but deliberately not the immediate product-enrichment focus.

---

# 3. Audit of the Original Thematic Ideas

The original vault predated many major implementation milestones. This is the updated status of its main sections.

| Original section | Status | Update |
|---|---|---|
| 1. Bigger Product Direction | 🟡 | Platform-core direction is real; modular/vertical product layer remains future. |
| 2. Better Tenant and Identity Experience | 🟡 | Email-first workspace discovery is built; smooth post-login switching/global identity/custom domains remain open. |
| 3. Timezone-Aware Everything | ⬜ | Still worth treating as a platform capability. |
| 4. Role/Capability-Aware Dashboards | 🟡 | Separate control planes exist; rich capability-aware/personal dashboards remain open. |
| 5. Shared Calendar / Everything Time | ⬜ | Open. |
| 6. Chat and Real-Time Collaboration | 🟡 | Task collaboration is strong; channels/DMs/presence/live messaging remain open. |
| 7. Ticketing / Helpdesk | ⬜ | Open. |
| 8. Login, Sessions, Activity, Attendance | 🟡 | Strong auth/session/security foundation exists; attendance/presence/device UX remain open. |
| 9. Notifications | 🟡 | Durable in-app/email/preferences built; digest/live/web-push/attention controls remain open. |
| 10. Background Jobs / Reliable Events | 🟡 | Durable retries/leases/idempotency exist in notifications/webhooks/billing; generic scheduling/job product remains open. |
| 11. Email / External Communication | 🟡 | Provider-backed email delivery exists; many future message types/digests remain open. |
| 12. Projects and Tasks | 🟡 | Strong basics + collaboration built; richer work-management depth remains open. |
| 13. Files, Documents and Knowledge | 🟡 | R2 attachments built; file workspace/knowledge base/versioning remain open. |
| 14. Workflow and Approval Engine | ⬜ | Open. |
| 15. Assets / Facilities / Booking | ⬜ | Open. |
| 16. Workforce / HR-like Features | 🟡 | Users/org hierarchy exist; workforce product modules remain open. |
| 17. CRM / External Relationships | ⬜ | Open. |
| 18. Analytics and Reporting | 🟡 | Admin/billing/usage visibility exists; end-user analytics/reporting remains open. |
| 19. Search | ⬜ | High-priority product gap. |
| 20. API Keys / Developer Platform | 🟡 | Tenant API keys + usage limits built; service accounts/OAuth apps/richer scopes remain open. |
| 21. Outbound Webhooks / Integrations | ✅ | Core tenant-configurable webhook lifecycle/history/replay delivered through PR #112. Third-party app integrations remain optional. |
| 22. Billing / Subscription Depth | ✅ | Core external billing/catalog/cancellation/reconciliation/history is complete through PR #106. Advanced add-ons/usage pricing remain optional. |
| 23. Custom Fields | ⬜ | Open. |
| 24. Organizational Hierarchy | ✅ | Core hierarchy/relationship/scoped authorization exists. Multi-site/enterprise-group modeling can extend it later. |
| 25. Security Expansion | 🟡 | OIDC SSO, delegation, validity, Explain Access and SSO break-glass built; MFA/passkeys/SAML/SCIM/access reviews remain open. |
| 26. UI/UX Modernization | 🟡 | Modern React/MUI app exists; command palette, richer tables, saved views, dark mode/accessibility polish remain open. |
| 27. Branding / Personalization | 🟡 | Foundations/settings exist; complete user/tenant personalization remains open. |
| 28. Useful AI | ⬜ | No need to rush; should come after search/knowledge/workflows are strong. |

---

# 4. Audit of the Original "Wild" Ideas

The original #29–#100 ideas remain useful. Their implementation state is now:

## Already meaningfully realized or partly realized

- 🟡 **#32 Permission Simulator** — Explain Access now solves current-state "why can/can't this user access this?"; hypothetical impact simulation is still open.
- 🟡 **#44 Explain This Screen** — authorization explanations exist; generic contextual help is not built.
- 🟡 **#45 Contextual Workspaces** — project/task detail surfaces combine work and collaboration; broader object-centric workspaces remain open.
- 🟡 **#46 Universal Activity Timeline** — task activity history exists; a cross-entity timeline does not.
- 🟡 **#47 Human-Friendly Audit Logs** — audit surfaces exist; more narrative/entity-rich rendering can improve them.
- 🟡 **#57 Tenant Health Check** — some admin/billing/integration visibility exists; unified entropy/health checks do not.
- 🟡 **#59 Access Expiry by Default** — assignment validity and delegation expiry exist; default-expiry policy and review UX remain open.
- 🟡 **#60 Break-Glass Access** — SSO has a guarded password-capable tenant-admin recovery path; emergency temporary privilege elevation is not built.
- 🟡 **#61 Privacy Zones** — scopes can isolate project/org areas; specialized sensitive-data compartments remain open.
- 🟡 **#63 Data Lifecycles** — several entities support status/archive/retirement/history; no generic retention/legal-hold/anonymization framework exists.
- 🟡 **#66 Entity Linking Everywhere** — projects/tasks/comments/attachments/notifications already link context; universal typed relations remain open.
- 🟡 **#83 Minimum Necessary Data** — backend scope/authorization supports this principle; field-level projection/redaction is still open.
- 🟡 **#89 Progressive Complexity** — permission-aware navigation hides inaccessible areas; module enablement/onboarding progression remains open.
- 🟡 **#90 Contextual Navigation** — project/task context exists; richer context-specific navigation remains open.
- 🟡 **#100 Platform Personality** — the UI has professional foundations; more deliberate product character can be added without gimmicks.

## High-value original ideas still open

These remain especially worth preserving:

- ⬜ **#29 Organizational Memory**
- ⬜ **#30 Decision Records Everywhere**
- ⬜ **#31 Time Travel / Historical State Explorer**
- ⬜ **#33 Organization Graph**
- ⬜ **#34 Bus-Factor Radar**
- ⬜ **#35 Meeting Debt**
- ⬜ **#36 Quiet Organization Mode**
- ⬜ **#37 Smart Handoffs**
- ⬜ **#38 What Am I Blocking?**
- ⬜ **#39 Reverse Dependency View**
- ⬜ **#40 Risk Inbox**
- ⬜ **#41 Scenario / Sandbox Mode**
- ⬜ **#42 Synthetic Tenant Generator**
- ⬜ **#43 Feature Laboratory**
- ⬜ **#48 Consent-Based Workload Heatmap**
- ⬜ **#49 Organization Pulse**
- ⬜ **#50 Smart Daily Brief**
- ⬜ **#51 End-of-Day Handoff**
- ⬜ **#52 Follow-the-Sun Operations**
- ⬜ **#53 Deadline Reality Check**
- ⬜ **#54 Do-Not-Schedule Context**
- ⬜ **#55 Conflict-Aware Resource Scheduling**
- ⬜ **#56 Escalation Without Spam**
- ⬜ **#58 Security Posture Dashboard**
- ⬜ **#62 Redaction Instead of Binary Access**
- ⬜ **#64 Organization Templates**
- ⬜ **#65 Module Composer**
- ⬜ **#67 One Global Create Button**
- ⬜ **#68 Action-Oriented Search**
- ⬜ **#69 Personal Work Queue**
- ⬜ **#70 Smart Snooze**
- ⬜ **#71 Waiting-On Relationships**
- ⬜ **#72 Healthier Presence Statuses**
- ⬜ **#73 Team Agreements**
- ⬜ **#74 Decision Expiry**
- ⬜ **#75 Automatic Postmortem Skeleton**
- ⬜ **#76 No-Blame Incident Mode**
- ⬜ **#77 What Changed Since I Was Away?**
- ⬜ **#78 Attention Heatmap**
- ⬜ **#79 Organizational Drift Detection**
- ⬜ **#80 Explainable Automation**
- ⬜ **#81 Automation Guardrails**
- ⬜ **#82 Operational Digital Twin**
- ⬜ **#84 Organizational Memory With Expiration**
- ⬜ **#85 Cross-Tenant Collaboration Spaces**
- ⬜ **#86 Data Boundary Visualization**
- ⬜ **#87 Safe Share Preview**
- ⬜ **#88 Universal Undo Where Safe**
- ⬜ **#91 Multi-Language / Locale-Aware Platform**
- ⬜ **#92 Accessibility Profiles**
- ⬜ **#93 Operational Modes**
- ⬜ **#94 What Is Safe to Delete?**
- ⬜ **#95 Offboarding Wizard**
- ⬜ **#96 Role-Aware Onboarding Journey**
- ⬜ **#97 Knowledge Ownership**
- ⬜ **#98 Stale Work Detection**
- ⬜ **#99 Noise-vs-Signal Analytics**

---

# 5. Near-Term Product-Enrichment Backlog

This section is deliberately closer to buildable product work than the wild experiments below.

## Tier A — small/medium scope, high visible value

1. **Global search foundation**
2. **Command palette**
3. **Favorites + recently viewed**
4. **My Work / personal attention queue**
5. **Saved filters/views**
6. **Dashboard refresh with permission-aware widgets**
7. **Quick-create menu**
8. **Bulk task actions**
9. **Breadcrumb/context navigation cleanup**
10. **Role-aware onboarding + useful empty states**
11. **User preferences: theme, density, timezone basics**
12. **Better notification-center grouping**

## Tier B — medium scope, meaningful work-management depth

1. **Kanban board**
2. **Calendar/deadline view**
3. **Subtasks**
4. **Task dependencies**
5. **Labels/tags**
6. **Recurring tasks**
7. **Project milestones**
8. **Project templates**
9. **Project overview/health page**
10. **Workload view**
11. **CSV import/export**
12. **Rich user/project selectors and avatars**

## Tier C — large product modules

1. **Custom fields + forms**
2. **Workflow/approval engine**
3. **Knowledge/document module**
4. **User-facing analytics/reporting**
5. **Project/team discussions or contextual chat**
6. **Ticketing/service-request module**
7. **Automation builder**
8. **Shared calendar/resource booking**
9. **Cross-entity relationships**
10. **Permission-aware AI assistant after search/knowledge are mature**

---

# 6. New Differentiated / Weird Ideas

The following ideas are intentionally less standard than boards, dashboards, AI summaries or workflow builders.

## 104. Context Capsule — 🧪

Create a shareable internal snapshot of "everything needed to understand this right now":

- task/project,
- relevant comments,
- recent decisions,
- attachments,
- current owner,
- due date,
- blockers,
- linked entities.

The capsule is permission-aware and can expire.

Useful for handoffs, review requests and "look at this" moments without forcing someone to reconstruct context manually.

## 105. Permission Lens — 🧪

A temporary UI overlay for admins/debugging that explains *why* buttons, tabs and records are available or unavailable.

Examples:

> Edit disabled — missing `project.update` for this project.

> Billing visible — granted through TENANT_ADMIN assignment.

> Mutation blocked — authorization passes, but workspace is read-only because of subscription lifecycle.

This extends Explain Access into understandable product UX.

## 106. "Why Now?" Notifications — 🧪

Every important notification should answer:

- Why did I receive this?
- Why now?
- What changed?
- What action is expected?
- What happens if I ignore it?

This is more useful than merely showing event text.

## 107. Attention Escrow — 🧪

Instead of "mute notifications," users can place low-priority interruptions into escrow during focus periods.

Urgent events pass through; everything else is released as a compact context-aware bundle later.

Goal: protect attention without losing responsibility.

## 108. Change Blast-Radius Preview — 🧪

Before changing a deadline, owner, project status or workflow state, preview downstream effects:

> Moving Phoenix by 7 days affects 12 tasks, 3 people and 2 dependent milestones.

This is the work-management equivalent of a database migration preview.

## 109. Alternate-Reality Planning — 🧪

A user can temporarily change dates/owners/dependencies in a private scenario without saving them.

Then compare:

- current plan,
- proposed plan,
- affected deadlines,
- workload changes,
- newly blocked work.

Only explicitly applying the scenario changes real data.

## 110. Role Ghost / "View As" Without Impersonation — 🧪

Admins can preview what a role or selected user *would be able to see* without creating a session as that person.

Use authorization simulation, never credential/session impersonation.

Useful for permission design, onboarding and support.

## 111. Work Receipt — 🧪

Important actions generate a compact receipt:

> You moved Project Phoenix from Active to Archived.
> 14 tasks remain open.
> 3 members will lose active-project navigation.
> Outbound event emitted: `project.archived`.
> Undo: available for 30 seconds / unavailable.

Makes consequential changes understandable and traceable.

## 112. Support Capsule — 🧪

A user can click **Create support bundle** and generate a safe diagnostic package containing:

- route/screen,
- application version,
- request correlation IDs,
- failed request category,
- current permission reason,
- browser basics,
- redacted state.

Never include passwords, tokens, secrets or unrelated tenant data.

This can make bug reports dramatically better without screen-sharing.

## 113. Assumption Register — 🧪

Projects/decisions can explicitly record assumptions:

> Vendor API will be ready by October 1.

Each assumption has:

- owner,
- evidence,
- confidence,
- review date,
- linked work.

If an assumption becomes false, the system shows affected work.

## 114. Contradiction Radar — 🧪

Detect explicit contradictions between current records:

- two active decisions specify different launch dates,
- project deadline differs from approved milestone,
- document says one owner while the project has another,
- two policies conflict.

Do not silently resolve contradictions; surface them for humans.

## 115. Decision Collision Guard — 🧪

Before recording a new decision, show existing active decisions on the same subject/scope.

Purpose: stop organizations from accumulating mutually incompatible decisions in different places.

## 116. Regret Check / Outcome Review — 🧪

When making a major decision, optionally record:

> What do we expect this change to improve?

After 30/60/90 days, ask:

> Did it actually work?

This converts decisions into learning loops rather than permanent assumptions.

## 117. Context Compression Checkpoint — 🧪

When a thread/task/project becomes long, allow users to create a **human-approved canonical checkpoint**:

- current state,
- decisions,
- unresolved questions,
- next actions,
- links to evidence.

Future users start from the checkpoint instead of reading 180 comments.

AI may draft it, but a human owns the canonical version.

## 118. Memory Quarantine — 🧪

Superseded knowledge should not simply disappear or remain equally trusted.

Quarantined content remains historically available but search/AI clearly marks:

> Superseded on 12 Sep 2026 by Decision D-104.

This protects organizational memory from stale truth.

## 119. Project Necromancer — 🧪

When an abandoned project becomes relevant again, generate a revival brief:

- why it stopped,
- last known state,
- unresolved blockers,
- previous owners,
- stale dependencies,
- decisions that may no longer be valid,
- what must be revalidated.

A weird name for a genuinely useful capability.

## 120. Responsibility Gap Detector — 🧪

Find important objects with ambiguous accountability:

- critical task has no assignee,
- project has no active owner,
- document has no reviewer,
- integration has no responsible team,
- approval has no valid approver.

Focus on missing responsibility, not employee scoring.

## 121. Dependency Debt — 🧪

Not just "task A depends on task B."

Highlight brittle structures:

- chains that are too long,
- one task blocking many unrelated outcomes,
- dependencies owned by inactive users,
- cross-team handoffs with no fallback.

Think technical debt, but for coordination.

## 122. One-Click "I'm Blocked" Packet — 🧪

A user clicks **I'm blocked** and the platform asks only for the missing piece.

It automatically packages:

- current task,
- blocker category,
- relevant dependencies,
- last attempts,
- owner/team who can likely unblock it.

Then route it to the right place instead of creating vague "blocked" statuses.

## 123. Async Meeting Replacement — 🧪

When creating a meeting, optionally ask:

> Is the goal a decision, update, brainstorm or coordination?

For simple update/decision meetings, offer an async packet instead:

- context,
- question,
- deadline for responses,
- decision owner.

Do not block meetings; provide a lower-cost alternative.

## 124. Bureaucracy Detector — 🧪

Look for workflows where process has grown without clear value:

- approval step almost never rejects,
- the same person approves twice,
- requests wait longer in approval than in execution,
- a workflow gained many steps over time.

This should diagnose process friction, not automatically remove controls.

## 125. Friction Budget — 🧪

Teams can define acceptable process friction for routine work:

> Standard purchase under ₹10,000 should require at most two approvals.

The system warns when workflow configuration exceeds the agreed budget.

Useful for preventing "enterprise" from becoming synonymous with slow.

## 126. Reality-vs-Plan Drift — 🧪

Compare declared priorities/plans against actual work signals:

> Phoenix is marked "top priority," but only 3% of completed work and no recent decisions relate to it.

This is organizational planning feedback, not employee monitoring.

## 127. Context Orphan Detector — 🧪

Find useful information that is disconnected from work:

- important file linked nowhere,
- decision with no affected project,
- long discussion with no resulting action,
- task with no project/context,
- document with no owner.

Help users reconnect or archive orphaned context.

## 128. Commitment Ledger — 🧪

People make commitments in comments and meetings that never become tasks.

Allow lightweight commitments:

> "I will send the revised proposal by Friday."

A commitment is smaller than a task but still has owner/date/context and can later be converted into work.

## 129. Permission-Aware Clipboard / Share Guard — 🧪

Before sharing a link or bundle with another user/external collaborator, check whether they can actually access the linked material.

Example:

> Vendor X can open the report but cannot open 2 internal attachments referenced inside it.

Prevent "shared but useless" and accidental exposure scenarios.

## 130. Entity Passport — 🧪

Any major entity can expose one concise passport:

- identity,
- owner,
- lifecycle/status,
- scope/access summary,
- important links,
- recent changes,
- dependencies,
- external integrations,
- retention/history basics.

Useful for projects, users, APIs, integrations, assets and future tickets.

## 131. Staleness Gravity — 🧪

Important unresolved items should gradually become harder to ignore even when nobody is actively touching them.

Do this based on explicit risk/deadline/ownership signals, not arbitrary activity metrics.

## 132. Missing-Context Detector — 🧪

Warn when work is structurally incomplete:

- urgent task with no owner,
- decision with no rationale,
- deadline with no responsible person,
- handoff with no next owner,
- document with no review date.

This is "linting" for organizational work.

## 133. Team API for Humans — 🧪

Each team exposes a simple service contract:

- what we own,
- how to request help,
- what counts as urgent,
- expected response time,
- escalation path,
- current constraints.

This reduces coordination friction without requiring everyone to know the org chart.

## 134. Intent Mode — 🧪

Instead of navigating modules, users start with an intent:

- Launch a release
- Onboard a customer
- Investigate an incident
- Prepare an audit
- Plan a research experiment

The UI temporarily composes relevant projects, tasks, people, files, deadlines and actions into one cockpit.

This is a possible long-term differentiator for a horizontal platform.

## 135. Human Checkpoints for Automation/AI — 🧪

Allow policies such as:

- AI may draft but not send externally,
- automation may assign but not deactivate users,
- agent may edit tasks but not change authorization,
- billing/subscription mutations always require human confirmation.

Treat human review as a first-class workflow primitive rather than an afterthought.

## 136. Automation Circuit Breaker — 🧪

If an automation suddenly matches far more events than its historical baseline, pause it automatically or require confirmation.

Example:

> This rule normally affects 4–8 tasks/day. It is about to affect 1,842 tasks.

This goes beyond pre-deployment simulation and protects against runtime accidents.

## 137. "Can the Product Say No?" — 🧪

Before creating another task/project/meeting/request, the product may suggest:

- an existing duplicate,
- a better destination,
- an existing decision,
- an async alternative,
- a simpler action.

The user remains in control.

Differentiation can come from reducing unnecessary work, not only helping users create more of it.

## 138. AI Disagreement Mode — 🧪

If AI is added later, it should not merely confirm the user's narrative.

For high-value decisions, an optional mode can surface:

- contrary evidence,
- stale assumptions,
- missing sources,
- unresolved contradictions.

The output must cite only authorized source material and clearly distinguish evidence from inference.

## 139. Trustworthy AI Abstention — 🧪

The assistant should be allowed to say:

> I cannot answer confidently because the only supporting document is superseded and the current owner has not confirmed a replacement.

A system that knows when organizational context is stale may be more valuable than one that always generates an answer.

## 140. Outcome vs Output — 🧪

Projects can record the intended outcome separately from the activity plan.

Example:

Output: migrate billing checkout.

Outcome: reduce failed subscription starts below 2%.

Later reporting can ask whether shipped work actually changed the intended outcome.

---

# 7. Product Principles to Preserve

## 7.1 Authorization before intelligence

Any search, analytics, automation or AI feature must operate on authorized data rather than receiving broad tenant data and attempting to filter later.

## 7.2 Explain important decisions

Whenever the product makes or suggests a consequential decision, prefer:

- why,
- source,
- scope,
- affected entities,
- reversibility.

## 7.3 Help users create less unnecessary work

The product should not optimize for maximum tasks, messages, notifications or meetings.

Useful software can also:

- prevent duplicates,
- group noise,
- expose stale work,
- suggest async alternatives,
- reveal unnecessary process.

## 7.4 Avoid surveillance

Do not build:

- keystroke tracking,
- screenshots,
- secret activity scoring,
- opaque employee productivity ranks,
- manipulative presence metrics.

Use work objects, explicit responsibilities and aggregate organizational signals instead.

## 7.5 Progressive complexity

Simple teams should be able to use Projects/Tasks/People without understanding every enterprise subsystem.

Advanced features should appear as teams need them.

## 7.6 Do not confuse configurable with unstructured

Custom fields/workflows/modules should extend clear domain models, not turn the database into arbitrary JSON controlled by UI configuration.

## 7.7 Keep strange features falsifiable

A weird idea is good only if we can define:

- who benefits,
- what problem it solves,
- how to test it,
- what evidence would make us remove it.

---

# 8. Things We Should Still Resist

❌ Kafka without an event-volume problem.

❌ Redis without a real shared-cache/distributed-state problem.

❌ Kubernetes for appearance.

❌ Premature microservices.

❌ Elasticsearch/vector infrastructure before search requirements justify it.

❌ AI as an authorization or security boundary.

❌ Generic "AI agent" features merely because competitors have them.

❌ Employee surveillance and opaque productivity scores.

❌ Notification growth as an engagement metric.

❌ Infinite customization that destroys consistency and testability.

❌ Pretending every organization is simply a collection of projects/tasks.

❌ Full payroll/medical/financial domain implementations without domain-specific requirements and compliance design.

---

# 9. Current Direction

For the next development phase, prioritize **Product Experience & Work Management Enrichment** before returning to the deferred operations/DR milestone.

A reasonable progression is:

```text
Discoverability
  -> global search
  -> command palette
  -> recents/favorites

Personal productivity
  -> My Work
  -> saved views
  -> better dashboard

Work-management depth
  -> Kanban/calendar
  -> subtasks/dependencies/labels
  -> recurring work/templates

Adaptability
  -> custom fields/forms
  -> workflow/approval engine
  -> knowledge/documents

Insights and differentiation
  -> analytics
  -> risk/context features
  -> selected experiments from #104+
```

Operations/disaster recovery, load/failure testing and production R2 verification remain important, but are deliberately **deferred from the immediate product sequence** while the application is enriched with user-facing capability.

---

## Parking Lot

Use this for ideas discovered during implementation that are not ready for a numbered entry.

- [ ] Cross-module keyboard shortcut system
- [ ] Project archive/revival UX
- [ ] Permission-aware export bundles
- [ ] User-configurable start page
- [ ] Smart duplicate detection across tasks/projects

---

**Status:** Living Idea Vault

**Last audited:** 2026-09-13

**Rule:** Nothing here becomes a roadmap item merely because it exists here.
