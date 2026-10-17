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
Create a native Android application with a first-run model-provider/key setup, task input, execution log, permission checks and foreground execution. Evaluate a bundled Python runtime only after verifying its ABI and every required dependency; replace unsupported native packages and host-only ADB calls rather than silently omitting them.

### Milestone 4 — integration and device tests
Connect the app to the accessibility helper, test on Android 13/ARM64, verify UI observation/action flows, interruption/recovery, network failures, background limits and secure key storage. Publish a standalone APK only when these checks pass.

## Acceptance criteria for calling it standalone

- One APK installs on Android 13+ ARM64 and launches without Termux, a PC, an external server, or ADB running on another device.
- User can configure a supported model provider and enter a task.
- The agent can observe and act across apps using explicit Android accessibility permission.
- Unsupported features are clearly surfaced; no fake success or placeholder automation.
- A clean CI build and on-device smoke test pass.

## Current status

Milestone 1 is the first implementation step. The complete standalone APK is **not yet built**. Do not rename or present the helper APK as the full Artemis app.
