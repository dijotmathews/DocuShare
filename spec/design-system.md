# Design System

> Theme, component usage, and visual standards. Reference this when building or reviewing UI.

---

## 1. Theme

- **Base theme:** Vaadin Aura
- **Custom CSS:** `src/main/resources/META-INF/resources/styles.css`

**Aura and Lumo are two different, incompatible design systems.** This project uses **Aura**. Do not use `--lumo-*` CSS variables — they belong to the Lumo theme and must not be mixed with Aura. Use `--aura-*` variables for Aura-specific properties (typography, shadows) and `--vaadin-*` variables for base properties shared across all themes (spacing, radius, colors).

**Always use Aura theme variables instead of hard-coded values** (e.g., `--aura-font-size-xs` through `--aura-font-size-xl` for font sizes). Do not use hardcoded `px`, `rem`, or `em` values when an Aura variable exists. This ensures consistency with the Vaadin Aura theme and allows global adjustments through theme customization.

---

## 2. Color Palette

Aura computes all color variations automatically from a small set of base properties. Override these instead of hard-coding hex values.

| Token | Default | Usage |
|-------|---------|-------|
| `--aura-accent-color-light` | Blue | Primary actions, focus rings, selection highlights (light mode) |
| `--aura-accent-color-dark` | Blue | Primary actions, focus rings, selection highlights (dark mode) |
| `--aura-neutral` / `-light` / `-dark` | Dark gray / off-white | Text, borders, default UI chrome |
| `--aura-red` | Red | Error states, destructive actions |
| `--aura-orange` | Orange | Warnings |
| `--aura-green` | Green | Success states, confirmations |
| `--aura-blue` | Blue | Informational, links |
| `--aura-yellow` | Yellow | Caution, highlights |
| `--aura-purple` | Purple | Decorative accents |

Derived read-only tokens (do not override directly):
- `--aura-accent-contrast-color` — high-contrast text on accent backgrounds
- `--aura-accent-text-color` — accent-derived text color with good contrast
- `--aura-accent-border-color` — border tinted with accent color
- `--aura-accent-surface` — surface tinted with accent color
- `--aura-red-text`, `--aura-green-text`, etc. — palette text variants with better contrast

Base style tokens (shared across all themes):
- `--vaadin-text-color` — main text color
- `--vaadin-text-color-secondary` — secondary/muted text
- `--vaadin-text-color-disabled` — disabled state text
- `--vaadin-border-color` — prominent borders (3:1 contrast)
- `--vaadin-border-color-secondary` — subtle, non-essential borders
- `--vaadin-background-color` — base content background
- `--vaadin-background-container` — buttons, toolbars, highlighted areas
- `--vaadin-background-container-strong` — more prominent container background

Use accent class names (e.g. `.aura-accent-purple`) on `<html>` or individual components to swap accent color contextually.

---

## 3. Typography

Aura uses the **Instrument Sans** web font by default (`--aura-font-family-instrument-sans`), falling back to the system font stack.

| Token | Purpose |
|-------|---------|
| `--aura-font-family` | App-wide font family (set on `<body>`) |
| `--aura-base-font-size` | Base size (unitless number, represents M size in px) |
| `--aura-font-size-xs` through `-xl` | Computed font sizes (rem, rounded to nearest px) |
| `--aura-base-line-height` | Base line height (unitless, relative to font size) |
| `--aura-line-height-xs` through `-xl` | Computed line heights (rem, rounded to nearest 2px) |
| `--aura-font-weight-regular` | Normal body text |
| `--aura-font-weight-medium` | Emphasis, subheadings |
| `--aura-font-weight-semibold` | Headings, strong emphasis |
| `--aura-font-smoothing` | Set to `auto` to disable grayscale anti-aliasing |

Use Aura font-size tokens (`--aura-font-size-s`, etc.) instead of hard-coded `px`/`rem` values.

---

## 4. Spacing & Layout

Aura computes gap and padding from `--aura-base-size` (unitless, range 12–24). Use the resulting base style tokens:

| Token | Purpose |
|-------|---------|
| `--vaadin-gap-xs` through `-xl` | Space between elements in flex/grid layouts |
| `--vaadin-padding-xs` through `-xl` | Internal padding for containers and content areas |
| `--vaadin-padding-inline-container` | Horizontal padding for single-line containers (buttons, inputs) |
| `--vaadin-padding-block-container` | Vertical padding for single-line containers |

**Border radius** (computed from `--aura-base-radius`, unitless, range 0–10):

| Token | Purpose |
|-------|---------|
| `--vaadin-radius-s` | Small controls (should not become circles) |
| `--vaadin-radius-m` | Default component radius |
| `--vaadin-radius-l` | Large containers, cards, dialogs |

**Shadows** (Aura-specific):

| Token | Purpose |
|-------|---------|
| `--aura-shadow-xs` | Subtle elevation — buttons, inputs, checkboxes |
| `--aura-shadow-s` | Slight elevation — primary buttons, selected controls, cards |
| `--aura-shadow-m` | Clear elevation — overlays, notifications, dialogs |

**Surface colors** for visual hierarchy (read-only, computed):
- `--aura-surface-color` — semi-transparent elevated background
- `--aura-surface-color-solid` — opaque version
- Control with `--aura-surface-level` (number, higher = more elevation) and `--aura-surface-opacity` (default 0.5)

**Layout approach:** Use Vaadin `VerticalLayout` / `HorizontalLayout` (Flow) or flexbox/grid with `--vaadin-gap-*` / `--vaadin-padding-*` tokens. No hard-coded spacing values.

---

## 5. Component Standards

> Preferred Vaadin components and usage patterns. List components actually used or planned.

| Component | When to Use | Notes |
|-----------|-------------|-------|
| `LoginForm` | Login page (`/login`) | Centered on the page under the app name; error messages set via `LoginI18n`; forgot-password button hidden |
| `Button` | Primary and secondary actions | Primary variant for the main action; tertiary variant for header actions such as Logout |
| `Grid` | Tabular data display (e.g. client list, the admin's document list) | Always enable column sorting; set an empty-state text (or a dedicated empty-state panel); single selection opens the row in the side form; read-only lists collapse to a single summary column below 640px instead of scrolling sideways |
| `FormLayout` + fields | Add/edit forms in a side panel next to the grid | Mandatory fields show the required indicator; validation runs on the server and errors are shown on the fields plus a summary at the top of the form; a field's error clears as soon as it is edited |
| `Notification` | Success feedback after saving | Success variant, top-center, about 4 seconds |
| `TabSheet` | Splitting one list into fixed categories (e.g. the client's Filings / Financials / Computations / Misc) | Each tab label carries a count badge; opens on the first tab with content; an empty tab says so in a plain sentence |
| `Upload` | Selecting the file on the upload form | Single file, 25 MB max; button relabelled "Select file..." so it can't be confused with the form's "Upload" button |
| `Select` | Picking from a fixed list (e.g. Document Type) | Placeholder "Select a type"; labels from the enum |
| `ComboBox` | Picking a record from a searchable list (e.g. the client PAN on the upload form) | Items come from the database; typing filters by every shown attribute (PAN or client name), ignoring case; custom values not allowed; option shows the key with a secondary line |
| `ConfirmDialog` | Confirming a destructive action (overwrite a file, reset a password) | Confirm button uses the error + primary theme; the safe choice is the cancel button |
| Admin navigation | Header of every admin page and cards on the Admin Dashboard | One list (`AdminNavigation`) in a fixed order: Dashboard, Upload Documents, Clients, Documents. Header links are highlighted only on their own page; dashboard cards show an icon, the label and a one-line description |
| `Dialog` | Information the admin must note (e.g. a client's default password) | Doesn't close on outside click; primary "Done" button in the footer |

---

## 6. Responsive Behavior

- **Mobile** (< 640px): Single column, stacked layouts, full-width cards
- **Tablet** (640–1024px): Two-column grid, side-by-side content
- **Desktop** (> 1024px): Multi-column grid, admin grid+form side by side

