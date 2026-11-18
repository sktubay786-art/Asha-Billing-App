# ASHA Billing (Android)

Native Android rewrite of **ASHA Billing** (`ASHA Mini Shallow`) built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Firebase Auth (Google Sign-In via Credential Manager)**, and **Cloud Firestore**.

## Features

- **Google Sign-In & Cloud Sync**: Real-time Firestore synchronization scoped per authenticated owner (`users/{userId}/...`) with zero-trust security rules (`firestore.rules`).
- **Overview Dashboard**: Live total outstanding balance, collected total, customer and invoice counts, collection progress bar, quick actions, and recent invoices.
- **Users Directory & Ledger**: Searchable user directory with due/cleared filter pills, due summaries, add/edit forms, full customer ledger, WhatsApp payment reminders, balance settlements, and cascade deletion.
- **New Bill Creation**: Stepped invoice builder with live emerald total payable banner, land unit conversion (`1 Bigha = 40 Decimal = 20 Katha`), automatic previous due carry-forward, initial cash payment entry, and printable/shareable invoices.
- **Payments & History**: Invoice-linked payment recording (`Cash`, `UPI`, `Bank transfer`, `Other`), searchable invoice history with status filters (`Due`, `Partial`, `Paid`), and native Android `PrintManager` invoice printing.
- **Calculator & Business Settings**: Built-in safe expression calculator supporting brackets, percentages, and operator precedence, alongside editable business profile and UPI details.

## Verification

- **Firestore Security Rules**: Tested locally via `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`
- **Android Build**: Built cleanly with Gradle (`compile_applet` / `gradle :app:assembleDebug`)
