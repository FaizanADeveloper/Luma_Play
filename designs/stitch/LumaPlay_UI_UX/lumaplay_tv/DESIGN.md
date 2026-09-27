---
name: LumaPlay TV
colors:
  surface: '#0d1322'
  surface-dim: '#0d1322'
  surface-bright: '#33394a'
  surface-container-lowest: '#080e1d'
  surface-container-low: '#151b2b'
  surface-container: '#191f2f'
  surface-container-high: '#242a3a'
  surface-container-highest: '#2f3445'
  on-surface: '#dde2f8'
  on-surface-variant: '#c2c6d5'
  inverse-surface: '#dde2f8'
  inverse-on-surface: '#2a3040'
  outline: '#8c909f'
  outline-variant: '#424753'
  surface-tint: '#adc6ff'
  primary: '#adc6ff'
  on-primary: '#002e69'
  primary-container: '#4d8efe'
  on-primary-container: '#00285c'
  inverse-primary: '#005ac1'
  secondary: '#7bd0ff'
  on-secondary: '#00354a'
  secondary-container: '#00a6e0'
  on-secondary-container: '#00374d'
  tertiary: '#d0bcff'
  on-tertiary: '#3c0091'
  tertiary-container: '#a078ff'
  on-tertiary-container: '#340080'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a41'
  on-primary-fixed-variant: '#004494'
  secondary-fixed: '#c4e7ff'
  secondary-fixed-dim: '#7bd0ff'
  on-secondary-fixed: '#001e2c'
  on-secondary-fixed-variant: '#004c69'
  tertiary-fixed: '#e9ddff'
  tertiary-fixed-dim: '#d0bcff'
  on-tertiary-fixed: '#23005c'
  on-tertiary-fixed-variant: '#5516be'
  background: '#0d1322'
  on-background: '#dde2f8'
  surface-variant: '#2f3445'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Inter
    fontSize: 38px
    fontWeight: '700'
    lineHeight: 46px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.005em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.01em
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.08em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  margin: 3.5rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1.25rem
  space-lg: 2rem
  space-xl: 3rem
---

## Brand & Style

This design system is tailored for the 10-foot television experience, engineered specifically for high-definition 1080p and 4K displays viewed from a distance of 2.5 to 3.5 meters. The core personality is utilitarian, cinema-grade, and effortless. It strips away mobile clutter to prioritize visual weight, immediate legibility, and predictable D-pad directional navigation.

The aesthetic fuses Google TV's clean visual structure with premium home-theatre fidelity. It utilizes deep, cinematic midnight navy surfaces that melt into room lighting, energized by high-visibility electric blue and cyan indicators. This system minimizes eye fatigue while establishing rapid spatial orientation across massive media libraries through directional focus cues, scale shifts, and high-contrast micro-metadata.

## Colors

The color palette is built exclusively for dark-room living environments and self-emissive displays (OLED/LED), preserving contrast without introducing blooming or harsh pure-white glare.

### Primary, Secondary, & Tertiary Accents
- **Primary Accent (`#4285F4`)**: The foundational action color. Used for primary focus indicators, active playback timelines, and core control states.
- **Secondary Accent (`#38BDF8`)**: Electric cyan highlight, applied directly to D-pad active focus borders, high-impact focus glows, and secondary progress indicators to ensure instant recognition at a distance.
- **Tertiary Accent (`#8B5CF6`)**: Deep violet accent for featured cinematic badges, playlist highlights, and promotional gradient overlays.

### Surface System
- **Background (`#0B1120`)**: Deep navy charcoal canvas grounding the overall screen.
- **Surface (`#111C30`)**: Subtle dark slate blue layer for side navigation bars, overlay backdrops, and secondary trays.
- **Surface Elevated / Card (`#192840`)**: Base resting state for media cards, modal sheets, and floating control bars.
- **Surface Container High (`#1E314F`)**: Used for hover/focus background transitions, selected list rows, and elevated dialogs.

### Typography & Status
- **Text Primary (`#F8FAFC`)**: Crisp slate-white ensuring uncompromised readability across all viewing angles.
- **Text Secondary (`#94A3B8`)**: Cool slate blue-gray for supporting metadata, subheadings, and runtime counters.
- **Text Muted (`#64748B`)**: Low-emphasis copy for secondary timestamps and inactive category filters.
- **Badge / Status Accent Colors**: Emerald (`#10B981`) for completed statuses, Amber Gold (`#F59E0B`) for HDR/Dolby Vision badges, and Crimson (`#EF4444`) for errors and storage warnings.

## Typography

The typography scale is strictly calibrated for 1080p rendering projected onto large displays (43" to 85"+). The base unit relies on `Inter` for clean horizontal geometric proportions and open counters, preventing letterforms from blurring together at standard living-room viewing distances.

- **Display Scales (`display-lg`, `display-md`)**: Reserved exclusively for active media hero titles, main billboard banners, and modal title highlights.
- **Headline Scales (`headline-lg`, `headline-md`, `headline-sm`)**: Form the backbone of browse rows, category sections, and detail screen subheadings.
- **Body Hierarchy (`body-lg`, `body-md`, `body-sm`)**: Used for plot synopses, track listings, and technical file information. Never drop below 14px on standard TV viewports to eliminate subpixel crawl.
- **Label Scales (`label-lg`, `label-md`, `label-sm`)**: Set with elevated weights (600–700) and positive letter-spacing (`0.04em`–`0.08em`). `label-sm` is dedicated to uppercase technical codec badges (e.g., `4K UHD`, `HDR10`, `DOLBY VISION`, `HEVC`, `AV1`).

## Layout & Spacing

The layout is built around the TV standard 16:9 canvas with strict adherence to **TV Safe Margins**. All interactive controls and mission-critical metadata are kept inside a `56px` (`3.5rem`) boundary to prevent edge clipping caused by manufacturer overscan.

### Grid & Row Navigation Model
- **Left Dock Navigation**: Fixed width collapsed sidebar (`88px`) expanding on focus to (`260px`).
- **Main Viewport**: Fluid horizontal swimlanes organized into category carousels. Card carousels maintain a fixed vertical pitch to allow standard directional D-pad snapping up and down.
- **Gutters & Spacing**: Standard horizontal item spacing uses `gutter` (`24px` / `1.5rem`) to accommodate card focus expansion (`scale: 1.04`) without overlapping adjacent elements.
- **Card Gaps & Padding**: Internal card padding uses `space-sm` (`12px`) and `space-md` (`20px`) for balanced grouping.

## Elevation & Depth

Visual hierarchy on TV screens relies on a combination of **tonal surface stepping**, **scale transformation**, and **luminous focus glow**, replacing subtle touch-screen shadows with unmistakable directional feedback.

### Tonal Hierarchy
- **Level 0 (Canvas)**: Background `#0B1120`. Non-focusable, low luminance.
- **Level 1 (Dock / Trays)**: Surface `#111C30`. Anchored side nav and structural panels.
- **Level 2 (Resting Cards & Inputs)**: `#192840`. Default interactive objects.
- **Level 3 (Focused State)**: Surfaces shift up to `#1E314F`, accompanied by scaling and outer emission.

### Remote Focus State Mechanics
Every focused element triggers a simultaneous tripartite transformation:
1. **Scale**: The focused component scales smoothly to `1.04x` (or `1.05x` for buttons and isolated chips) via a 180ms ease-out cubic curve (`cubic-bezier(0.2, 0, 0, 1)`).
2. **Focus Border**: A crisp `3px` solid stroke in secondary accent cyan (`#38BDF8`).
3. **Luminous Glow**: An ambient outer projection of `0 0 20px rgba(56, 189, 248, 0.45)` with a secondary spread of `0 12px 32px rgba(11, 17, 32, 0.8)` to lift the card cleanly off the row.

## Shapes

The design system employs **Level 2 (Rounded)** shape parameters:
- **Standard UI elements**: `0.5rem` (`8px`) border radius (buttons, chips, technical spec tags).
- **Cards & Media tiles (`rounded-lg`)**: `1rem` (`16px`) radius, creating soft, cinema-friendly frames around 16:9 thumbnails and posters.
- **Large dialogs & overlay backdrops (`rounded-xl`)**: `1.5rem` (`24px`) radius.

Pills and full circles are applied exclusively to playback controls (Play/Pause FAB), runtime badges, and active state indicators.

## Components

### TV Media Cards (16:9 Landscape)
- **Geometry**: Default 16:9 aspect ratio with `1rem` (`16px`) corner radius.
- **Resting State**: Background `#192840`, subtle border `#1E314F` (`1px`). Artwork covers the card with a bottom gradient fade (`rgba(11, 17, 32, 0.85)`).
- **Metadata Overlay**: Bottom-left displays title (`headline-sm`, `#F8FAFC`), bottom-right holds duration badge (`label-md`). A persistent `4px` playback progress track runs across the bottom edge (`#4285F4` fill on `#1E314F` trough).
- **Focused State**: Scales `1.04x`, border changes to `3px solid #38BDF8`, outer halo activates (`0 0 20px rgba(56, 189, 248, 0.45)`), and secondary metadata (codecs, full synopsis preview) fades in.

### TV Sidebar Navigation
- **Structure**: Collapsed strip (`88px`) with icons only; expands over content to (`260px`) on focus.
- **Resting Item**: Icon centered in `#94A3B8`.
- **Focused Item**: Icon and label transition to `#F8FAFC`. Background displays `#1E314F` with a vertical active cyan pill indicator (`4px` wide, full item height) pinned to the left edge.

### Action Buttons
- **Primary Button**: Background `#4285F4`, text `#F8FAFC` (`label-lg`), height `48px`, minimum width `140px`, radius `8px`.
- **Focused Primary**: Background `#38BDF8`, text `#0B1120`, scale `1.05x`, halo `0 0 16px rgba(56, 189, 248, 0.5)`.
- **Secondary / Ghost Button**: Background `#192840`, border `1px solid #1E314F`, text `#94A3B8`. On focus: scales `1.05x`, border turns `2px solid #38BDF8`, text becomes `#F8FAFC`.

### Technical Spec Badges
- **Visuals**: Dark translucent pills (`rgba(17, 28, 48, 0.8)`), border `1px solid rgba(148, 163, 184, 0.2)`. Padding `3px 8px`, corner radius `4px`.
- **Typography**: `label-sm` uppercase, font weight 700.
- **Variants**:
  - `4K UHD`, `HEVC`, `AV1`: Slate neutral text `#94A3B8`.
  - `HDR10`, `DOLBY VISION`: Amber Gold text `#F59E0B` with `rgba(245, 158, 11, 0.15)` background.
  - `DOLBY ATMOS`: Cyan text `#38BDF8` with `rgba(56, 189, 248, 0.15)` background.

### Chips & Filter Tabs
- **Resting**: Height `40px`, padding `0 16px`, background `#192840`, text `#94A3B8` (`label-md`), radius `8px`.
- **Active / Selected**: Surface `#1E314F`, text `#38BDF8`, bottom edge highlight.
- **Focused**: Scale `1.05x`, border `2px solid #38BDF8`, text `#F8FAFC`.

### Progress & Playback Controls
- **Scrubber Bar**: Track height `6px`, resting background `#1E314F`, elapsed bar `#4285F4`. On focus, track height expands to `10px`, thumb handle becomes an illuminated `20px` circle with cyan focus glow (`#38BDF8`).
- **Transport Buttons**: Circular `48px` diameter controls (`#192840`). On focus, scales to `54px` with solid cyan outline and elevated icon luminance.