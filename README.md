
**wrapper-for-game** is an Android shell module designed to be embedded into any Android game APK. When the game starts, this module is invoked to verify the device’s Android ID against a list stored in Firebase Realtime Database. If the ID is approved, it downloads a game cache ZIP and returns control to the game. If not, it blocks access and prompts the user.

## Features

- Verifies Android device ID against Firebase Realtime Database
- Displays a blocking dialog if the device is not authorized
- Downloads and unpacks a game cache ZIP on approved devices
- Provides progress feedback via notifications and dialogs
- Easily integrated by embedding DEX files and a single static method call

## Prerequisites

- Android Studio or command-line Android SDK (API 23+)
- Java 11
- Firebase project with Realtime Database enabled

## Installation & Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/madnessbrainsbl/wrapper-for-game.git
   cd wrapper-for-game
   ```

2. **Add Firebase configuration**:
   - Download `google-services.json` from your Firebase Console (Project Settings → Add Android app).
   - Place it in `app/`.

3. **Configure Realtime Database**:
   - In Firebase Console, go to **Realtime Database** → **Rules** and set:
     ```json
     {
       "rules": {
         "allowed_ids": { 
           ".read": true,
           ".write": false
         }
       }
     }
     ```
   - Seed example IDs via:
     ```bash
     firebase database:set /allowed_ids ./seed.json
     ```

4. **Build the module**:
   ```bash
   ./gradlew assembleRelease
   ```
   - Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
   - DEX files: `app/build/intermediates/dex/release/mergeDexRelease/classes.dex` and `classes2.dex`

## Integration into Your Game

1. **Extract DEX files** from `mergeDexRelease/` and merge them into your game’s APK.
2. **Invoke the shell** from your game’s entry point (e.g., `MainActivity`) with:
   ```smali
   invoke-static {p0}, Lcom/fmguides/fmfdef/Sdefac;->fmguidesDefender(Landroid/content/Context;)V
   ```
3. **Control flow**:
   - `fmguidesDefender(context)` launches `MainActivity` in this module.
   - Device ID is checked in Firebase.
   - If approved → download cache → unpack → show `Welcome` and return to game.
   - If not approved → show a blocking dialog with options to copy ID or exit.

## Customization

- **CACHE_URL**: Replace the placeholder in `MainActivity.java` with your real ZIP URL:
  ```java
  private static final String CACHE_URL = "https://your-server.com/path/gamecache.zip";
  ```

- **allowed_ids** Data Structure: In `seed.json`, add your device IDs:
  ```json
  {
    "allowed_ids": {
      "-MxKey1": "yourAndroidId1",
      "-MxKey2": "yourAndroidId2"
    }
  }
  ```

## Development

- Open in Android Studio and sync Gradle.
- Edit logic in `app/src/main/java/com/fmguides/fmfdef` as needed.
- Rebuild with `./gradlew assembleDebug` for testing.


