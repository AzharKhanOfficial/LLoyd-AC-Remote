# Lloyd AC Remote

An Android Studio project that turns an Android phone with a Consumer IR emitter into a Lloyd-compatible AC remote.

## Target device

- OnePlus Nord 3
- Lloyd GLS18I3FWBEW (the model used while developing this project)

## Run it

1. Open this folder in Android Studio.
2. Let Android Studio install/sync the Android Gradle Plugin and SDK 35.
3. Connect the phone with USB debugging enabled.
4. Press **Run**.
5. Point the top edge of the phone at the indoor AC unit.

## Build online without Android Studio

1. Create a GitHub repository and upload this project.
2. Open the repository's **Actions** tab.
3. Run **Build Lloyd AC Remote APK** (or push to the `main` branch).
4. Open the completed workflow run and download the `lloyd-ac-remote-debug-apk` artifact.
5. Extract `app-debug.apk` on your phone and install it.

The included GitHub Actions workflow uses a cloud runner to compile the APK and upload it as a workflow artifact.

## Important

The app uses the Lloyd-compatible ZH/JT-03 IR format documented by the open-source `ac-ir-mqtt-zhjt03` project. AC infrared implementations can vary by model, so test Power first. If Power does not respond, the protocol/code set for the exact remote will need to be adjusted.
