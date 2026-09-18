# ScreenBridge

ScreenBridge is a lightweight Android utility project that provides reusable screen and file services for other tools.

## Planned features

- Continuous screen recording
- Periodic screenshots
- File watching
- Automatic upload to a configured storage provider
- Video/file compression
- Retention and cleanup rules
- Local API for other apps to request screenshots or recording actions
- Simple start/stop controls

## Design

ScreenBridge is intentionally separate from projects that consume it. For example, Orb Collector can request screenshots from ScreenBridge instead of implementing screen capture itself.

## Safety

The project is intended for user-controlled automation and personal workflows. It does not include mechanisms for bypassing access controls or covert monitoring.

## Status

Initial project setup. Android implementation will be added next.
