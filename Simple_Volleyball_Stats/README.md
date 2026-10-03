# SimpleVolleyballStats (SVS) 🏐

> **Real-Time Performance Monitoring Application for Volleyball Players and Coaches**

This repository contains the source code for **SimpleVolleyballStats (SVS)**, developed as a **CSE 496 Graduation Project** at Gebze Technical University. The system provides a mobile-first solution for coaches and statisticians to instantly record match actions — serve, spike, block, dig, and more — without taking their eyes off the court, using both an intuitive touch UI and **AI-powered voice commands**.

---

## ✨ Features

### 📊 Match Tracking
- **Single Team Mode:** Track one team's statistics against any opponent.
- **Dual Team Mode:** Track both teams simultaneously in real-time on a single device, with a master/slave ViewModel synchronization architecture.
- **Action Recording:** Record all standard volleyball actions with immediate outcome selection (Ace, Kill, Error, In-Play, etc.).
- **Rally Log:** View a chronological log of all actions in the current set.
- **Set & Match Management:** Automatic set transitions, set score tracking (best of 3 or 5), and match-end detection.

### 👥 Team & Roster Management
- Create and manage multiple teams with full player rosters.
- Define player positions (Setter, Outside Hitter, Middle Blocker, Libero, etc.).
- Manage **starting lineups** with court position assignment (1–6).
- Handle **player substitutions** and **libero** rotations during live matches.

### 🎤 AI Voice Command Integration (Push-to-Talk)
- Record actions **hands-free** by holding the microphone button.
- Audio is sent to a local FastAPI backend server over LAN (Wi-Fi).
- **OpenAI Whisper** transcribes speech using volleyball-specific context prompts for higher accuracy.
- **Google Gemini LLM** parses the transcript into a structured action JSON object.
- Supports multiple languages: Turkish 🇹🇷, English 🇬🇧, German 🇩🇪, French 🇫🇷, Spanish 🇪🇸.

### 📈 Statistics & Reports
- Detailed **per-player statistics** tables (Total, `#`/Perfect, `+`/Good, `-`/Poor, `=`/Error).
- **Team-wide aggregate** statistics by action type.
- **Match Report:** View a full summary including set scores, team stats, and individual efficiency breakdowns.
- **Share Report:** Export a cleanly formatted text report (emoji-enhanced for readability) via any sharing app (WhatsApp, etc.).

---

## 🏗️ Architecture

This project follows a **Clean Architecture** pattern with a **decoupled monorepo** structure.

### Mobile Client (KMP)
```
presentation/
├── screens/
│   ├── home/          # Team list & navigation hub
│   ├── match/         # Single & Dual team match screens + ViewModel
│   ├── report/        # Match report viewer
│   ├── team/          # Team detail & player management
│   └── settings/      # App settings (server URL, language)
├── navigation/        # Compose Navigation graph
└── theme/             # Material 3 design tokens

domain/
├── model/             # Core data models (Team, Player, Match, Action, MatchState)
├── repository/        # Repository interfaces
├── service/           # AudioRecorder, VoiceService, VoiceInputService,
│                      #   MatchReportGenerator, ShareManager, ActionQueue
└── usecase/           # Business logic use cases

data/
├── local/             # SQLDelight database & driver factory
└── repository/        # Repository implementations
```

### Key Architectural Decisions

| Decision | Rationale |
|---|---|
| **Kotlin Multiplatform (KMP)** | Shared business logic across Android and iOS from a single codebase |
| **Jetpack Compose** | Declarative UI with reactive state management |
| **MVI Pattern** (`MatchIntent` → `MatchViewModel` → `MatchState`) | Unidirectional data flow for predictable state |
| **Koin DI (Singleton)** | `AudioRecorder`, `VoiceService`, `HttpClient` are shared singletons; ViewModels are factories |
| **`collectAsStateWithLifecycle`** | Lifecycle-aware state collection prevents recomposition on destroyed screens |
| **`BackHandler` for safe exit** | Stops hardware resources (microphone) on the IO thread *before* popping the back stack, eliminating the "White Screen" freeze on navigation |
| **Idempotent `stopRecording()`** | Race condition protection: no-op if recording was already stopped |
| **SQLDelight** | Type-safe SQL with offline-first data persistence |

---

## 🛠️ Tech Stack

### Client Application (Android / iOS)

| Layer | Technology |
|---|---|
| Language | Kotlin (Multiplatform) |
| UI | Jetpack Compose / Compose Multiplatform |
| State Management | `StateFlow` + MVI (`MatchIntent`) |
| Dependency Injection | Koin |
| Local Database | SQLDelight (SQLite) |
| Networking | Ktor Client |
| Serialization | Kotlinx Serialization |
| Lifecycle | `androidx.lifecycle.viewmodel.compose` |
| Audio | Android `MediaRecorder` API |
| Navigation | Compose Navigation |

### Local Backend Server (AI Processing)

| Layer | Technology |
|---|---|
| Language | Python 3 |
| Framework | FastAPI + Uvicorn |
| Speech-to-Text | OpenAI Whisper API (`whisper-1`) |
| NLP / Parsing | Google Gemini API (`google-genai`, `gemini-2.5-flash`) |
| Multipart Upload | `python-multipart` |

---

## 📁 Repository Structure

```
SVS_bitirme/
│
├── Simple_Volleyball_Stats/    # Kotlin Multiplatform client application
│   ├── composeApp/
│   │   └── src/
│   │       ├── commonMain/    # Shared Kotlin code (UI, domain, data)
│   │       ├── androidMain/   # Android-specific implementations
│   │       └── iosMain/       # iOS-specific implementations (expect/actual)
│   ├── iosApp/                # Xcode project entry point for iOS
│   └── build.gradle.kts
│
├── Local_Server/               # Python backend for voice processing
│   ├── main.py                 # FastAPI application (STT + LLM pipeline)
│   ├── llm_parser.py           # Gemini-based action JSON extractor
│   └── requirements.txt        # Python dependencies
│
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Hedgehog or later) with KMP plugin
- **JDK 17+**
- **Python 3.10+**
- **OpenAI API Key** (for Whisper)
- **Google AI API Key** (for Gemini)
- Both the development machine and the Android device must be on the **same Wi-Fi network**

### 1. Run the Local Backend Server

```bash
# Navigate to the server directory
cd Local_Server

# Install dependencies
pip install -r requirements.txt

# Set your API keys as environment variables
export OPENAI_API_KEY="sk-..."
export GEMINI_API_KEY="AIza..."

# Start the server
python main.py
```

The server will start at `http://0.0.0.0:8000`. Note the machine's local IP address (e.g., `192.168.1.X`).

**API Endpoints:**
- `GET /health` — Health check
- `POST /api/v1/transcribe` — Main endpoint: accepts `audio` (file) + `language` (string)
- `POST /api/v1/parse-text` — Debug endpoint: bypasses Whisper, sends text directly to LLM

### 2. Run the Android Client

1. Open `Simple_Volleyball_Stats/` in Android Studio.
2. Sync Gradle.
3. Launch the app on a device or emulator.
4. Go to **Settings** and enter the server's local IP address (e.g., `http://192.168.1.X:8000`).
5. Grant microphone permission when prompted.

> **Note:** The app uses `android:usesCleartextTraffic="true"` to allow HTTP connections on the local network during development. For production, use HTTPS.

> 💡 **Pro Tip — USB Debugging (No Wi-Fi required):** If testing via USB cable, run the following command in your terminal to forward the server port to the device:
> ```bash
> adb reverse tcp:8000 tcp:8000
> ```
> Then set the app's server URL to `http://localhost:8000`. No shared Wi-Fi network needed.

---

## 🤝 Dual Team Mode — How It Works

`DualTeamMatchScreen` runs two `MatchViewModel` instances simultaneously:

- **Home ViewModel (Master):** The single source of truth for score, serving side, set count, and match state.
- **Away ViewModel (Slave):** Only records actions and statistics. It does **not** update the score; instead, it emits a `lastAction` that the master observes via `LaunchedEffect`.

```
[Away action recorded]
       ↓
awayState.lastAction changes
       ↓
LaunchedEffect on master screen
       ↓
homeViewModel.handleIntent(AddHomePoint / AddAwayPoint)
       ↓
homeState updates → LaunchedEffect → awayViewModel.SyncPeerState(...)
```

This prevents score desynchronization and keeps a single DB record per match.

---

## 📄 License

This project was developed for academic purposes as a graduation thesis. All rights reserved.

---

*Developed by Ahmet Yusuf Oğuz — Gebze Technical University, CSE 496, 2025–2026*