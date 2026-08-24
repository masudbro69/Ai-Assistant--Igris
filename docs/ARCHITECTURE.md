# IGRIS Architecture

```
                          IGRIS (MainActivity / VoiceEngine)
                                     │
                              ┌──────┴──────┐
                              │  AI BRAIN   │  brain/IgrisBrain.kt
                              └──────┬──────┘
            ┌────────────────────────┼────────────────────────┐
            │                        │                        │
        UNDERSTAND                PLANNER                 ROUTER
   brain/TaskClassifier.kt     brain/Planner.kt        ai/ModelRouter.kt
   (Bangla+English intent,     (multi-step plans,     (fast-intent vs rule-engine
    tone, language detect)      approval-aware)        vs cloud vs ollama)
            │                        │                        │
            └────────────────────────┼────────────────────────┘
                                     │
                              TOOL ENGINE          tools/Tool.kt, tools/AllTools.kt
            ┌──────────────────┬─────┴─────┬──────────────────┐
            │                  │           │                  │
        DEVICE              TIME/UTIL     SEARCH/KNOWLEDGE   PLAN/POLICY
      DeviceTools.kt        TimeTools.kt  SearchTools.kt     PlanTools.kt
                            InfoTools.kt
            │
     PERMISSION ENGINE  policy/Policy.kt   (risk → confirmation → execute)
            │
     SECURITY / PRIVACY policy/Policy.kt PrivacyLedger, Offline Fortress
            │
     MEMORY & STORAGE   memory/, data/, knowledge/   (SQLite + Local RAG)
            │
     VOICE / UI         voice/VoiceEngine.kt, ui/    (STT + interruptible TTS)
```

## Packages
| Package | Purpose |
|---|---|
| `brain` | Understand→Plan→Route→Execute pipeline |
| `ai` | Model router + providers (rule engine, OpenAI-compatible, Ollama) |
| `tools` | Executable tools (device, time, info, search, plan) |
| `policy` | Risk levels, permission engine, privacy ledger |
| `memory` | Personal memory store |
| `knowledge` | Vault + TF-IDF local RAG |
| `data` | SQLite schema + settings + history |
| `voice` | STT + TTS, barge-in, language/tone |
| `services` | Notification listener, reminder/briefing receivers |
| `ui` | Chat + panels |
| `core`, `util` | Shared types, classifier helpers, math, language |
