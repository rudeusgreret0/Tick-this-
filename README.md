# Tic Tac Toe — Atelier XO Studio Edition

A studio-crafted, tactile Android strategy game built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room**, **DataStore**, and **Firebase AI Logic (Gemini API)**.

---

## ✨ Key Features

### 1. Architectural Design System ("Atelier XO")
- **Restrained Studio Aesthetics**: Warm alabaster stone surfaces, vermilion & viridian pieces, and brushed brass accents — zero cheap RGB/neon clutter.
- **4 Curated Studio Palettes**:
  - **Atelier Ivory** (Default Light — Warm Alabaster, Terracotta Vermilion X, Viridian Teal O)
  - **Nordic Porcelain** (Cool Limestone, Cobalt Ultramarine X, Burnished Amber O)
  - **Kyoto Hinoki** (Cypress Cream, Torii Crimson X, Matcha Moss O)
  - **Obsidian Studio** (Dark Matte Basalt, Ember Coral X, Mint Jade O)
- **Custom Bundled Typography**: Local Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, and `JetBrains Mono`).

### 2. Complete Gameplay & Rule Variants
- **Three Opponent Modes**:
  1. **Gemini AI Tactician**: Powered by **Firebase AI Logic (`gemini-3.8-flash`)** with structured JSON output, real-time grandmaster move commentary, confidence telemetry, strict move validation, and automatic local Minimax fallback.
  2. **Local Minimax Engine**: Deterministic Alpha-Beta pruning engine across three intelligence tiers (*Apprentice*, *Tactician*, *Grandmaster*).
  3. **Pass & Play Duel**: Local two-player tabletop mode with customizable player callsigns.
- **Two Board Rule Disciplines**:
  - **Classic 3×3**: Traditional rules.
  - **Infinity 3-Mark Mode (No Draws)**: Each player holds at most 3 active marks on the grid. Placing a 4th mark immediately vanishes your oldest mark (highlighted with a live `FADING` indicator).
- **Tactical Controls**: Full **Undo** stack, **Advisor Hint** engine, **Swap X/O**, **Rematch**, and live move notation ticker (`#1 X→B2`).

### 3. Persistent Archives & Interactive Replay Inspector
- **Statistics Dashboard**: Tracks Total Matches, Win Rate %, Current & Best Streak, Fastest Victory, Average Moves, and Win/Draw/Loss distribution.
- **Step-by-Step Replay Modal**: Tap any recorded match in the **Records** tab to scrub backward and forward through every move on a visual 3×3 board.

### 4. Programmatic 16-Bit PCM Audio & Haptics
- Synthesizes warm marimba & ceramic chimes in real time via Android `AudioTrack` (`SoundAndHapticsManager`) alongside optional ambient harmonic studio chords and precision haptic feedback.

---

## 🔐 Security & Gemini AI Setup

This project uses **Firebase AI Logic** with **Firebase App Check** (`DebugAppCheckProviderFactory` in debug builds and `RecaptchaAppCheckProviderFactory` in release builds).
- **No client-side `GEMINI_API_KEY` is hardcoded or packaged in the APK.**
- `.env.example` contains no raw credentials.
- To connect your own Firebase project:
  1. Place your project's `app/google-services.json` in `app/`.
  2. Configure `recaptcha_site_key` in `app/src/main/res/values/firebase_applet_config.xml` for release builds.

---

## 🛠️ Build, Test & APK Generation Instructions

### Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Build Installable APK
```bash
gradle :app:assembleDebug
```
The compiled installable APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk` (and copied to `TicTacToe-v1.0.0.apk`).

### Installing the APK on Your Android Phone
1. Download `TicTacToe-v1.0.0.apk` from the **GitHub Releases** page (or export via AI Studio's **Build / Download APK** menu).
2. Open the downloaded `.apk` file on your Android smartphone.
3. If prompted, allow installation from your browser or file manager, then tap **Install**.
