## 4.0.1

- [#56] Fix PendingIntent flags issue on Android 12+.

## 4.0.0

- Removed deprecated properties `rationaleText`, `rationaleTitle`, `blockedTitle`, `blockedText`, `resolutionTitle`,
  and `resolutionText` from `Locus.configure{}` block. Check migration guide in [README.md](https://github.com/BirjuVachhani/locus-android/blob/master/README.md).
- Fixed [#46](https://github.com/BirjuVachhani/locus-android/issues/46): location_settings_denied returned while enabling the location.
- Dependency upgrades.

## 3.2.1

- Fixed permission permanently denial - on Android Q+, denying permission 2 times results into permanently denied
  permission.
- Upgraded dependencies
- Updated documentation

## 3.2.0

- Deprecated text configurations from `Locus.configure{}`. Check migration guide
  in [README.md](https://github.com/BirjuVachhani/locus-android/blob/master/README.md).
- Upgraded dependencies
- Upgraded Banner