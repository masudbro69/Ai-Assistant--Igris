# Spec → Implementation Map

Legend: ✅ implemented in v1 · 🟡 partial · 🔭 roadmap (see ROADMAP.md)

| # | Spec feature | Status | Where |
|---|---|---|---|
| 1 | Product identity | ✅ | README, resources |
| 2 | Unified multi-capability system | ✅ | whole app |
| 3 | AI Brain (reason/memory/plan/tool-select/permission/execute) | ✅ | `brain/IgrisBrain.kt` |
| 4 | Multi-model router (chat/code/vision/…; offline/hybrid) | ✅ | `ai/ModelRouter.kt` |
| 5 | Smart offline mode (works with no internet) | ✅ | rule engine + tools all offline |
| 6 | Adaptive model by RAM/battery/thermal/CPU | ✅ | `core/DeviceInspector.kt` |
| 7 | AI power saving (fast intent vs planner) | ✅ | `ModelRouter.fastIntentTasks` |
| 8 | Proactive AI (configurable suggestions) | 🟡 | setting flag + briefing |
| 9 | Daily briefing ("Good morning IGRIS") | ✅ | `tools/TimeTools.kt#BriefingTool` |
| 10 | Personal knowledge vault | ✅ | `knowledge/KnowledgeVault.kt`, `ui/VaultActivity` |
| 11 | Local RAG (offline docs) | ✅ | TF-IDF in `knowledge/` |
| 12 | Project agent workspace | ✅ | `tools/PlanTools.kt#ProjectTool`, `ui/ProjectsActivity` |
| 13 | Task planner (decompose complex goals) | ✅ | `brain/Planner.kt` |
| 14 | Modes (Ask/Assistant/Agent/Automation/Private/Offline) | 🟡 | settings + offline fortress |
| 15 | Safe agent execution (plan→risk→permission→confirm→verify) | ✅ | `policy/Policy.kt` + Brain |
| 16 | Screen assist | 🔭 | needs accessibility API |
| 17 | Camera AI / OCR | 🔭 | v2 ML Kit |
| 18 | Live voice conversation | 🟡 | STT/TTS present; continuous mode v2 |
| 19 | Interruptible voice ("Stop") | ✅ | `voice/VoiceEngine.interrupt()` |
| 20 | Emotion-aware response tone | ✅ | `util/Language.kt#ToneAnalyzer` |
| 21 | Smart language switching (Bangla↔English) | ✅ | `LanguageDetector` + TTS locale |
| 22 | Custom personality | ✅ | settings + `ModelRouter.buildSystemPrompt` |
| 23 | Memory control (remember/forget/edit/search/export/wipe) | ✅ | `memory/MemoryStore.kt`, `tools/SearchTools.kt#MemoryTool` |
| 24 | Memory categories + sensitive control | ✅ | `MemoryCategory` |
| 25 | AI journal | ✅ | `InfoTools.kt#JournalTool` |
| 26 | Smart contextual reminders | 🟡 | time reminders v1; contextual v3 |
| 27 | Routine engine | ✅ | `PlanTools.kt#RoutineTool`, `ui/RoutinesActivity` |
| 28 | Notification summary | ✅ | `services/IgrisNotificationListener` + tool |
| 29 | File agent (local search) | ✅ | `SearchTools.kt#FileSearchTool` |
| 30 | Creative studio | 🟡 | planner templates; full studio v2 |
| 31 | Study mode (explain/quiz/flashcards) | ✅ | `InfoTools.kt#StudyTool` |
| 32 | Business mode | 🟡 | project/planner templates |
| 33 | Smart unified search | ✅ | `SearchTools.kt#SmartSearchTool` |
| 34 | Command history (deletable) | ✅ | `data/Stores.kt#CommandHistoryStore`, `ui/HistoryActivity` |
| 35 | Plugin market architecture | 🟡 | registry with per-tool perms; marketplace v4 |
| 36 | Developer mode | 🟡 | diagnostics panel; full dev mode v2 |
| 37 | AI tool marketplace security | 🔭 | v4 |
| 38 | Smart home hub | 🔭 | v3 |
| 39 | Multi-device IGRIS | 🔭 | v4 |
| 40 | Offline local network | 🔭 | v4 |
| 41 | Device handoff | 🔭 | v4 |
| 42 | Self diagnostics | ✅ | `ui/DiagnosticsActivity` |
| 43 | Offline recovery (fallback chain) | ✅ | `ModelRouter.generate` catch fallback |
| 44 | Privacy dashboard | ✅ | `ui/PrivacyActivity` + ledger |
| 45 | Network lock (Offline Fortress) | ✅ | settings + router |
| 46 | Data export | ✅ | memory export / wipe |
| 47 | One-tap data wipe | ✅ | `PlanTools.kt#PrivacyTool`, `PrivacyActivity` |
| 48 | Hardware acceleration | 🔭 | v4 |
| 49 | Performance engine (tier by task) | ✅ | `DeviceInspector` + router |
| 50 | Ultimate architecture | ✅ | see ARCHITECTURE.md |
| 51 | Final principle (understand→…→remember) | ✅ | `IgrisBrain.handle` |
| 52 | Positioning | ✅ | README |
| 53 | Long-term vision | 🔭 | ROADMAP |
