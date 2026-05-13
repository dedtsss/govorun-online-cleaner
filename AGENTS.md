# AGENTS.md

Guidance for AI coding agents working on this repository.

## Core coding-agent rules

These rules are adapted from the Karpathy-style coding-agent guidelines and the project CODEX.md rules.

### 1. Think before coding

- Inspect the real repository before changing files.
- Do not assume missing requirements silently.
- State assumptions when they affect implementation.
- If a request has several meanings, choose the safest small interpretation and say so, or ask when the wrong choice would be costly.
- Push back when a simpler or safer approach exists.

### 2. Simplicity first

- Write the minimum code that solves the task.
- Do not add speculative features, abstractions, configuration, or frameworks.
- Prefer clear, boring code over clever code.
- If the change becomes large, re-check whether a smaller change is enough.

### 3. Surgical changes

- Touch only files directly needed for the task.
- Do not refactor, rename, reformat, or clean adjacent code unless required.
- Match existing style.
- Preserve unrelated user changes.
- Remove only unused code created by your own change.

### 4. Goal-driven execution

- Convert the task into verifiable success criteria.
- For bugs, reproduce the bug first when practical, then fix it.
- Run the smallest relevant check before claiming completion.
- Report changed files, verification performed, and anything not tested.

## Project

This repository is a fork of Govorun Lite.

Current fork goal: keep offline Russian speech recognition via GigaAM and add optional online AI cleanup of already-recognized text.

## Non-negotiable constraints

- Do not remove or break offline GigaAM speech recognition.
- Do not remove or break the user dictionary replacement feature.
- Do not hardcode API keys, tokens, secrets, credentials, or personal data.
- Do not commit generated APK files, downloaded ONNX model files, or downloaded AAR files.
- Do not rename the Kotlin package/namespace unless explicitly requested. The fork uses a different `applicationId` so it can install side-by-side with the upstream app.
- Preserve upstream license notices and attribution.

## Build environment

Before building in a clean environment, run:

```bash
chmod +x ./gradlew
chmod +x ./scripts/*.sh
./scripts/download-sherpa-onnx.sh
./scripts/download-model.sh
```

Then build:

```bash
./gradlew assembleDebug --no-daemon
```

GitHub Actions already does this in `.github/workflows/android-debug-apk.yml`.

## Current Android basics

- Main module: `app`
- Kotlin package/namespace: `com.govorun.lite`
- Fork applicationId: `com.govorun.onlinecleaner`
- Visible app name: `Говорун AI`
- Minimum SDK: 33
- Compile SDK: 35
- Java/Kotlin target: 17
- ABI: `arm64-v8a`

## Important files

- Accessibility service and final text insertion path:
  - `app/src/main/java/com/govorun/lite/service/LiteAccessibilityService.kt`
- Current final insertion method:
  - `pasteText(text: String)`
- Dictionary post-processing:
  - `app/src/main/java/com/govorun/lite/transcriber/Dictionary.kt`
- Offline recognizer/model setup:
  - `app/src/main/java/com/govorun/lite/model/GigaAmModel.kt`
  - `app/src/main/java/com/govorun/lite/transcriber/OfflineTranscriber.kt`
- Existing settings screen:
  - `app/src/main/java/com/govorun/lite/ui/SettingsActivity.kt`
  - `app/src/main/res/layout/activity_settings.xml`
- AI cleanup settings added by the fork:
  - `app/src/main/java/com/govorun/lite/util/AiCleanerPrefs.kt`
  - `app/src/main/java/com/govorun/lite/ui/AiCleanerSettingsActivity.kt`

## v0.2 feature target

Add optional online AI cleanup after offline recognition.

Intended flow:

```text
GigaAM offline recognition
  -> user dictionary replacements
  -> optional AI cleanup of the recognized text
  -> user confirms/copies/inserts/replaces text
```

For v0.2:

- First provider: GigaChat.
- Store only user-provided Authorization Key in app settings.
- Do not send audio to any online service.
- Send only recognized text and only when the user enabled AI cleanup or manually requested cleanup.
- Show errors without crashing: missing key, network error, API error, empty response.
- Do not enable fully automatic cleanup by default. Prefer manual confirmation first.

## Expected agent workflow

For every code change:

1. Inspect the relevant files first.
2. Make the smallest safe change.
3. Run the build command:

```bash
./gradlew assembleDebug --no-daemon
```

4. If the build fails, read the error, fix it, and build again.
5. Report:
   - changed files;
   - exact commit hash;
   - build result;
   - any remaining risks or TODOs.

## Current GitHub Issues

- #1: Update fork branding in About screen, README and app texts.
- #2: v0.2: Add online AI cleanup for recognized text.
