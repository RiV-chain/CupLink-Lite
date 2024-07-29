# CupLink

Voice and video calls without any server or Internet access. Simply scan each other's QR code and call each other. This works in many local networks such as community mesh networks, company networks, or at home. By default, CupLink uses the RiV mesh network for seamless connectivity, but it can also operate independently.

## Features

- Voice and video calls
- No accounts or registration required
- Encrypted communication
- Settings, calls and contacts backup and encryption
- App Settings and Boot password protection
- Ability to add custom addresses to reach contacts
- P2P calls over Wi-Fi Direct
- Neighbours peers auto discovery with IPv6 multicasting
- Public peers publishing
- Push-to-talk
- Serverless

## Supported devices
- Android 5.0 LOLLIPOP and newer
- Android Auto

## Peers

CupLink acts as a mesh network peer gathering many benefits from such topology. Ultimately it does not require any dedicated server to sustain the network. Each CupLink device may redirect traffic
from other such peers.
## Documentation

CupLink exchanges the contact name and IP address via QR code. An IP address is sufficient to connect to clients, and there is no need for a DHCP server. CupLink utilizes RiV Mesh virtual static IPv6 addresses to establish connections.

For comprehensive details, please refer to the [Documentation](docs/Documentation.md) or the [FAQ](docs/faq.md).

## Build Instructions

Starting from version 0.4.6.x, the WebRTC library is pre-built and published in Maven [repo](https://github.com/RiV-chain/artifact). Instructions for building the library can be found [here](https://dev.to/ethand91/webrtc-for-beginners-part-55-building-the-webrtc-android-library-e8l). Additionally, version 0.4.6.x has migrated to the [Unified Plan](https://www.callstats.io/blog/what-is-unified-plan-and-how-will-it-affect-your-webrtc-development).