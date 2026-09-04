# BeanBag (CoffeeBeanTracker / 豆袋)

[![License](https://img.shields.io/badge/code%20license-Apache%202.0-blue)](LICENSE) [![Trademark](https://img.shields.io/badge/name%20%26%20logo-not%20licensed-important)](NOTICE.txt) [![Platform](https://img.shields.io/badge/platform-Android-brightgreen)](#)

Coffee bean inventory & brew logging app: green bean batches, roasted stock, brew deductions, and roast sync with its sister app. 100% offline.

> Chinese documentation: [README.md (中文)](README.md)

Sister app: [RoastCurve (烤豆)](https://github.com/MDx-MoJe/RoastCurve) · [Gitee](https://gitee.com/MDx-MoJe/roast-curve) — live roast curve monitoring & profile following.

```
BeanBag   (豆袋) → manage beans (inventory, records, cupping)
RoastCurve (烤豆) → manage curves (monitoring, design, export)
```

## Features

- **Green bean batches**: origin, process, variety, altitude, grade, harvest year, remaining weight — full audit trail
- **Roasted stock**: roast date, resting days, best-before reminders; same-name merges automatically
- **Brew logging**: deduct per brew style (pour over / espresso), full history
- **Roast sync**: with RoastCurve installed, each roast automatically deducts green & adds roasted stock (idempotent)
- **Cupping notes**: flavor tags & scores
- **Data backup**: one-tap export/import (zip)
- **Bilingual UI**: switch language in-app (Follow System / 中文 / English), takes effect immediately

100% offline. No ads, no tracking, no accounts.

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Android View system + ViewBinding |
| Database | Room (SQLite) |
| Interop | ContentProvider (cross-app idempotent roast sync) |

## Downloads

Grab the APK from the download page (kept in sync on both platforms):

- **GitHub**: [Releases](https://github.com/MDx-MoJe/CoffeeBeanTracker/releases)
- **Gitee (faster in China)**: [发行版 / Releases](https://gitee.com/MDx-MoJe/coffee-bean-tracker/releases)

## Build

```bash
# Android Debug APK (no signing setup needed)
./gradlew assembleDebug
```

Requirements: JDK 17+, Android SDK 34.

> For release builds, copy `keystore.properties.example` to `keystore.properties`
> with your own keystore; without it, release builds fall back to the debug signature.

## RoastCurve Integration (optional)

The two apps talk over an Android ContentProvider — install either app independently.
With RoastCurve present, each roast auto-deducts green beans and adds roasted stock.
Without it, BeanBag works fully standalone.

## License & Trademark

**Code**: licensed under [Apache License 2.0](LICENSE) — free to use, study, modify and redistribute.

**Not covered by the license**: the app name "豆袋 / CoffeeBeanTracker", the logo, and the official signing certificate remain the property of MDx. Forks must rename the app and replace the logo. See [NOTICE.txt](NOTICE.txt).

## Support

BeanBag is free, open-source and ad-free forever. If it helps, consider a star on [GitHub](https://github.com/MDx-MoJe/CoffeeBeanTracker/stargazers) or [Gitee](https://gitee.com/MDx-MoJe/coffee-bean-tracker), or [sponsoring on Afdian](https://afdian.com/a/RoastCurve). See [SPONSOR.md](SPONSOR.md).

Copyright © 2026 MDx
