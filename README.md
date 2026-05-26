# Mnemo

Mnemo turns your screenshots into a searchable, connected knowledge base — entirely on your device. No cloud, no uploads, no account required.

## What it does

You take screenshots to save things: articles, job posts, recipes, conversations, ideas. They pile up in your gallery and become impossible to find. Mnemo fixes this.

Point Mnemo at your screenshot folder and it reads every image using an on-device AI (Gemma 3n). For each screenshot it extracts:

- What it is — LinkedIn post, Reddit thread, email, article, chat, etc.
- Who and what it mentions — people, companies, projects, URLs
- A short summary and key topics
- Whether it needs action and how urgently

That data feeds into a **knowledge graph** that connects related screenshots by topic and entity. You can browse by gallery, explore the graph visually, or search by meaning rather than filename.

Every morning, Mnemo sends a digest of what was captured overnight, grouped by topic.

Everything runs locally. Gemma 3n runs on your phone's CPU/GPU. Your screenshots never leave the device.

## Getting started

### Requirements

- Android 9.0 (API 28) or newer
- ~3.5 GB free storage for the AI models
- WiFi recommended for the initial model download

### First-time setup

1. **Download the models** — open the Setup screen and download both models:
   - **Gemma 3n E2B** (3.14 GB) — understands and describes your screenshots
   - **GTE Small** (34 MB) — powers semantic search
2. **Select your screenshot folder** — tap "Select" and pick the folder where your screenshots live (usually `Pictures/Screenshots`)
3. **Index** — go to the Indexing screen, select the screenshots you want to analyze, and tap Index. Mnemo processes them in the background.

After indexing, search, gallery, and graph views are all populated automatically.

## Screens

| Screen | What it's for |
|--------|---------------|
| **Setup** | Download models, pick screenshot folder, trigger re-indexing |
| **Gallery** | Browse indexed screenshots with summaries and metadata |
| **Search** | Find screenshots by meaning — "visa requirements" finds a screenshot of a government site even if it doesn't say those exact words |
| **Graph** | Visual map of how screenshots connect — clusters show recurring topics |
| **Indexing** | Pick which screenshots to analyze and track progress |

## Privacy

Mnemo processes everything on-device. No analytics, no telemetry, no network calls beyond the one-time model download from HuggingFace. The extracted metadata is stored in a local SQLite database on your phone.

## Technical notes

- AI inference: [Google LiteRT LM](https://ai.google.dev/edge/litert) with Gemma 3n E2B (INT4 quantized)
- Semantic search: [GTE Small](https://huggingface.co/thenlper/gte-small) via ONNX Runtime
- Built with Jetpack Compose, Room, WorkManager
- Requires Android 9+ for the MediaStore API used to scan screenshots
