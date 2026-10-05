# OVS-Vault 🔐

> **Private. Offline-first. Built around local data protection.**

OVS-Vault is a security-focused Android application designed to keep sensitive user data protected through local encryption and an offline-first architecture.

## 🔐 Core Features

- 🔒 **AES-256-GCM encryption**
- 🧠 **PBKDF2-HMAC-SHA256 key derivation**
- 📵 **Offline-first data storage**
- 🧬 **Biometric authentication**
- 📝 **Secure notes and code snippets**
- 🗃️ **Room / SQLite local database**
- 🔎 **Integrated database inspection tools**
- 🛡️ **Client-side data protection**

## 🧰 Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-Database-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Firebase AI](https://img.shields.io/badge/Firebase%20AI-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

## 🤖 AI Studio

This project contains the Android project generated/maintained through Google AI Studio workflows.

**AI Studio project:**  
https://ai.studio/apps/aa80b4f9-1518-41d3-ad15-69fb1c9a93d6

## 🏗️ Security Model

OVS-Vault is designed around a local-first security model:

```
User
  ↓
Master Password / Biometrics
  ↓
Key Derivation
  ↓
Encrypted Local Storage
  ↓
Local Data
```

Sensitive data is intended to remain on the device rather than being sent to a remote service.

## 🚀 Run Locally

### Requirements

- Android Studio
- Android SDK 36
- JDK 11
- Android device or emulator

### Setup

Open the project in Android Studio, allow Gradle synchronization to complete, configure any required environment variables in `.env`, and run the debug build.

**Never commit API keys, passwords, keystores or other private credentials.**

## ⚠️ Security Note

This is a development project. Do not treat it as a professionally audited password manager or security product without independent security review.

## 👤 Author

**Imanshu — @Imanshu999**

[GitHub](https://github.com/Imanshu999)

---

**Privacy by design. Build with purpose. 🔐**
