# Application Theme System

The frontend uses one persistent light/dark appearance preference across public authentication, tenant workspaces, and the system-administration console.

## Behavior

- Dark mode is the default for new browsers.
- The selected mode is stored locally and restored without a page reload.
- The theme control is available from authenticated headers and public authentication screens.
- Supporting browsers use a view-transition reveal from the toggle position; reduced-motion users and unsupported browsers receive an immediate low-motion fallback.
- Desktop tenant and system-admin navigation can collapse into a persistent icon rail. Mobile navigation remains a temporary drawer.

## Visual direction

Both modes use neutral metallic surfaces rather than saturated product colors. Dark mode uses graphite, black, gunmetal, and restrained silver highlights. Light mode uses brushed-silver/off-white surfaces with charcoal controls and text. Cards, tables, forms, menus, dialogs, Kanban lanes, alerts, and navigation rely on semantic MUI tokens so pages inherit the active mode consistently.

Page-specific UI should prefer theme tokens such as `background.paper`, `text.secondary`, `divider`, and `action.hover` over hard-coded light/dark colors. Reserve explicit colors for semantic status states or intentionally mode-aware branded surfaces.
