# Tic Tac Toe — Atelier XO Studio Edition

A crafted Android Tic Tac Toe game built with **Kotlin** and **Jetpack Compose**, combining a tactile tabletop aesthetic, responsive canvas stroke animations, a multi-key **Gemini API Manager** with hardware-backed encryption, an unbeatable local **Minimax** engine, and persistent **Room** match replays.

---

## Key Features

### 1. Focused Mobile Game Interface
- **Centerpiece 3×3 Board**: Elevated board surface with responsive spring physics, choreographed `X` (terracotta) and `O` (deep jade) stroke drawing animations, and an animated gold winning strike line.
- **Sleek Player Score Capsules**: Compact side-by-side player badges with active turn rings, live scores, and provider pills (`Gemini AI`, `Gemini (Key Label)`, or `Local Master AI`).
- **Curated Studio Palettes**: Instant switching between **Atelier Ivory**, **Nordic Porcelain**, **Kyoto Hinoki**, and **Obsidian Studio**, plus Light / Dark / System mode overrides.

### 2. Advanced Gemini API Manager (Settings → Gemini API Manager)
- **Multi-Key Management**: Add, name/label (`Primary Key`, `Backup Key`, `Personal Key`), switch, replace, or remove multiple Gemini API keys independently.
- **Hardware-Backed Encryption & Masking**: Keys are encrypted at rest using Android Keystore (`AES/GCM/NoPadding`) and displayed only in masked form (`AIza••••••••7WqU`) after saving, with a Show/Hide toggle during input.
- **Live Connection Testing & Status Badges**: Test unsaved or saved keys on demand with clear status pills (`Valid`, `Quota Exceeded`, `Invalid`, `Unavailable`, `Untested`) and diagnostic details.
- **Resilient Automatic Failover**:
  1. Uses the user's selected personal Gemini API key first (if active).
  2. Automatically rotates to any other saved valid backup key if the primary key hits HTTP 429 quota limits.
  3. Falls back to the built-in Studio Cloud Proxy (Firebase AI Logic) or the unbeatable **Local Master AI** (Minimax with Alpha-Beta pruning) with a compact, single-line dismissible notice so gameplay never stalls.

### 3. Game Modes & Rule Variants
- **Gemini AI**: Structured JSON tactical opponent with live move commentary and automatic failover.
- **Master AI (Local Minimax)**: Offline opponent with 4 difficulty tiers (*Casual*, *Tactical*, *Master*, *Unbeatable* via depth-penalized Alpha-Beta pruning).
- **Pass & Play (2P)**: Local two-player tabletop mode with customizable player names.
- **Classic 3×3 & Infinity 3-Mark Mode**: Play standard 3×3 rules or **Infinity 3-Mark**, where each player can hold at most 3 marks on the board—placing a 4th mark causes the oldest mark to vanish, eliminating draws.

### 4. Match Archive, Replay Inspector & Synthesized Acoustics
- **Room Match History**: Automatically records completed matches with move notation (`X:B2;O:A1;...`), duration, mode, and winner.
- **Step-by-Step Replay Scrubber**: Inspect any past game move-by-move on an interactive mini-board.
- **Programmatic 16-bit PCM Audio & Haptics**: Zero-asset synthesized marimba and ceramic chimes via `AudioTrack` plus tactile haptic impulses.

---

## Building & Running Tests

### Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest --stacktrace
```

### Build Installable Debug APK
```bash
gradle :app:assembleDebug --stacktrace
cp app/build/outputs/apk/debug/app-debug.apk TicTacToe-v1.0.0.apk
```

Note: Both `settings.gradle.kts` and `.github/workflows/android-release.yml` automatically verify and download the bundled Google Fonts (`space_grotesk.ttf`, `plus_jakarta_sans.ttf`, `jetbrains_mono.ttf`) if missing on a fresh GitHub checkout before resource compilation begins.
