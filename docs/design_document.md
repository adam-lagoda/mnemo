# Mnemo — Claude Code Bootstrap Prompt

Copy everything below the line into Claude Code from the root of your empty Android Studio project.

---

```
You are bootstrapping "Mnemo" — an Android app that indexes the user's screenshot gallery using on-device AI (Gemma 3n via MediaPipe/LiteRT-LM), builds a knowledge graph with community detection, and delivers proactive weekday morning notifications summarizing what was captured.

## Who I am
I'm an ML engineer with deep experience in robotics, DevOps, and control engineering. I have Android Studio installed. Skip explanations of basic concepts. Be direct about tradeoffs. If something is a hack, say so.

## Hard constraints
- Fully edge-deployed. Zero cloud. Zero network calls. All inference on-device.
- Kotlin + Jetpack Compose. Target SDK 35, min SDK 28.
- On-device VLM: Gemma 3n E2B (2GB memory footprint) via Google AI Edge / MediaPipe LLM Inference API for screenshot extraction. This model accepts image+text input and returns text. Use the multimodal prompting path.
- On-device embeddings: a small ONNX model (gte-small or e5-small-v2, quantized INT8) via ONNX Runtime Mobile for generating text embeddings from extracted metadata. If ONNX setup is too heavy for bootstrap, stub the embedding interface and use TF-IDF or bag-of-words cosine similarity as a fallback — I'll swap it later.
- Room DB for all persistence. No external DB.
- WorkManager for all background scheduling.
- No Gradle version catalog — use classic dependencies block for now, I'll migrate later.

## Project structure
Generate this structure. Do NOT generate placeholder/TODO files — every file should have real, compilable implementation or be explicitly marked as a stub with a clear interface.

```
app/src/main/java/com/mnemo/
├── MnemoApp.kt                    # Application class, DI init
├── di/
│   └── AppModule.kt               # Manual DI (no Hilt/Dagger — keep it simple, I'll add DI later if needed)
├── data/
│   ├── db/
│   │   ├── MnemoDatabase.kt       # Room database
│   │   ├── ScreenshotDao.kt       # DAO for screenshots
│   │   ├── GraphEdgeDao.kt        # DAO for graph edges
│   │   └── entities/
│   │       ├── ScreenshotEntity.kt # id, uri, timestamp, sourceType, extractedJson, embeddingBlob, communityId, reviewed
│   │       └── GraphEdgeEntity.kt  # sourceId, targetId, weight, edgeType (semantic|entity|temporal)
│   ├── repository/
│   │   ├── ScreenshotRepository.kt
│   │   └── GraphRepository.kt
│   └── model/
│       └── ExtractionResult.kt     # Data class for structured VLM output
├── extraction/
│   ├── VlmExtractor.kt            # Interface for VLM extraction
│   ├── GemmaExtractor.kt          # Gemma 3n implementation via MediaPipe
│   └── ExtractionPrompts.kt       # Prompt templates for structured extraction
├── embedding/
│   ├── EmbeddingEngine.kt         # Interface
│   ├── OnnxEmbeddingEngine.kt     # ONNX Runtime impl (can be stubbed)
│   └── TfIdfFallbackEngine.kt     # Fallback: TF-IDF cosine similarity
├── graph/
│   ├── GraphBuilder.kt            # Builds adjacency from embeddings + entities + timestamps
│   ├── LouvainClustering.kt       # Louvain community detection (pure Kotlin impl)
│   └── GraphAnalytics.kt          # Query helpers: get communities, find related, etc.
├── scheduling/
│   ├── ScreenshotMonitor.kt       # ContentObserver on MediaStore for new screenshots
│   ├── ExtractionWorker.kt        # WorkManager worker: batch extract unprocessed screenshots
│   ├── GraphUpdateWorker.kt       # WorkManager worker: rebuild edges + communities after extraction
│   └── MorningNotificationWorker.kt # WorkManager periodic: weekday 8am, summarize unreviewed
├── notification/
│   └── NotificationHelper.kt      # Notification channel setup + builders
├── ui/
│   ├── navigation/
│   │   └── MnemoNavGraph.kt       # Nav graph: Gallery, Search, Graph, Settings
│   ├── gallery/
│   │   ├── GalleryScreen.kt       # Grid of indexed screenshots, filterable by community/source
│   │   └── GalleryViewModel.kt
│   ├── search/
│   │   ├── SearchScreen.kt        # Semantic search across extracted content
│   │   └── SearchViewModel.kt
│   ├── graph/
│   │   ├── GraphScreen.kt         # Force-directed graph visualization
│   │   ├── GraphViewModel.kt
│   │   └── ForceDirectedLayout.kt # Physics sim for graph layout (Compose Canvas)
│   ├── detail/
│   │   ├── DetailScreen.kt        # Single screenshot: image + extracted data + related nodes
│   │   └── DetailViewModel.kt
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
└── util/
    ├── BitmapUtils.kt             # Screenshot loading, resizing for VLM input
    └── DateUtils.kt               # Weekday checks, morning window logic
```

## Extraction prompt design (critical)
The VLM extraction prompt is the core IP. Design it carefully. The prompt sent to Gemma 3n with each screenshot should request a JSON response with:

```json
{
  "source_type": "linkedin|instagram|reddit|twitter|email|chat|article|other",
  "title": "inferred title or subject line",
  "entities": ["person names", "company names", "project names", "URLs"],
  "topics": ["machine learning", "hiring", "product launch"],
  "action_items": ["follow up with X", "read paper Y"],
  "summary": "2-3 sentence summary of content",
  "sentiment": "positive|negative|neutral|mixed",
  "urgency": 0.0-1.0,
  "language": "en"
}
```

The prompt must handle diverse screenshot types gracefully — a LinkedIn post looks nothing like a Reddit thread or a WhatsApp chat. Instruct the model to infer source type from visual layout cues (LinkedIn's blue header, Reddit's upvote arrows, chat bubbles, email headers, etc.)

## Graph construction logic
- **Semantic edges**: cosine similarity between embedding vectors > 0.7 threshold (configurable)
- **Entity edges**: shared entities between screenshots (exact match on normalized entity strings)
- **Temporal edges**: screenshots within 30 minutes of each other get a weak edge (weight 0.3)
- Edge weights are combined: `w = max(semantic, entity) + temporal_bonus`
- Run Louvain on the weighted adjacency to get communities
- Label communities by most frequent topics within them

## Louvain implementation
Implement Louvain modularity optimization in pure Kotlin. It's a simple algorithm:
1. Start with each node in its own community
2. For each node, try moving it to each neighbor's community
3. Accept the move that gives the biggest modularity gain (if positive)
4. Repeat until no moves improve modularity
5. Contract the graph (merge communities into super-nodes) and repeat

This runs fine on <10k nodes. Don't over-engineer it.

## Force-directed graph layout
Implement a basic force-directed layout using Compose Canvas:
- Repulsion: Coulomb's law between all node pairs (use Barnes-Hut if >500 nodes, otherwise brute force is fine)
- Attraction: Hooke's law along edges
- Damping: velocity *= 0.95 each tick
- Color nodes by community
- Size nodes by degree
- Tap to select → show detail
- Pinch to zoom, drag to pan

You know this — it's literally a particle sim. Use `LaunchedEffect` with a frame loop, not `animateFloatAsState`.

## Morning notification logic
- `MorningNotificationWorker` runs as a `PeriodicWorkRequest` with ~24h interval
- Check: is it a weekday? Is it between 7:00-9:00? If not, skip.
- Query: unreviewed screenshots from last 48 hours
- Group by community
- Build notification: "Mnemo found 5 items in 'ML Research', 3 in 'Job Opportunities', 2 in 'Project Ideas'"
- Tapping the notification opens the gallery filtered by those communities

## What to generate now (priority order)
1. `build.gradle.kts` (app-level) and `build.gradle.kts` (project-level) with all dependencies
2. `AndroidManifest.xml` with all required permissions (READ_MEDIA_IMAGES, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE)
3. Room DB, entities, DAOs, database
4. ExtractionResult data model + ExtractionPrompts
5. VlmExtractor interface + GemmaExtractor (MediaPipe integration — use the actual MediaPipe LLM Inference API, reference the latest docs if unsure about the multimodal API surface)
6. EmbeddingEngine interface + TfIdfFallbackEngine (stub ONNX for now)
7. ScreenshotMonitor (ContentObserver)
8. All three WorkManager workers
9. NotificationHelper
10. GraphBuilder + LouvainClustering + GraphAnalytics
11. All UI screens + ViewModels + navigation
12. ForceDirectedLayout (Compose Canvas)
13. Theme (dark-first, Material3, monochromatic with a single accent color — think "developer tool", not "consumer app")
14. MnemoApp + AppModule wiring

## What NOT to do
- Don't add Hilt/Dagger/Koin. Manual DI for now.
- Don't add Retrofit/OkHttp. No network.
- Don't add Firebase anything.
- Don't generate unit tests yet — I'll add them.
- Don't use Navigation Compose type-safe routes if it complicates things — string routes are fine.
- Don't use experimental Compose APIs without flagging them.

## After generating
Tell me:
1. What model file I need to download and where to put it (for Gemma 3n)
2. Any permissions I need to grant manually on the device
3. What's stubbed vs real
4. Known limitations or things that will need iteration

Go. Start with the Gradle files and work down the priority list.
```