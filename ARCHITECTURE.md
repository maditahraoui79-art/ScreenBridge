# Architecture

```text
Android device
    |
    v
ScreenBridge
    +-- Screen Capture
    +-- Screenshot Scheduler
    +-- Recording Manager
    +-- File Watcher
    +-- Upload Manager
    +-- Storage / Retention
    +-- Local API
    |
    +----> Orb Collector
    +----> Future tools
```

## Core principle

ScreenBridge owns device-level capture and file handling. Consumer projects should only request the data they need.

## First milestone

1. Android app shell
2. User-approved screen capture permission
3. Screenshot service
4. Local screenshot storage
5. Basic recording service
6. Upload abstraction
7. Simple settings screen
