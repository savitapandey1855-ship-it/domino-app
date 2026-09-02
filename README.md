# Domino - Social Media App

Android wrapper app for https://domino6139socialmedia.edgeone.dev/

## Features

- **Pull-to-refresh** — swipe down to refresh page
- **Loading progress bar** — smooth top progress bar
- **Network error page** — "Something went wrong" with retry button
- **Portrait lock** — app stays portrait only
- **No zoom** — pinch-to-zoom disabled
- **No text copy** — text selection/copy disabled
- **File uploads** — camera & gallery support
- **Push notifications** — Firebase Cloud Messaging (FCM) ready
- **Professional UI** — purple theme matching brand

## Build

This project is built via GitHub Actions. Push to `main` or trigger the workflow manually.

## Firebase Setup (for push notifications)

1. Go to https://console.firebase.google.com → create project
2. Add Android app with package ID `com.domino.social`
3. Download `google-services.json` → replace `app/google-services.json`
4. Project Settings → Service Accounts → Generate private key → save as `firebase-service-account.json`
5. Test: `python send_notification.py --title "Test" --body "Hello!" --topic all`

## Package

- **Package ID:** com.domino.social
- **Version:** 1.0.0
- **Min SDK:** 24 (Android 7.0+)
- **Target SDK:** 34 (Android 14)
