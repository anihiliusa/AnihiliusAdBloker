# Xtube / AnihiliusAdBloker

Active Android test project in `main`.

## Current active app

- App name: `Xtube`
- Package: `com.anihiliusa.xtube`
- Version: `1.1.2`
- Version code: `112`
- Active Activity: `app/src/main/java/com/anihiliusa/xtube/MainActivity.java`

## Important

Old legacy files may still be visible in the repository because mass delete through the connector was blocked. The active Gradle build ignores them.

The active build is controlled by:

```text
app/build.gradle
```

It includes only the clean Java source path and excludes:

```text
com/anihilius/adblocker/**
**/*.kt
```

## APK output

After a successful GitHub Actions build, the APK is committed back into:

```text
apk/Xtube-v1.1.2-debug.apk
```

## Build workflow

Use:

```text
.github/workflows/build.yml
```

The workflow tests the Gradle project, builds the debug APK, uploads it as artifact, and pushes the APK back into the `apk/` folder.

## Manual build

```bash
gradle :app:assembleDebug --no-daemon --stacktrace
```
