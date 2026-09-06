# Wiki Maintenance

GitHub Wiki pages are stored in a separate Git repository:

```text
Cyfer-ap/MultiTenantSAAS.wiki.git
```

The main repository therefore keeps `wiki/*.md` as the **canonical, reviewable source**. Do not treat long-lived edits made directly in the Wiki UI as authoritative.

## Automatic publishing

The live GitHub Wiki is synchronized automatically by:

```text
.github/workflows/wiki-sync.yml
```

The workflow runs after a push to `main` when any of these change:

- `wiki/**`
- `scripts/publish-wiki.ps1`
- `.github/workflows/wiki-sync.yml`

It uses the repository `GITHUB_TOKEN`, configures a GitHub Actions bot commit identity and invokes:

```powershell
.\scripts\publish-wiki.ps1
```

The publisher clones `MultiTenantSAAS.wiki.git`, copies every Markdown page from the canonical `wiki/` directory, removes live Markdown pages that no longer exist in source, runs `git diff --cached --check`, and commits/pushes only when a difference exists.

The workflow runs only from `main`; pull requests do **not** publish unmerged Wiki content.

## One-time Wiki initialization

GitHub must expose the `.wiki.git` repository before automation can use it. If the Wiki has never been initialized, create the first temporary `Home` page once through the repository Wiki UI. After that, the automatic workflow owns synchronization from `wiki/`.

## Manual fallback / preview

Automatic publishing is the normal path. For troubleshooting or local preview:

```powershell
# Preview the difference without committing or pushing.
.\scripts\publish-wiki.ps1 -NoPush

# Manual authenticated publish after gh auth setup-git, if ever needed.
gh auth setup-git
.\scripts\publish-wiki.ps1
```

In GitHub Actions the script reads `GITHUB_TOKEN` automatically. Locally, it continues to work with the user's normal Git credential configuration when no token is supplied.

## Required and special files

The publisher validates these required source pages:

```text
Home.md
_Sidebar.md
_Footer.md
Wiki-Maintenance.md
```

GitHub renders these specially:

```text
Home.md       -> Wiki landing page
_Sidebar.md   -> custom navigation sidebar
_Footer.md    -> custom footer
```

## Editing policy

1. edit `wiki/*.md` in the main repository
2. merge through the normal protected-main PR workflow
3. let `Wiki Sync` publish the merged content automatically
4. if an emergency direct Wiki edit is unavoidable, copy the same change back into `wiki/` immediately or the next automatic sync will overwrite it

Prefer Wiki page links over links to source Markdown so readers remain inside the Wiki.
