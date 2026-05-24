# Paani - Water Reminder App

An easy-to-use, highly accessible water drinking reminder app designed specifically for senior citizens, featuring massive touch targets and simple time-of-day settings.

## Overview

Paani is an Android application designed with accessibility in mind, specifically catering to senior citizens. It helps users stay hydrated by providing simple reminders and an intuitive interface with large touch targets.

## Features

- **High Accessibility:** Massive touch targets for easy interaction.
- **Simple Reminders:** Easy time-of-day settings for water drinking reminders.
- **Local Storage:** Uses Room database (`WaterDatabase.kt`) to keep track of water intake locally.
- **Alarms/Notifications:** Employs AlarmManager and BroadcastReceivers (`WaterReceiverComponents.kt`) for reliable delivery of hydration reminders.
- **AI Integration ready:** Includes basic Gemini API setup for future enhancements.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")` (if present and causing build errors)
6. Run the app on an emulator or physical device

## Project Structure

- `app/src/main/java/`: Contains the Kotlin source code, including the UI (`MainActivity.kt`), the Database components, and the Broadcast receivers for alarms.
- `metadata.json`: Contains the project metadata.
