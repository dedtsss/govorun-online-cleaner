# Govorun Online Cleaner — agent rules

## Обязательный язык публикаций GitHub

**Весь новый авторский человекочитаемый текст, публикуемый в GitHub от имени владельца, писать по-русски.** Это относится к названиям и описаниям задач, запросам на слияние, комментариям и отзывам, этапам и результатам исполнения, сообщениям коммитов, отчётам, журналам, документации и авторским пояснениям в коде. Перед публикацией проверять язык и исправлять английские шаблонные фразы. Не переводить программные идентификаторы, имена и пути, команды, параметры, API, машинные статусы (`RUN_STARTED`, `SUCCESS`, `BLOCKED` и др.), код и дословные внешние цитаты, если перевод нарушает точность или совместимость. Исключения — только по явному указанию владельца либо при строгой технической необходимости. Исторические записи массово не переписывать. Общее обязательное правило: [политика русского языка](https://github.com/dedtsss/agent-dispatch/blob/main/docs/memory/github-russian-language-policy.md).

This is the canonical rule file. Read the task/Issue, this file, and only relevant files.

- Preserve offline GigaAM recognition, dictionary replacement, the fork package/namespace, and upstream attribution.
- Keep online cleanup optional and text-only: never send audio, never enable automatic cleanup by default, and retain user confirmation.
- Do not commit keys, credentials, APKs, downloaded ONNX models, or downloaded AAR files.
- Make focused changes on a branch; avoid unrelated refactors, merges, deployment changes, or destructive actions without explicit authorization.

For code changes, run `./gradlew assembleDebug --no-daemon` after required local model setup, or report exactly why it could not run. Include changed files, commit, build result, and remaining risks.
