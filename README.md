# IGRIS — Intelligent General-purpose Responsive Intelligence System

> **"Your Device. Your Intelligence. Your IGRIS."**
> *Not Just an Assistant. Your Personal AI Operating Agent.*

IGRIS is an **offline-first, multilingual (Bangla + English), voice-controlled personal AI operating agent** for Android. It is not a chatbot — it is a unified system combining:

AI Assistant • Voice Assistant • Local AI Agent • Personal Memory • Device Controller • Automation Engine • Productivity Agent • Vision Assistant • Translation Assistant • File Assistant • Research Assistant • Personal Knowledge Base • Offline AI Companion

Core operating philosophy (per action):
`Understand → Think → Plan → Permission Check → Execute → Verify → Report → (optionally) Remember`

---

## Download / Install

1. Go to **Releases** (or the latest successful **Actions** run → `igris-apks` artifact).
2. Download `igris-release.apk`.
3. On your phone enable *Install unknown apps*, then install. (Uninstall any older CI build first — CI generates a signing key per run.)

## Build from source

Requires JDK 17 and the Android SDK (compileSdk 35). CI builds automatically on every push (see `.github/workflows/android.yml`).

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

## What's implemented now (v1)

- **AI Brain pipeline** (classify → route → permission → execute → verify) in `brain/IgrisBrain.kt`
- **Fast Intent Engine** — flashlight, volume, timers, alarms, reminders, notes, calculator, app launch, device info with *no* LLM
- **Multi-model router** — offline rule engine always; **free OpenCode Zen models by default** (Big Pickle, MiniMax M2.5 Free, Nemotron 3 Super Free, MiMo V2 Pro/Flash Free, DeepSeek V4 Flash Free, GPT-5 Nano) with auto-rotation; optional custom OpenAI-compatible endpoint or Ollama (LAN/local)
- **Offline Fortress / Privacy ledger / one-tap wipe**
- **Personal Memory** with categories, forget/search/export
- **Knowledge Vault + offline Local RAG** (TF-IDF) — answer from your own documents with zero internet
- **Daily briefing, routines, projects, plugins registry, smart search, notification summary, command history**
- **Voice**: on-device STT + interruptible TTS, Bangla/English detection, emotion/tone-aware response style
- **Self diagnostics & adaptive model tier by RAM/thermal/battery**

## Roadmap

See `docs/ROADMAP.md` — on-device local LLM, camera OCR/vision, smart-home hub, multi-device handoff, plugin marketplace.

Full product specification: `docs/SPEC.md`. Feature→code map: `docs/FEATURE_MAP.md`.

Licensed under Apache-2.0.
