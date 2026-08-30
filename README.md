# ChatNova AI — Premium Android AI Chatbot

A production-grade, local-first Android AI Chatbot application built from scratch with **Kotlin**, **Jetpack Compose (Material 3)**, and **Clean Architecture (MVVM)**.

## ✨ Features

- **Frontier AI Models**: Direct integration with OpenRouter API. Supports **Ox Alpha (GLM-5.3 1M Context)**, DeepSeek R1, Claude 3.5 Sonnet, Gemini 2.0 Flash, and Llama 3.3.
- **Multimodal Attachments**: Real file picker via Android Storage Access Framework for Images (JPEG, PNG, WEBP), Documents (PDF, TXT, JSON, CSV, MD), and Videos.
- **Live Streaming SSE**: Real-time token streaming using OkHttp Server-Sent Events with Stop Generation and Retry support.
- **Markdown & Code Highlighting**: Rich formatted text with syntax code blocks, horizontal scroll, and one-tap copy button.
- **Local-First Privacy**:
  - **Zero Account/Login Requirement**: No signup, email, or passwords needed.
  - **Encrypted API Key Storage**: Secure storage via `EncryptedSharedPreferences` / Android Keystore.
  - **Local Room Database**: All chat history and messages stored on-device.
- **Full History Management**: Search conversations, rename chats, swipe-to-delete, and clear all history.
- **Custom Instructions**: Set global response style (Concise, Coding Expert, In-Depth Tutor) and persistent system prompts.
- **Dynamic Model Selector**: Browse, filter (Free, Vision, Coding), and switch AI models on the fly.
- **Modern Material 3 Theme**: Dark mode, light mode, dynamic system colors, and fluid animations.

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material 3 + Compose BOM 2024.11.00
- **Architecture**: MVVM + Clean Architecture + Repository Pattern
- **Persistence**: Room Database 2.6.1 + DataStore Preferences 1.1.1 + Android Security Crypto
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0 (SSE streaming) + Gson
- **Async**: Kotlin Coroutines + StateFlow / SharedFlow
- **Image Loading**: Coil Compose 2.7.0
- **Min SDK**: 24 (Android 7.0+)
- **Target SDK**: 35 (Android 15+)

## 🚀 Getting Started

1. Open this project in **Android Studio Ladybug / Meerkat** (or newer).
2. Sync Gradle with project files.
3. Run the app on an Android device or emulator.
4. Go to **Settings > OpenRouter API Key** (or launch screen) and paste your free key from [openrouter.ai/keys](https://openrouter.ai/keys).
5. Start chatting!

## 📦 License

MIT License. Designed with excellence for Android.
