# Android standalone Artemis port

## Goal

Produce a single installable Android APK that runs the Artemis agent on the phone itself, rather than requiring a separate desktop host. This is a porting project; the existing `ArtemisAccessibilityHelper.apk` is only the accessibility/device bridge and is not the agent.

## Findings from the current repository

- The Python package requires Python >= 3.12.
- The runtime uses LangGraph/LangChain, MCP, multiple model-provider SDKs, FastAPI/Uvicorn, ADB/uiautomator2, and native-heavy image/science packages including OpenCV, NumPy, SciPy, Pillow and matplotlib.
- The existing Android helper is a small Java accessibility service using Android SDK APIs. Its job is to expose device UI actions; it does not bundle or execute the Python agent.
- The standard startup path assumes a host environment with system tools and ADB. A phone-only build must replace or redesign those assumptions.

## Port strategy

### Milestone 1 — reproducible Android helper build
Build the existing helper in GitHub Actions and publish the APK as a workflow artifact. This validates Android SDK/Java setup and gives a repeatable baseline without changing helper behavior.

### Milestone 2 — isolate an on-device agent runtime
Inventory imports and runtime entry points, then split host-only features (ADB server/device discovery, IDE/MCP server, desktop capture, system telemetry and notebook tooling) from the core observe/plan/act loop. Keep model API keys in Android private storage and never commit them.

### Milestone 3 — Android app shell and runtime compatibility
The first Android client has now been built as an ARM64 debug APK with CPython 3.12 and the existing accessibility-service implementation. This validates the Android shell, interpreter packaging and helper transport only. Continue with a first-run model-provider/key setup, task input, execution log, and foreground execution. Evaluate every Python/native dependency and replace unsupported host-only ADB calls rather than silently omitting them.

### Milestone 4 — integration and device tests
Connect the app to the accessibility helper, test on Android 13/ARM64, verify UI observation/action flows, interruption/recovery, network failures, background limits and secure key storage. Publish a standalone APK only when these checks pass.

## Acceptance criteria for calling it standalone

- One APK installs on Android 13+ ARM64 and launches without Termux, a PC, an external server, or ADB running on another device.
- User can configure a supported model provider and enter a task.
- The agent can observe and act across apps using explicit Android accessibility permission.
- Unsupported features are clearly surfaced; no fake success or placeholder automation.
- A clean CI build and on-device smoke test pass.

## Current status

- **Milestone 1 — completed:** the existing accessibility helper can be built reproducibly and uploaded by GitHub Actions.
- **Milestone 2 — not started:** isolate the agent loop from host-only ADB/MCP and desktop dependencies.
- **Milestone 3 — initial Android app and interpreter baseline built successfully:** `android-app/` contains a native Android console client and an embedded CPython 3.12 runtime for ARM64. The app starts Python and reports interpreter status, then can display the existing server console through WebView. The interpreter and helper transport are only foundations: the ARTEMIS agent modules, model-provider packages, and Android-native replacements for host-only ADB/UIAutomator paths are not yet integrated. The console still requires a running ARTEMIS backend and is not the standalone agent.
- **Milestone 4 — not started:** real-device validation of an on-device engine and accessibility integration.

The Android console client is built as a debug APK (package `com.artemis.android`, version `0.3.1`, ARM64) and bundles the existing accessibility-service implementation in the same APK. It exposes the helper's authenticated loopback interface to the embedded Python runtime. The successful GitHub Actions build verifies the APK output and uploads it as an artifact. The app has not yet been installed or smoke-tested on a physical Android phone. A separately buildable helper APK remains available for the original host workflow. Neither artifact alone is the complete Artemis agent. Do not describe the current console client as standalone until the Python agent runtime is bundled, the required dependencies work on Android ARM64, and the acceptance tests above pass.
