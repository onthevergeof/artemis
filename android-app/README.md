# ARTEMIS Android Console

This is the first Android application shell for ARTEMIS. It provides an installable Android client for the **existing ARTEMIS web console**, preserving the dashboard and task UI supplied by the original server.

## Current scope

- Connect to an existing ARTEMIS server URL.
- Remember the selected server URL locally.
- Display the original ARTEMIS web console in Android WebView.
- Keep same-origin console navigation inside the app and open external links in the browser.
- Support the console's web file chooser.
- Permit cleartext HTTP only for loopback addresses; use HTTPS for remote servers.

## Important limitation

This APK does **not** yet bundle the Python agent, ADB/UIAutomator runtime, model-provider libraries, or MCP server. The ARTEMIS backend must already be running at the configured URL. This client is not yet the requested single-device, offline/standalone Artemis agent.

The separate `ArtemisAccessibilityHelper.apk` is still the device accessibility bridge. It is not the agent runtime.

## Build

From this directory, with JDK 17 and Android SDK platform 35 installed:

```bash
gradle --no-daemon :app:assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```
