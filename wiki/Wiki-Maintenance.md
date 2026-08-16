# Wiki Maintenance

## Why the repository `wiki/` folder does not automatically appear in the Wiki tab

GitHub Wiki pages are stored in a separate Git repository:

```text
Cyfer-ap/MultiTenantSAAS.wiki.git
```

The main repository's `wiki/` directory is therefore a **version-controlled source copy**, not the live Wiki repository.

## Source of truth

Use:

```text
wiki/*.md
```

in the main repository as the canonical editable source.

Changes should normally go through the protected-main pull-request workflow.

## One-time Wiki initialization

Before GitHub exposes the `.wiki.git` repository, create the first Wiki page once through the GitHub Wiki UI.

The initial page can be a temporary `Home` page; the publishing script will replace it from the canonical source.

## Publishing

After Wiki initialization and after the documentation PR is merged:

```powershell
git switch main
git pull --ff-only origin main

gh auth setup-git
.\scripts\publish-wiki.ps1
```

The script:

1. validates required source pages
2. clones `MultiTenantSAAS.wiki.git` into a temporary directory
3. synchronizes all Markdown pages from `wiki/`
4. removes Markdown pages that no longer exist in the source folder
5. runs `git diff --check`
6. commits only when there are changes
7. pushes the live Wiki repository

Use preview mode to inspect without pushing:

```powershell
.\scripts\publish-wiki.ps1 -NoPush
```

## Special files

GitHub renders these specially:

```text
Home.md       -> Wiki landing page
_Sidebar.md   -> custom navigation sidebar
_Footer.md    -> custom footer
```

## Editing policy

Avoid making long-lived edits directly in the GitHub Wiki UI because they can drift from the source folder.

If an emergency Wiki edit is made directly, copy the same change back into `wiki/` before the next source sync.

## Links

Prefer Wiki page links over links to the source Markdown files so readers stay inside the Wiki.
