# PocketBite — Android app

PocketBite is an offline-first Android food budget and expense tracker for BDT (৳).

## Included in this starter release

- Monthly budgets, previous/next month navigation, remaining-balance and inclusive remaining-day calculations.
- Editable breakfast/evening allocation percentages (default 60/40).
- Expense entry, editing, deletion, category filters and monthly transaction history.
- Food price catalogue with editable ranges, availability, and per-unit prices.
- Flexible meal planning with quantities and actual-cost recording.
- Monthly category analytics and a recent 7-day spending chart.
- WorkManager reminders for breakfast, evening, daily expense entry, and monthly budget review.
- JSON full backup/restore and CSV expense export using Android's document picker.
- No login, cloud service, internet permission, or advertising SDK.

## Build an installable APK with GitHub Actions

1. Create an empty repository on GitHub named **PocketBite**.
2. Upload the contents of this folder to the repository root (the `.github` folder must also be uploaded).
3. Open the repository's **Actions** tab. The `Build PocketBite APK` workflow starts automatically after the first push. You can also select it and press **Run workflow**.
4. Open the completed workflow run, scroll to **Artifacts**, and download `PocketBite-debug-apk`.
5. Extract the downloaded ZIP. Install `app-debug.apk` on your Android phone. Android may ask you to allow installation from the browser or file manager you used.

The workflow builds a debug APK, suitable for personal testing and direct installation. It is not a Play Store release-signed package.

## Local build

Requires JDK 17, Android SDK platform 35 and Gradle 8.9:

```bash
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## App details

- Application ID: `com.pocketbite.app`
- Version: `1.0.0` (versionCode 1)
- Minimum Android: Android 8.0 (API 26)
- Target Android: API 35
- Currency display: Bangladeshi Taka (৳)

All food prices shown by users are user-managed local values. Any suggested sample prices are only examples, not guaranteed market prices.

## Important data note

The app stores budgets, food items, meal plans and expenses in a local Room database. Export a JSON backup before uninstalling the app or moving to a new phone. Restoring a backup replaces the current PocketBite data after validation.

## Notes for this MVP

- The initial UI labels are English-first; a full Bengali/English language switch is not included in this first build. The app uses Bangladeshi Taka and the local food catalogue is editable.
- Reminder delivery is handled by WorkManager, so Android battery management can defer notifications slightly. Exact-to-the-minute delivery is not guaranteed.
- The app does not ask for network access and does not send stored data to a server.
