Auto Privacy Shield

*Auto Privacy Shield* is an AI-powered Android application that *detects and masks sensitive information in real-time* on your device screen. From faces and Aadhaar/PAN numbers to OTPs and passwords, the app ensures your private data stays protected while screen sharing, recording, or presenting.

Built at a hackathon, this project combines *Computer Vision, OCR, and Android system services* into a seamless privacy protection suite.

---

## Submission
https://youtu.be/WxyfBQApN8Q


## Features

* 🔍 *Face Detection & Masking* — Blur or hide faces automatically in real time.
* 📖 *OCR-powered Redaction* — Detects and masks sensitive text (Aadhaar, PAN, OTPs, phone numbers, etc.) using regex classification.
* 📱 *Notification Privacy* — Intercepts sensitive notification text internally and prevents exposure.
* 🎥 *Screen Capture Protection* — Masks private data during screen recordings and live streams.
* ⚡ *Real-time Processing* — Optimized for speed with efficient bitmap/image processing.
* 🔐 *On-device Privacy* — All processing is done locally; no data leaves the device.

---

## 🏗 Project Structure


Auto-Privacy-Shield/
├── app/
│ └── src/main/
│ ├── AndroidManifest.xml
│ ├── java/com/example/autoprivacyshield/
│ │ ├── MainActivity.java # UI & app entry point
│ │ ├── ScreenCaptureService.java # Foreground service for screen capture
│ │ ├── DetectionUtils.java # ML Kit face & OCR detection
│ │ ├── MaskingUtils.java # Blur/overlay sensitive regions
│ │ ├── OCRActivity.java # OCR on user-selected images
│ │ ├── NotificationService.java # Securely handles notification data
│ │ └── ... # Supporting utils & models
│ └── res/ # Layouts, drawables, strings
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties

---

## 🚀 Getting Started

### Prerequisites

* Android Studio *Arctic Fox (or newer)*
* Android SDK 33+
* Gradle 7+
* Java 11 / Kotlin

### Installation

1. Clone the repository:

   bash
   git clone https://github.com/your-username/Auto-Privacy-Shield.git
   cd Auto-Privacy-Shield
   
2. Open in *Android Studio*.
3. Sync Gradle & build the project.
4. Run on an emulator or physical Android device.

---

## 🔑 Permissions

Auto Privacy Shield requires these permissions to work effectively:

* *📷 Camera* — For detecting and masking faces and OCR scanning.
* *🎥 Media Projection (Screen Capture Permission)* — Required for analyzing live screen frames (granted through a system dialog).
* *🔔 Notification Access* — To read, filter, and mask sensitive notification text internally.
* *⚙ Foreground Service* — Ensures uninterrupted screen capture and masking in the background.
* *💾 Storage (optional)* — To save masked images locally if needed.
* *🌐 Internet (optional)* — Only required if integrating cloud-based OCR/AI models; the core app runs fully offline.

⚠ Without granting these permissions, the app cannot guarantee full privacy protection.

---

## 🔬 Technical Workflow

1. *Screen Capture* → ScreenCaptureService runs as a *foreground service* using *MediaProjection API*, continuously capturing screen frames.
2. *Preprocessing* → Frames are converted into Bitmaps for analysis.
3. *Detection* →

   * *Face Detection* via ML Kit.
   * *OCR* identifies text blocks.
   * Regex filters classify sensitive text (OTP, Aadhaar, PAN, phone numbers).
4. *Masking* → MaskingUtils applies blur or overlays to detected regions.
5. *Notifications* → NotificationService captures and forwards sensitive notification data securely via LocalBroadcastManager, ensuring it never leaves the app.
6. *UI Update* → MainActivity displays live masked frames and processed notifications.

---

## 💡 Use Cases

* Safe *screen recording* without leaking private data.
* Secure *device sharing* with colleagues/friends.
* *Live presentations/streaming* where privacy is critical.
* General-purpose *privacy protection* for everyday Android users.

---

## 🏆 Hackathon Context

This project was built during a hackathon to *redefine digital privacy*. Our mission:

* ✅ Practical in real-world use
* ✅ Technically challenging
* ✅ Socially impactful

By integrating *Computer Vision + OCR + Secure Android Services*, we built a real-time privacy shield prototype ready for future scaling.

---

## 👥 Team

* Dhruthi Rudrangi
* Ganashree P M
* P N BhavyaSree
* Keerthi M

---

## 🌟 Acknowledgments

* [Google ML Kit](https://developers.google.com/ml-kit) for on-device face detection & OCR
* Android Open Source Project
