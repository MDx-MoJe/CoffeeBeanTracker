# Contributing

[简体中文](CONTRIBUTING.md) | English

Thanks for your interest in BeanBag (CoffeeBeanTracker), an open-source coffee inventory app for coffee lovers. Any form of contribution is welcome.

## Project Structure

```
app/src/main/java/com/coffee/beantracker/
├── data/      # Room entities & DAOs
├── bridge/    # BeanBag interop interface (ContentProvider)
├── utils/     # Helpers (date, flavor tags, privacy policy manager, locale…)
└── *.kt       # Screen Activities & Fragments
```

## Build

```bash
# Android Debug APK
./gradlew assembleDebug
```

Requirements: JDK 17+, Android SDK 34.

## Commit Guidelines

- One commit per logical change; the message should state what and why
- When bumping the version, update both `versionCode` (increment by 1) and `versionName` in `app/build.gradle`
- Never commit keys, passwords, or local paths: `keystore.properties`, `local.properties`, `*.jks` are all in `.gitignore`

## Signing & Privacy

- Self-builds need no signing setup: copy `keystore.properties.example` to `keystore.properties` and fill in your own keystore; without it, release builds automatically fall back to the debug signature
- This app is fully offline: it collects and uploads no data. See the in-app Privacy Policy for details

## Sister Project

- [RoastCurve (烤豆)](https://github.com/MDx-MoJe/RoastCurve) · [Gitee](https://gitee.com/MDx-MoJe/roast-curve): live roast curve monitoring & profile following, linked to this app via ContentProvider. Protocol changes must be released on both sides in sync.

## License

This project is licensed under the [Apache License 2.0](LICENSE). By contributing, you agree to license your contribution under the same terms.
