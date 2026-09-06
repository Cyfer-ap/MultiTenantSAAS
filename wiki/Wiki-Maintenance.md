# Wiki Maintenance

GitHub Wiki pages are stored in a separate Git repository:

```text
Cyfer-ap/MultiTenantSAAS.wiki.git
```

The main repository therefore keeps `wiki/*.md` as the **canonical, reviewable source**. Do not treat long-lived edits made directly in the Wiki UI as authoritative.

## Automatic validation and publishing

Wiki synchronization is managed by:

```text
.github/workflows/wiki-sync.yml
```

The workflow watches:

- `wiki/**`
- `scripts/publish-wiki.ps1`
- `.github/workflows/wiki-sync.yml`

On a pull request, it runs a **no-push validation** of the canonical Wiki source and publishing script. Draft/unmerged documentation is never published.

After the change reaches `main`, the workflow validates again and then uses the repository `GITHUB_TOKEN` to publish the merged source to `MultiTenantSAAS.wiki.git`.

The publish step configures a GitHub Actions bot commit identity and invokes:

```powershell
.\scripts\publish-wiki.ps1
```

The publisher:

1. validates required source pages
2. clones the live Wiki repository
3. synchronizes every Markdown page from `wiki/`
4. removes live Markdown pages that no longer exist in source
5. runs `git diff --cached --check`
6. commits only when a difference exists
7. pushes the live Wiki repository

## One-time Wiki initialization

GitHub must expose the `.wiki.git` repository before automation can use it. If the Wiki has never been initialized, create a temporary `Home` page once through the repository Wiki UI. After that, the automated workflow owns synchronization from `wiki/`.

## Manual fallback / preview

Automatic publishing is the normal path. For troubleshooting or local preview:

```powershell
# Validate and preview differences without committing or pushing.
.\scripts\publish-wiki.ps1 -NoPush

# Manual authenticated publish if CI ever needs a fallback.
gh auth setup-git
.\scripts\publish-wiki.ps1
```

In GitHub Actions the script reads `GITHUB_TOKEN` automatically. Locally, when no token is supplied, it continues to use the user's normal Git credential configuration.

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
_Sidebar.md   -> custom navigation sidebar
_Footer.md    -> custom footer
```

## Editing policy

1. edit `wiki/*.md` in the main repository
2. let the PR Wiki validation run
3. merge through the normal protected-main workflow
4. let `Wiki Sync` publish the merged content automatically
5. if an emergency direct Wiki edit is unavoidable, copy it back into `wiki/` immediately or the next source sync will overwrite it

Prefer Wiki page links over links to source Markdown so readers remain inside the Wiki.
