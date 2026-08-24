# IGRIS Roadmap

## v1 (this repo)
Shipped: Brain pipeline, fast-intent tools, router, memory, vault RAG, voice, privacy, diagnostics, routines/projects/plugins, briefing.

## v2 — On-device intelligence
- Bundle a small on-device LLM (e.g. via MLC-LLM / llama.cpp) as `LOCAL_MODEL` route.
- Embedding-based RAG (replace TF-IDF with local embeddings).
- Camera AI: ML Kit OCR (Latin + Bangla), object labeling, visual QA.

## v3 — Automation & home
- Contextual reminders (app-open triggers), Routine engine scheduling.
- Smart-home hub (Home Assistant / MQTT / local APIs).
- Notification-summary auto-triggers.

## v4 — Ecosystem
- Plugin marketplace with per-plugin permissions & security levels.
- Multi-device identity, local-network handoff, offline sync.
- Hardware acceleration (GPU/NPU) model selection.
