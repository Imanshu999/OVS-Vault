#OVS-Vault (OVS-Vault Vault Service)
​OVS-Vault ek highly secure aur offline-first application hai jo user ke data ko local storage par encrypt kar ke rakhta hai. Iska design "zero-knowledge" principle par based hai, jiska matlab hai ki aapka sensitive data kabhi bhi aapke device se bahar nahi jata.
​Features
​Zero-Knowledge Encryption: AES-256 GCM encryption ka istemal karke data ko locally encrypt kiya jata hai.
​Offline-First Architecture: App internet connection ke bina fully functional hai, jisse data privacy badhti hai.
​Biometric Authentication: Security ko aur majboot karne ke liye biometric lock ki suvidha available hai.
​Code & Note Management: Programmers ke liye code snippets aur generic notes ko tag ke sath save karne ki suvidha.
​Integrated SQLite Explorer: App ke andar hi SQL queries run kar ke data ko inspect aur manage kiya ja sakta hai.
​Technical Specifications
​Key Derivation: PBKDF2WithHmacSHA256.
​Encryption Cipher: AES-256 GCM NoPadding.
​Storage: Local Room Database (SQLite).
​Security Model: Client-side decryption only.
​Getting Started
​App Initialize: Pehli baar app kholne par, apna 'Master Cryptographic Password' set karein.
​Lock/Unlock: App band hone par, database ko unlock karne ke liye master password ya biometric authentication ka use karein.
​Manage Data: 'All Items', 'Programmer Files', ya 'Secure Keys' tabs mein ja kar apna data organize karein.
​Disclaimer
​Yeh application offline use ke liye hai. App ka master key local device par hi rahta hai, isliye apna password hamesha yaad rakhein, kyunki iska koi recovery mechanism nahi hai.

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/aa80b4f9-1518-41d3-ad15-69fb1c9a93d6

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
