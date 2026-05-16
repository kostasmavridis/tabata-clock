# ADR-005: DataStore Preferences over SharedPreferences

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis  
**Tags:** architecture, data, persistence

---

## Context

The app persists five integer settings: `prepareSecs`, `workSecs`, `restSecs`, `rounds`, `sets`. These are read on app launch and written when the user changes a value in the settings panel.

Two standard persistence options exist for small key-value data on Android:

| Option | API | Thread safety | Coroutine support | Error handling |
|---|---|---|---|---|
| `SharedPreferences` | Synchronous (with async `apply()`) | Not safe — `apply()` race conditions | None built-in | Silent — `apply()` failures are swallowed |
| **DataStore Preferences** | `Flow`-based, fully async | Coroutine-safe, serialised writes | Native `Flow` / `suspend` | Exceptions propagated to the `Flow` |

The ViewModel uses Kotlin coroutines and `StateFlow` throughout. Integrating a `Flow`-based persistence layer is architecturally consistent and eliminates the need for thread synchronisation.

## Decision

Use **`androidx.datastore:datastore-preferences`** (Preferences DataStore — key-value, not Proto DataStore).

```kotlin
private val Context.dataStore by preferencesDataStore(name = "tabata_settings")

override val settingsFlow: Flow<TabataSettings> = context.dataStore.data.map { prefs ->
    TabataSettings(
        prepareSecs = prefs[KEY_PREPARE] ?: 10,
        workSecs    = prefs[KEY_WORK]    ?: 20,
        ...
    )
}

override suspend fun saveSettings(settings: TabataSettings) {
    context.dataStore.edit { prefs -> prefs[KEY_PREPARE] = settings.prepareSecs ... }
}
```

The `ISettingsRepository` interface abstracts the DataStore — tests use an in-memory fake that implements the same interface.

## Consequences

### Positive
- Write operations are `suspend` — they participate in structured concurrency and cannot block the main thread
- `settingsFlow` is a cold `Flow` — the ViewModel subscribes with `stateIn(WhileSubscribed(5_000))`, automatically releasing the subscription when there are no active collectors
- Write failures propagate as exceptions in the coroutine — they don't silently disappear like `SharedPreferences.apply()`
- No XML serialisation, no `getString` / `putString` type unsafety — `intPreferencesKey` is typed

### Negative
- First read requires a `runBlocking { repo.settingsFlow.first() }` call in the ViewModel constructor to initialise `secondsLeft` synchronously before the coroutine machinery is ready — a minor design tension
- DataStore files must be excluded from backup to avoid restore-time conflicts (implemented in `backup_rules.xml` and `data_extraction_rules.xml`)

### Mitigations
- The `runBlocking` initialisation block is isolated to a single line and clearly documented in the ViewModel
- The backup exclusion rules are in place (see security fix commit `afbb0f9`)

## Alternatives Considered

### `SharedPreferences`
Synchronous `getString`/`getInt` calls on the main thread would require manual dispatching to an IO dispatcher. `apply()` failures are silently swallowed. Being deprecated in favour of DataStore by Google.

### Room database
A full SQLite abstraction is disproportionate for five integer values. Room is appropriate when the data is relational or queried with complex criteria.

### Proto DataStore
Requires defining a `.proto` schema and a custom `Serializer`. Adds protobuf as a compile dependency. The extra type safety is not justified for five flat integer fields.
