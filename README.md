<img src="https://cuplink.net/img/logo.png" alt="CupLink Logo" style="height: 32px; vertical-align: middle; margin-right: 8px;"> **CupLink™**
=================================================================================


**Serverless, encrypted video and voice calling over v6Space™** – no accounts, no infrastructure, no compromises.

**CupLink™** is a fully open-source peer-to-peer calling dApp built on top of **v6Space™**, the decentralized IPv6 mesh overlay from **RiV Chain™**. It enables seamless communication—even without Internet access—by using cryptographic IPv6 identities, QR code pairing, and secure, self-routing connections. Perfect for use in home networks, company LANs, community mesh setups, or fully offline scenarios.

* * *

### 🔐 Core Features

*   Peer-to-peer **voice and video calling** in ultra-high-definition (4K+ supported).
    
*   **No accounts, servers, or internet required**.
    
*   **End-to-end+ encryption** at the network layer.
    
*   Built-in **call-spy detection** and app boot protection.
    
*   **QR-based contact exchange** (no DHCP or DNS setup needed).

*   **Wi-Fi Direct** support for direct local communication.
    
*   Encrypted **backup of contacts, calls, and settings**.
    
*   Public peer discovery & auto-routing using **IPv6 multicast**.
    
*   **Push-to-talk** and speaker mode.
    
*   Works seamlessly over **v6Space™ VpN overlay** or independently on compatible local networks.
    

* * *

### 📱 Supported Platforms

*   Android 5.0 (Lollipop) and up.
    
*   Android Auto (in-call mode).
    
*   Experimental builds for desktop coming soon.
    

* * *

### 🌐 Peer Architecture

CupLink™ acts as a full mesh node in the **v6Space™** network. Each peer can route traffic for others, enabling scalable, serverless communication. No relays, no NAT-tunneling—just static, self-assigned IPv6 addresses and direct transport.

* * *

### 📄 How It Works

*   Devices exchange identity and address using a QR code (contact name + static IPv6).
    
*   CupLink™ connects directly using v6Space™’s cryptographic overlay.
    
*   Peer discovery happens via IPv6 multicast; fallback via public peer tables.
    
*   No DHCP or external DNS required—fully sovereign networking.

*   For comprehensive details, please refer to the [Documentation](docs/Documentation.md) or the [FAQ](docs/faq.md).
    

* * *


### 📦 Build Instructions

*   Starting from version 0.4.6.x, the WebRTC library is pre-built and published in Maven [repo](https://github.com/RiV-chain/artifact). Instructions for building the library can be found [here](https://dev.to/ethand91/webrtc-for-beginners-part-55-building-the-webrtc-android-library-e8l). Additionally, version 0.4.6.x has migrated to the [Unified Plan](https://www.callstats.io/blog/what-is-unified-plan-and-how-will-it-affect-your-webrtc-development).
    

* * *
