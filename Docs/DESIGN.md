# DESIGN.md — Visual & UX System

---

## Visual Direction

**Lane:** Dense, data-heavy, professional. Think Linear's interface meets Stripe's dashboard.

**Justification:** Argent's users are developers and technical founders. They interact with dashboards daily. They expect information density, fast scanning, and zero decorative elements. A warm, editorial style (like Stripe docs) would feel slow. A minimal, confident style (like Vercel) would feel too sparse for financial data. Linear's density + Stripe's trustworthiness is the right balance.

**Personality:**
- Confident, not flashy
- Dense, not cluttered
- Professional, not corporate
- Precise, not decorative

---

## Color System

### Brand Colors

| Token | Hex | Usage |
|---|---|---|
| `--brand-50` | `#EEF2FF` | Light brand tint |
| `--brand-100` | `#E0E7FF` | Hover states on light backgrounds |
| `--brand-200` | `#C7D2FE` | Focus rings |
| `--brand-400` | `#818CF8` | Interactive elements |
| `--brand-500` | `#6366F1` | Primary buttons, links, active states |
| `--brand-600` | `#4F46E5` | Button hover |
| `--brand-700` | `#4338CA` | Button active/pressed |

**Why Indigo?** It reads as technical and trustworthy without being the overused blue. It distinguishes Argent from generic SaaS products.

### Neutral Colors (Slate)

| Token | Hex | Usage |
|---|---|---|
| `--neutral-0` | `#FFFFFF` | Background (light mode) |
| `--neutral-25` | `#FCFCFD` | Subtle background |
| `--neutral-50` | `#F9FAFB` | Card backgrounds, table rows |
| `--neutral-100` | `#F2F4F7` | Borders, dividers |
| `--neutral-200` | `#EAECF0` | Input borders |
| `--neutral-300` | `#D0D5DD` | Disabled states |
| `--neutral-400` | `#98A2B3` | Placeholder text |
| `--neutral-500` | `#667085` | Secondary text |
| `--neutral-600` | `#475467` | Body text |
| `--neutral-700` | `#344054` | Headings |
| `--neutral-800` | `#1D2939` | Primary text |
| `--neutral-900` | `#101828` | Highest contrast text |
| `--neutral-950` | `#0C111D` | Background (dark mode) |

### Semantic Colors

| Token | Hex | Usage |
|---|---|---|
| `--success-50` | `#ECFDF3` | Success background |
| `--success-500` | `#12B76A` | Success icons, positive balances |
| `--success-700` | `#039855` | Success text |
| `--warning-50` | `#FEF3F2` | Warning background |
| `--warning-500` | `#F79009` | Warning icons, pending states |
| `--warning-700` | `#DC6803` | Warning text |
| `--error-50` | `#FEF3F2` | Error background |
| `--error-500` | `#F04438` | Error icons, failed transactions |
| `--error-700` | `#B42318` | Error text |
| `--info-50` | `#EFF8FF` | Info background |
| `--info-500` | `#2E90FA` | Info icons, links |
| `--info-700` | `#1570EF` | Info text |

### Balance-Specific Colors

| Token | Hex | Usage |
|---|---|---|
| `--balance-positive` | `#12B76A` | Positive balance / credit |
| `--balance-negative` | `#F04438` | Negative balance / debit |
| `--balance-neutral` | `#667085` | Zero balance |

---

## Typography

### Font Choices

**Primary:** `Inter` — UI text, labels, body copy
**Monospace:** `JetBrains Mono` — Financial data, codes, IDs, amounts

**Why Inter?** It's the best-performing UI font for screens. Excellent kerning at small sizes, clear at data-dense layouts. Not the most unique choice, but the most reliable for a product where readability of financial data is critical.

**Why JetBrains Mono?** Distinct characters (0 vs O, 1 vs l), ligatures for developers, feels technical without being gimmicky.

### Type Scale

| Token | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| `display-lg` | 36px / 2.25rem | 700 | 44px | Page titles (rare) |
| `display-sm` | 30px / 1.875rem | 600 | 38px | Section headers |
| `heading-lg` | 24px / 1.5rem | 600 | 32px | Card titles |
| `heading-sm` | 20px / 1.25rem | 600 | 28px | Subsection headers |
| `body-lg` | 16px / 1rem | 400 | 24px | Primary body text |
| `body-sm` | 14px / 0.875rem | 400 | 20px | Secondary text, table cells |
| `caption` | 12px / 0.75rem | 500 | 16px | Labels, badges, timestamps |
| `mono-lg` | 16px / 1rem | 500 | 24px | Large financial amounts |
| `mono-sm` | 13px / 0.8125rem | 400 | 20px | IDs, codes, data tables |

### Typography Rules

- **Financial amounts:** Always `JetBrains Mono`. Always right-aligned.
- **Wallet IDs, API keys, transaction IDs:** Always `JetBrains Mono`.
- **Body text:** Never smaller than 14px.
- **Line height:** 1.5x for body, 1.25x for headings.
- **Letter spacing:** Tighter for headings (-0.02em), normal for body.

---

## Spacing & Grid

### Base Unit

**4px** — All spacing is a multiple of 4.

### Spacing Scale

| Token | Value | Usage |
|---|---|---|
| `space-0` | 0px | — |
| `space-1` | 4px | Tight spacing (icon to text) |
| `space-2` | 8px | Compact spacing (inline elements) |
| `space-3` | 12px | Default spacing (form fields) |
| `space-4` | 16px | Comfortable spacing (card padding) |
| `space-5` | 20px | Section spacing |
| `space-6` | 24px | Card padding, table cell padding |
| `space-8` | 32px | Section gaps |
| `space-10` | 40px | Large section gaps |
| `space-12` | 48px | Page-level spacing |
| `space-16` | 64px | Major section dividers |

### Grid System

- **12-column grid** for page layouts.
- **Column width:** Flexible (fluid).
- **Gutter:** 24px.
- **Max content width:** 1280px (centered on large screens).

### Layout Breakpoints

| Breakpoint | Width | Columns | Usage |
|---|---|---|---|
| `sm` | 640px | 4 | Mobile landscape |
| `md` | 768px | 8 | Tablet |
| `lg` | 1024px | 12 | Desktop |
| `xl` | 1280px | 12 | Wide desktop |
| `2xl` | 1536px | 12 | Ultra-wide |

---

## Component System

### Buttons

**Variants:**

| Variant | Style | Usage |
|---|---|---|
| `primary` | Brand-500 bg, white text | Primary actions (Create, Submit) |
| `secondary` | Neutral-100 bg, neutral-700 text | Secondary actions (Cancel, Close) |
| `ghost` | Transparent bg, neutral-600 text | Tertiary actions (More, Filter) |
| `danger` | Error-500 bg, white text | Destructive actions (Delete, Revoke) |
| `link` | No bg, brand-500 text, underline | Inline navigation |

**Sizes:**

| Size | Height | Padding | Font |
|---|---|---|---|
| `sm` | 32px | 0 12px | 13px |
| `md` | 36px | 0 16px | 14px |
| `lg` | 40px | 0 20px | 14px |

**States:**
- Default → Hover (darken 5%) → Active/Pressed (darken 10%) → Disabled (opacity 50%)
- Focus ring: 2px brand-200 offset 2px

### Inputs

**Heights:** 36px (sm), 40px (md)

**States:**
- Default: Neutral-200 border, white bg
- Hover: Neutral-300 border
- Focus: Brand-500 border, brand-200 ring
- Error: Error-500 border, error-50 bg
- Disabled: Neutral-100 bg, neutral-300 border, neutral-400 text

**Labels:** 14px, neutral-700, above input with 4px gap.
**Helper text:** 12px, neutral-500, below input with 4px gap.
**Error text:** 12px, error-700, below input with 4px gap.

### Cards

- Border: 1px neutral-100
- Border radius: 8px
- Padding: 24px
- Background: white (light mode)
- No shadow by default
- Subtle shadow on hover (optional, for interactive cards)

### Tables

- **Header:** Neutral-50 bg, neutral-500 text, 12px uppercase, 500 weight
- **Row:** White bg, neutral-100 bottom border
- **Row hover:** Neutral-25 bg
- **Row selected:** Brand-50 bg
- **Cell padding:** 12px vertical, 16px horizontal
- **Sticky header:** Yes
- **Sortable columns:** Neutral-400 sort icon, brand-500 when active
- **Alignment:** Text left, numbers right, dates right, actions center

### Badges / Status

| Status | Background | Text | Dot |
|---|---|---|---|
| `active` | Success-50 | Success-700 | Success-500 |
| `pending` | Warning-50 | Warning-700 | Warning-500 |
| `failed` | Error-50 | Error-700 | Error-500 |
| `frozen` | Info-50 | Info-700 | Info-500 |
| `closed` | Neutral-100 | Neutral-600 | Neutral-400 |

Badge style: 6px 12px padding, 12px font, 16px line height, 4px border radius.

### Modals / Dialogs

- Overlay: rgba(0, 0, 0, 0.5)
- Modal bg: white
- Border radius: 12px
- Max width: 480px (sm), 640px (md), 800px (lg)
- Padding: 24px
- Title: heading-sm
- Actions: Right-aligned, secondary + primary buttons
- Close button: Top-right, ghost variant

### Toasts / Notifications

- Position: Top-right
- Width: 380px
- Border radius: 8px
- Padding: 16px
- Shadow: 0 4px 12px rgba(0, 0, 0, 0.15)
- Auto-dismiss: 5 seconds (success/info), manual dismiss (error)
- Left border: 4px semantic color

### Empty States

- Center-aligned within container
- Icon: 48px, neutral-300
- Title: heading-sm, neutral-700
- Description: body-sm, neutral-500
- Action: Primary button below description
- No illustrations or decorative images

### Loading States

- **Skeleton screens** preferred over spinners for content areas
- Skeleton: Neutral-100 bg, neutral-200 shimmer animation
- Spinner: 20px, brand-500, used only for button loading states
- Full-page loading: Skeleton layout, not centered spinner

---

## Layout Patterns

### Navigation

**Sidebar (fixed, left)**

```
┌──────────────────────────────────────────┐
│ ┌──────────┐                             │
│ │  ARGENT   │  ← Logo/brand             │
│ └──────────┘                             │
│                                          │
│ ┌──────────────────┐                     │
│ │  Dashboard       │  ← Nav items       │
│ │  Wallets         │     Icon + label    │
│ │  Transactions    │     Active: brand   │
│ │  Ledger          │     bg + brand text │
│ │  Audit Log       │                     │
│ │  Reports         │                     │
│ ├──────────────────┤                     │
│ │  Settings        │  ← Bottom section  │
│ │  API Keys        │                     │
│ └──────────────────┘                     │
│                                          │
│ ┌──────────────────┐                     │
│ │  [User avatar]    │  ← User menu      │
│ │  John Doe         │                     │
│ │  Owner            │                     │
│ └──────────────────┘                     │
└──────────────────────────────────────────┘
```

**Sidebar specs:**
- Width: 240px (expanded), 64px (collapsed)
- Background: neutral-950 (dark sidebar)
- Text: neutral-400 (inactive), white (active)
- Active indicator: Brand-500 bg, 4px left border
- Icons: Lucide, 20px
- Labels: 14px, 500 weight
- Border-right: 1px neutral-800

### Content Area

- Padding: 32px all sides
- Max width: 1280px (centered on ultra-wide)
- Section spacing: 32px between sections
- Card grid: 1-2-3 columns responsive

### Top Bar (within content area)

- Page title: display-sm or heading-lg
- Breadcrumbs: body-sm, neutral-500
- Actions: Right-aligned buttons
- Filters: Below title, horizontal row

### Mobile / Tablet

- Sidebar collapses to icon-only (64px) on tablet
- Sidebar becomes bottom nav on mobile (not in V1 scope)
- Tables become card-based layouts on mobile (not in V1 scope)
- V1 targets tablet (768px+) and desktop only

---

## Micro-Interactions & Motion

### Principles

1. **Motion communicates state change, not decoration.** Every animation should help the user understand what just happened.
2. **Fast over smooth.** 150ms is the default. Users in a dashboard are productive, not browsing.
3. **Subtle over dramatic.** No bouncing, no elastic easing. Purposeful, not playful.

### Duration Scale

| Token | Duration | Usage |
|---|---|---|
| `instant` | 0ms | Toggle states |
| `fast` | 100ms | Button press, hover |
| `normal` | 150ms | Modal open, dropdown, tooltip |
| `slow` | 250ms | Page transitions, complex animations |

### Easing

| Token | Value | Usage |
|---|---|---|
| `ease-in` | cubic-bezier(0.4, 0, 1, 1) | Elements leaving screen |
| `ease-out` | cubic-bezier(0, 0, 0.2, 1) | Elements entering screen |
| `ease-in-out` | cubic-bezier(0.4, 0, 0.2, 1) | Elements moving within screen |

### What Animates

- **Modal open/close:** Scale from 0.95 to 1 + fade, 150ms ease-out
- **Dropdown open/close:** Fade + translate-y from -4px to 0, 100ms ease-out
- **Toast enter:** Slide in from right + fade, 150ms ease-out
- **Toast exit:** Fade out, 100ms ease-in
- **Table row hover:** Background color transition, 100ms
- **Button press:** Scale to 0.98, 50ms
- **Page transitions:** None (instant, no fade between pages)
- **Skeleton shimmer:** 1.5s infinite linear animation

### What Does NOT Animate

- Number values (balance changes instantly, no counting animation)
- Page loads (content appears immediately)
- Table data (rows appear instantly)
- Navigation (instant switch)

---

## Anti-Patterns to Avoid

The following are explicitly banned in Argent's UI:

1. **No generic gradient hero sections.** This is a dashboard, not a landing page.
2. **No emoji as icons.** Use Lucide icon set consistently. No 🚀💰📊 in the UI.
3. **No centered single-column marketing layout.** Dense sidebar + content area is the pattern.
4. **No Inter font with rounded-xl everything.** Use Inter with 8px border radius, not 16px.
5. **No purple gradients.** Indigo is the brand color. No gradients anywhere.
6. **No glassmorphism or frosted glass effects.** Solid backgrounds only.
7. **No decorative illustrations.** Icons and data only. No cartoon people or abstract shapes.
8. **No carousel or slider components.** Data is displayed in tables and cards.
9. **No card shadows by default.** Use borders for separation. Shadows only for elevated elements (modals, toasts).
10. **No animations for the sake of animation.** Every motion must communicate state change.
11. **No "Welcome back, John!" dashboard headers.** Just show the data.
12. **No large padding wastelands.** Dense, information-rich layouts. White space is for separation, not decoration.
13. **No colored backgrounds on entire page sections.** White/neutral backgrounds. Color is for data and status only.
14. **No tooltips for critical information.** If it's important enough to need explaining, it should be visible.

---

## Dark Mode (Future)

Dark mode is planned for V2. When implementing:

- Use the neutral-950 scale as the base background
- Invert the neutral scale (neutral-800 becomes card bg, neutral-700 becomes borders)
- Keep brand colors the same (they work on dark backgrounds)
- Keep semantic colors the same (they work on dark backgrounds)
- Financial amounts maintain their positive/negative coloring

Do not implement dark mode in V1. Light mode only.

---

## Iconography

**Set:** Lucide React (consistent, clean, 1px stroke weight)

**Sizes:**
- Navigation: 20px
- Inline with text: 16px
- Buttons: 16px (with text), 20px (icon only)
- Empty states: 48px
- Status indicators: 12px (dot only, no icon)

**Rules:**
- One icon set only (Lucide). No mixing FontAwesome, Material, or custom SVGs.
- Icons in navigation always paired with labels (no icon-only nav items).
- Status uses colored dots, not icons (except in badges where space is tight).
