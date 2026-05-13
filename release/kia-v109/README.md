# Kia v109

Финальный пакет Kia для TEYES CC4 Pro / Android 15.

## Состав

- `apk/Kia_v109_signed.apk` - готовый подписанный APK.
- `apk/SHA256SUMS` - контрольная сумма APK.
- `docs/apk_badging.txt` - package/version из APK.
- `docs/apksigner_verify.txt` - проверка подписи.
- `docs/changed_files.txt` - список файлов релиза.
- `docs/change_stat.txt` - краткая статистика изменений.

## Версия

- package: `kia.app`
- app name: `Kia`
- versionCode: `89`
- versionName: `10.9-kia`

## Установка

```bash
adb install -r release/kia-v109/apk/Kia_v109_signed.apk
```

## Что изменено

- Папка Android-приложения переименована в `android/kia`.
- Android namespace, `applicationId`, Java package и внутренние broadcast action переведены на `kia.app`.
- Название приложения на экране заменено на `Kia`.
- README и sandbox-переменные обновлены под новое короткое имя.

## Сборка из исходников

```bash
cd android/kia
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="/Users/legion/Library/Android/sdk" \
ANDROID_SDK_ROOT="/Users/legion/Library/Android/sdk" \
./gradlew testReleaseUnitTest assembleRelease
```

## Подпись debug-ключом

```bash
BT=/Users/legion/Library/Android/sdk/build-tools/37.0.0
"$BT/zipalign" -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/kia-aligned.apk
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" "$BT/apksigner" sign \
  --ks /Users/legion/.android/debug.keystore \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out /tmp/Kia_signed.apk /tmp/kia-aligned.apk
```
