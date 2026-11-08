# ARTEMIS Android Console

This is the first Android application shell for ARTEMIS. It provides an installable Android client for the **existing ARTEMIS web console**, preserving the dashboard and task UI supplied by the original server.

## Current scope

- Connect to an existing ARTEMIS server URL.
- Bundle an embedded CPython 3.12 runtime for the Android ARM64 ABI and run a startup diagnostic probe.
- Include the existing ARTEMIS Accessibility Service in the same APK, with an explicit user-controlled Accessibility Settings opt-in.
- Configure a private random session token and expose the helper's authenticated loopback protocol to embedded Python without ADB port forwarding.
- Remember the selected server URL locally.
- Display the original ARTEMIS web console in Android WebView.
- Keep same-origin console navigation inside the app and open external links in the browser.
- Support the console's web file chooser.
- Permit cleartext HTTP only for loopback addresses; use HTTPS for remote servers.

## Important limitation

This APK does **not** yet bundle the Python agent, ADB/UIAutomator runtime, model-provider libraries, or MCP server. The ARTEMIS backend must still be running at the configured URL for the full dashboard and agent. The APK now includes CPython 3.12 and the opt-in accessibility bridge, but the ARTEMIS agent modules and required third-party/native dependencies are not yet bundled or executed on-device. It is not yet the requested single-device, standalone Artemis agent.

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
