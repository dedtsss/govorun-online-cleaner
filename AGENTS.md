# Govorun Online Cleaner — agent rules

This is the canonical rule file. Read the task/Issue, this file, and only relevant files.

- Preserve offline GigaAM recognition, dictionary replacement, the fork package/namespace, and upstream attribution.
- Keep online cleanup optional and text-only: never send audio, never enable automatic cleanup by default, and retain user confirmation.
- Do not commit keys, credentials, APKs, downloaded ONNX models, or downloaded AAR files.
- Make focused changes on a branch; avoid unrelated refactors, merges, deployment changes, or destructive actions without explicit authorization.

For code changes, run `./gradlew assembleDebug --no-daemon` after required local model setup, or report exactly why it could not run. Include changed files, commit, build result, and remaining risks.
