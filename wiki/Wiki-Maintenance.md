# Wiki Maintenance

The GitHub Wiki is published from version-controlled source under `wiki/` in the main repository.

The live Wiki repository is:

```text
Cyfer-ap/MultiTenantSAAS.wiki.git
```

Do not treat long-lived direct edits in the Wiki UI as authoritative. The next source sync will overwrite them.

## Documentation ownership

The Wiki is **reader-facing documentation**, not a second internal checkpoint system.

Repository-side ownership remains:

- `readme.md` — stable public overview
- `CHECKPOINT.md` — current application/repository status
- `HANDOFF.md` — current resume instructions
- `AGENTS.md` — persistent engineering contract
- `guides/current_architecture.md` — canonical technical architecture
- `guides/ENGINEERING_STANDARDS.md` — technical-health debt register and engineering rules
- focused guides — detailed domain contracts
- `guides/Wild_Thoughts.md` — exploratory idea vault

Wiki ownership is:

- `Home.md` — reader-facing landing page
- `Architecture.md` — reader-facing architecture summary
- domain pages — reader-facing subsystem documentation
- `Roadmap.md` — product direction/deferred milestones
- `Developer-Handoff.md` — concise Wiki navigation/resume pointer

Do not copy full checkpoint/handoff text into Wiki pages. Summarize stable concepts and point readers to the owning document when repository-internal detail is needed.

## Automatic validation and publishing

Synchronization is managed by:

```text
.github/workflows/wiki-sync.yml
```

The workflow watches Wiki source and publisher changes.

On pull requests it performs a **no-push validation** so draft/unmerged Wiki changes are never published.

After changes reach `main`, it validates again and publishes the merged source to `MultiTenantSAAS.wiki.git` using:

```powershell
.\scripts\publish-wiki.ps1
```

The publisher:

1. validates required source pages
2. clones the live Wiki repository
3. synchronizes Markdown pages from `wiki/`
4. removes live Markdown pages that no longer exist in source
5. runs `git diff --cached --check`
6. commits only when a difference exists
7. pushes the live Wiki repository

## Editing policy

1. edit `wiki/*.md` in the main repository
2. update only the Wiki page that owns the changed concept
3. let PR Wiki validation run
4. merge through the normal PR/CI workflow
5. let Wiki Sync publish merged `main`
6. if an emergency direct Wiki edit is unavoidable, immediately copy it back into `wiki/`

Avoid “update every Wiki page” milestone sweeps unless those pages genuinely own changed facts.

## Required and special files

The publisher validates:

```text
Home.md
_Sidebar.md
_Footer.md
Wiki-Maintenance.md
```

GitHub renders these specially:

```text
Home.md       -> Wiki landing page
_Sidebar.md   -> navigation
_Footer.md    -> footer
```

## Manual fallback / preview

Automatic publishing is the normal path.

```powershell
# Validate and preview without pushing.
.\scripts\publish-wiki.ps1 -NoPush

# Manual authenticated publish only when CI needs a fallback.
gh auth setup-git
.\scripts\publish-wiki.ps1
```

In GitHub Actions the publisher uses `GITHUB_TOKEN`. Local execution uses the developer's configured Git credentials when no explicit token is supplied.
