## 5.0.0

- Add support for Huawei HMS location Kit by [@Abdullqadir](https://github.com/Abdullqadir).

## 4.1.2

- Fixes [#83](https://github.com/BirjuVachhani/locus-android/issues/83)

## 4.1.1

- Fixes [#77](https://github.com/BirjuVachhani/locus-android/issues/77)
- Refactor some of the usages of deprecated `LocationRequest.create()` to use new `LocationRequest.Builder` API.

## 4.1.0

- Fixes [#65](https://github.com/BirjuVachhani/locus-android/issues/65)

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