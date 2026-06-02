⬢ ORDO
> [Read in Russian / Читать на русском](README_RU.md)

📡 **Website:** [yourordo.com](https://yourordo.com/)

⚠️ **Disclaimer & Current Status:**
This project is currently a **fully functional prototype and an active proof-of-concept**. It is in a state of continuous development, refactoring, and testing. While the core communication mechanics are stable, please note that backward compatibility of local database schemas is not guaranteed at this stage. **App updates may occasionally result in a complete reset of your local chat history on your device.** Please do not use it as your primary channel for critical communications just yet!

---

Hi! I had this idea to try to implement an environment for communication and shaping your thoughts, and to realize it not in the familiar, but essentially unnatural lists and tables, but to make navigation in a fractal (fractal-branching) way. 

On the main screen, you are greeted by three main directions of navigation:

*   **⬢ Me (1.1) ... (Work in Progress):** Space for shaping and structuring personal thoughts (vault, calendar, notes).


*   **⬢ We (1.2):** This is a practically complete block at the moment and one of the most interesting! :) Space for mindful personal dialogues and communication bridges. 

    The main idea I adhered to and wanted to achieve in this block is a minimal digital footprint (why not? :)). Here are the principles it is built on:

    *   **Dialogue only on devices:** Your dialogue is stored only on your phones. The server only passes encrypted text through. If you deleted a message or dialogue on your end, it might only remain with your companion. Nothing accumulates on the server.

    *   **Messages fall instead of flying out:** The first thing you'll need to get slightly used to is that visually, messages fall into your phone rather than flying out of it, unlike everywhere else now. This restores the sense of the "weight" of incoming information and the natural flow of reading.

    *   **Mindful communication:** I wanted to emphasize mindful communication. This is not an environment for sending a grocery list for the evening or shooting a casual "Hey, how are you" without the intention of talking about something specific right now. This is an environment for mindful communication, when you truly have something to talk about.

    *   **Manual status selection:** Initially, your status in a dialogue is always "offline". You decide when you are ready to talk, but you can see your companion's status at any time. You can also switch to draft mode to write a single message for as long as you want, edit it, and send it whenever you feel ready.

    *   **Ping:** There is no point and no way to write a message first so that it arrives as a standard push notification. Instead, you can "ping" your companion directly from the chat window—essentially inviting them to talk. Pings work without commercial push services (like Google FCM) that track your connections and harvest metadata on third-party servers. Instead, a lightweight background service (Foreground Service) runs in the app. It maintains a thin, energy-efficient WebSocket connection with your server. When a ping is sent, the server finds your connection globally by ID (PubKey) and instantly forwards a tiny, silent encrypted packet of just a few bytes. The phone receives this "knock on the door," maps the channel hash locally, and shows a notification: *'You are being called to connect'*.
        
        *(Note: For the ping system to work reliably, you need to grant the app "Unrestricted background execution" permission in your phone's battery settings. Our background service is specifically designed to consume virtually zero battery).*

    *   **Only your own:** This is an environment where only your own people can write. Only those who have gone through a real-time "handshake" (exchanging one-time invitation keys) can be here. There is zero spam from strangers on a hardware level.

    *   **Own servers (Work in Progress):** Currently wrapping up the server so everyone can deploy their own server anywhere, connect only to their own server, and create dialogues there, or even build separate nodes and dialogues on separate servers.


*   **⬢ World / Communities (1.3) ... (Work in Progress):** Space for groups, external feeds, and broadcast channels.


I recorded video instructions because all this might sound a bit confusing from the description alone. Here is a visual demo: [youtube.com/@YOUR_ORDO](https://www.youtube.com/@YOUR_ORDO)

---

## 🛠 Tech Stack

*   **Client (Android):** Written entirely in Kotlin and Jetpack Compose. On-device database is encrypted using SQLCipher (128-bit key).
*   **Server (Go Relay):** Runs exclusively in RAM as a stateless router. Ephemeral sessions are completely wiped from memory upon inactivity.

---

## 🛰 How it works under the hood (Technical Details)

If you are interested in how this works on a protocol level without diving into boring specifications:

### 1. Identity (DNA Key)
There are no registration servers or passwords in the app. Upon first launch, the device generates an **Ed25519** cryptographic key pair. Your public key is your passport. Based on it, a human-readable text shortcode is generated (profile "DNA", e.g., `Gold013189`). 
You can export your keys as an encrypted string for backup and restore your profile on any other device.

### 2. End-to-End Encryption (E2EE)
All messages and media are encrypted on devices using the **AES-256-GCM** algorithm with end-to-end keys generated during pairing. The server never sees your messages in plain text—for the server, it is just a transit of a random byte array.

### 3. Local Pairing Ritual (QR)
When meeting in person, you scan each other's one-time QR codes. This code contains your public key, your server address, and a precise timestamp (milliseconds) signed with your digital signature. Phones verify the signatures, exchange encryption keys, and instantly establish a unique communication bridge.

### 4. Remote Pairing Ritual (Blind Match via Secret Word)
If you are far apart, you can pair remotely. Both of you enter each other's ID and the same secret word. 
Phones send a one-way cryptographic hash of the word to the server, rather than the word itself. The server acts as a "blind switchboard": seeing two identical hashes in memory, it connects your devices in a temporary tunnel for a fraction of a second. Phones "say hello," generate a mutual session salt for channel uniqueness, exchange public encryption keys, and write down the channel connection. The server closes the tunnel and steps away. 
*If someone makes a mistake in even one character of the secret word, your devices will never find each other.*

---

## 🔒 Security Disclaimer & Relay Privacy

The default server address is built into the application and obfuscated in the release binary rather than being exposed in the open-source repository. All messages are strictly end-to-end encrypted (E2EE) and signed via Ed25519.

However, because the client is open-source, any skilled reverse-engineer can extract the server's default domain from the binary. To mitigate unauthorized database spam and automated abuse, the private relay utilizes rate-limiting, strict signature verification, and RAM-only routing. 

If you have heightened metadata privacy concerns or want absolute control over your communications, you will be able to deploy your own instance of the Go Relay (its source code will be published in this repository soon).

---

## ⚖️ License & Concept Protection (Terms of Use)

This project is distributed with specific conditions regarding its use and intellectual property:

1.  **Non-Commercial Use:** You are free to use, study, modify, and share this code. However, any commercial use, monetization, or sale of this code (or its modified versions) is **strictly prohibited**.
2.  **Concept Protection:** The concept of fractal spatial navigation using hexagonal cells, as implemented in this application, is my original intellectual idea. If you wish to utilize this exact navigation mechanic in your own commercial products, you must obtain my explicit written permission.

*The full text of the terms of use can be found in the `LICENSE` file.*

---

I would be glad to read your feedback on the "handshake ritual" and other mechanics. I understand its complexity, but that's the whole point. I also understand that something might not work as intended—please write if you spot any bugs. I'm simply interested in making a reliable, scalable app that allows exchanging information without storing it anywhere other than your personal device. Currently, by the way, I am working on image and video transfer; it will be available in upcoming updates. Overall, I'd be happy to hear your opinion. This app was created strictly as a personal project "for my own circle," and I am sharing it with the world completely for free. I do not monetize it, and I do not plan to display ads.

If you share this philosophy and would like to support my work (help with server hosting bills and motivate the development of new features like video and image transfers, which I am currently working on) — you can do so here:

*   **Boosty (Donations & Subscriptions):** [boosty.to/yourordo](https://boosty.to/yourordo)
*   **Cryptocurrency:**
    *   *USDT (TRC-20): TSqh2SyFVy5jKKsgX3YEYDi946wnAnXsTd
    *   *Bitcoin (BTC): bc1qh0rq6ndzpkfd9nl5fzcnhqnhufz723vtnw525f

---

## ☺ Attributions & Third-Party Licenses

This application features the classic **"Kolobok"** smilies created by Ivan (Aiwan).

*   **Licensing Terms:** These smilies are used with the personal consent of the author. They are distributed strictly under their original license (the license file is located inside the application's assets archive) and are not subject to the main non-commercial license of this repository.
*   In the event of any commercial use of the application's code, investment, or transfer of the project to third parties, the "Kolobok" smilies license must be negotiated and acquired separately from the author (Aiwan).
