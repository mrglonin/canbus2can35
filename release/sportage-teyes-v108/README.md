# Sportage TEYES v108

Чистый пакет приложения Sportage для TEYES CC4 Pro / Android 15.

## Что внутри

- `apk/Sportage_v108_clean_release_signed.apk` - готовый подписанный APK.
- `apk/SHA256SUMS` - SHA-256 контрольная сумма APK.
- `docs/apk_badging.txt` - package/version/permissions из APK.
- `docs/apksigner_verify.txt` - результат проверки подписи.
- `docs/changed_files.txt` - список файлов, попавших в git-пакет.
- `docs/change_stat.txt` - краткая статистика изменений.

## Версия APK

- package: `com.sorento.navi`
- versionCode: `88`
- versionName: `10.8-clean-release`
- minSdk: `29`
- targetSdk: `35`
- подпись: Android Debug, тот же сертификат, что у предыдущих v106/v107 сборок

## Что вычищено

- Убран локальный мусор из Android-проекта: `.gradle`, `.idea`, `build`, `app/build`, старый `dist`, `local.properties`, `.DS_Store`.
- Убран нерабочий LAB-слой из приложения: вкладка LAB, `CanLabRunner`, сценарии replay/dry-run/manual CAN TX и LAB decode/export.
- В git-пакет положен один актуальный APK, без старых v94-v107 сборок, sidecar `.idsig` и Windows wrapper-файла.

## Главное изменение по мультимедиа

Музыка определяется не только через общий TEYES media-service. Добавлен мост к `com.teyes.music.widget`, который читает состояние из `content://com.teyes.music.provider/progress` и использует `songType` как приоритетный источник:

- `is_local_music` -> USB
- `is_local_radio` -> Радио
- `is_bluetooth_music` -> Bluetooth
- `is_cloud_music` / `is_network_music` -> TEYES Music
- CarPlay / Android Auto - по соответствующему `songType`

`MediaMonitor` удерживает активный источник, чтобы старые уведомления Яндекс/USB не перебивали текущий источник после переключения.

## Установка на магнитолу

```bash
adb install -r release/sportage-teyes-v108/apk/Sportage_v108_clean_release_signed.apk
```

Если Wi-Fi ADB:

```bash
adb connect 192.168.1.40:7575
adb install -r release/sportage-teyes-v108/apk/Sportage_v108_clean_release_signed.apk
```

## Проверка после установки

1. Открыть приложение Sportage.
2. В настройках выдать доступ к уведомлениям, если он не выдан.
3. Переключить источники на TEYES: USB, Радио, Bluetooth, Яндекс Музыка.
4. В строке мультимедиа проверить формат:

```text
Мультимедиа: источник / автор / трек / время
```

Ожидаемо:

- при USB источник должен держаться как `USB`;
- при радио источник должен держаться как `Радио`;
- при Bluetooth источник должен держаться как `Bluetooth`;
- Яндекс не должен перебивать USB/радио старыми уведомлениями, если он не активный источник.

## Сборка из исходников

Исходный Android-проект лежит в `android/sportage-app`.

```bash
cd android/sportage-app
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testReleaseUnitTest assembleRelease
```

Подпись debug-ключом:

```bash
BT=/Users/legion/Library/Android/sdk/build-tools/37.0.0
"$BT/zipalign" -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/sportage-aligned.apk
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" "$BT/apksigner" sign \
  --ks /Users/legion/.android/debug.keystore \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out /tmp/Sportage_signed.apk /tmp/sportage-aligned.apk
```

## Не класть в git

- `.gradle/`
- `.idea/`
- `build/`
- `app/build/`
- `dist/`
- `local.properties`
- старые APK/idsig
