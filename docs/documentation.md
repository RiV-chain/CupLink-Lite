CupLink Documentation
=======================

## Motivation

Currently, the market of VoIP software is dominated by software owned by big companies and tied to central infrastructure and the Internet in order to monetize the platform and control the data flow. On the other side there a various communities (e.g. [Freifunk](https://freifunk.net/), [RiV-mesh](https://github.com/RiV-chain/RiV-mesh), ..) try to establish various kinds of decentralized networks to connect people with alternative pathways even without Internet. Common VoIP software usually does not work there. This is where CupLink can serve as a use case or even "killer" App for these networks.

## Finding Contacts

CupLink has no discovery mechanism by design. Contacts are shared via QR-Code or text blob.

## How It Connects

CupLink uses a **cryptographically secure IPv6 addressing scheme** derived from Ed25519 public keys, making it impossible to clone or spoof IP addresses. This creates a statically assigned, globally unique address for each contact.

### IPv6 Address Generation Algorithm

The address generation follows this secure process:

1. **Take Ed25519 Public Key** (32 bytes)
2. **Bitwise Invert** the entire public key
3. **Count Leading 1s** in the inverted key
4. **Extract Remaining Bits** (excluding leading 1s and first 0)
5. **Construct IPv6 Address**:
   - **Prefix**: v6Space mesh network prefix (e.g., `fc00::/7`)
   - **Address Bit**: `0` (indicates this is an address, not a prefix)
   - **Leading 1s Count**: 7 bits representing number of leading 1s (0-127)
   - **Remaining Bits**: Truncated to fit 128-bit IPv6 address

### Address Structure
```
[Prefix][0][Leading1sCount][RemainingBits...]
|------|--|---------------|------------------|
 64bit  1bit    7bits          56bits
```

### Security Properties

- **Cryptographically Secure**: Address is derived from Ed25519 public key
- **Unforgeable**: Cannot generate valid address without private key
- **Globally Unique**: Each public key maps to exactly one IPv6 address
- **Static Assignment**: Address never changes for a given public key
- **No IP Cloning**: Impossible to spoof another user's IP address

### Connection Process

1. **Contact Discovery**: User adds contact with their Ed25519 public key
2. **Address Generation**: App generates IPv6 address using IPv6 address generation algorithm
3. **Mesh Network Lookup**: v6Space mesh network routes to the generated address
4. **Direct Connection**: TCP connection established to the cryptographically derived address
5. **Encrypted Signaling**: All call signaling encrypted with contact's public key

### Network Architecture

```
Contact A (Public Key A)          Contact B (Public Key B)
        |                                    |
        |-- Generate IPv6 from Key A --------|
        |                                    |
        |-- v6Space Mesh Network ------------|
        |                                    |
        |-- Direct TCP Connection -----------|
        |                                    |
        |-- Encrypted WebRTC Call -----------|
```

This approach ensures that:
- **No Central Server** is needed for address resolution
- **No DNS** is required for contact lookup
- **No IP Conflicts** can occur between different users
- **Perfect Forward Secrecy** is maintained through key-based addressing
- **Decentralized** communication without single points of failure

## WebRTC

CupLink uses [WebRTC](https://webrtc.org/), a well known library for video- and audio communication. It handles audio and video WebRTC also supports NAT traversal via ICE-Servers, but this feature has been turned off for CupLink, because it is not needed.

Resources:

- [Serverless WebRTC Android](https://github.com/wojta/serverless-webrtc-android)
- [Real time communication with Webrtc on Android](https://hackernoon.com/real-time-communication-with-webrtc-on-android-f96cdcfc4771)
- [Getting Started with WebRTC for Android](https://vivekc.xyz/getting-started-with-webrtc-for-android-daab1e268ff4)
- [WebRTC for Android](https://www.amryousef.me/android-webrtc)
- [Android WebRTC Tutorial](https://github.com/GleasonK/android-webrtc-tutorial)

## Crypto

[libsodium](https://github.com/jedisct1/libsodium) is used for the encryption and authentication of the initial TCP/IP connection (called signalling). After that, WebRTC is establishing a UDP connection for Audio/Video data. WebRTC uses it's own encryption.

### Database

The database contains the settings and contacts. It is stored in the internal/private file store of the app. If no password is used, the database is stored in plain text.

If a password is used, it will be salted and hashed using `libsodium::crypto_pwhash`. The hash is used as key for `libsodium::crypto_secretbox_open_easy` along with a nonce. The salt and nonce are stored along with the database and changed every time the database is stored.

The database file is prefixed with a four byte version header. Currently, it is set to zero.

### Calls

Contacts identities are based on public/secret keys (ed25519). These are used to sign the WebRTC SDP offers using `libsodium::crypto_sign` and are then encrypted using `libsodium.crypto_box_seal` (X25519, XSalsa20-Poly1305) with the recipients public key (curve25519) derived from the identity key.

Every packet is prefixed with a four byte header. Currently it only contains the packet length.

The WebRTC connection itself uses its own crypto scheme.

# Call Protocol Exchange Diagram

## Overview
This diagram shows the complete call protocol exchange between two CupLink Android applications, including the signaling flow, WebRTC negotiation, and media exchange.

## Protocol Flow

```mermaid
sequenceDiagram
    participant A as Caller App
    participant A_UI as Caller UI
    participant A_RTC as Caller RTC
    participant A_MSG as Caller MessageDispatcher
    participant A_SOCK as Caller Socket
    participant B_SOCK as Callee Socket
    participant B_MSG as Callee MessageDispatcher
    participant B_RTC as Callee RTC
    participant B_UI as Callee UI
    participant B as Callee App

    Note over A,B: 1. Call Initiation Phase
    A->>A_UI: User clicks "Call" button
    A_UI->>A_RTC: Create RTCCall for outgoing call
    A_RTC->>A_RTC: Initialize WebRTC factory and settings
    A_RTC->>A_RTC: Create peer connection with RTCConfiguration
    A_RTC->>A_RTC: Add audio/video tracks with hardware acceleration
    A_RTC->>A_RTC: Create data channel for camera state
    A_RTC->>A_RTC: Create offer with SDP constraints
    A_RTC->>A_RTC: Set local description (offer)
    A_RTC->>A_RTC: ICE gathering complete
    A_RTC->>A_MSG: Send call message with offer SDP
    A_MSG->>A_SOCK: Encrypt and send call message
    A_SOCK->>B_SOCK: TCP connection and message transfer
    B_SOCK->>B_MSG: Receive and decrypt call message
    B_MSG->>B_RTC: Process incoming call with offer

    Note over A,B: 2. Call Acceptance Phase
    B_RTC->>B_UI: Accept call (CallActivity)
    B_UI->>B_RTC: Initialize incoming call
    B_RTC->>B_RTC: Create peer connection for incoming call
    B_RTC->>B_RTC: Add audio/video tracks
    B_RTC->>B_RTC: Set remote description (offer)
    B_RTC->>B_RTC: Create answer with SDP constraints
    B_RTC->>B_RTC: Set local description (answer)
    B_RTC->>B_MSG: Send ringing response
    B_MSG->>B_SOCK: Encrypt and send ringing message
    B_SOCK->>A_SOCK: Forward ringing message
    A_SOCK->>A_MSG: Receive and decrypt ringing
    A_MSG->>A_RTC: Report ringing state
    A_UI->>A_UI: Display "Call is ringing"

    Note over A,B: 3. WebRTC Negotiation Phase
    B_RTC->>B_RTC: ICE gathering complete
    B_RTC->>B_MSG: Send connected response with answer SDP
    B_MSG->>B_SOCK: Encrypt and send connected message
    B_SOCK->>A_SOCK: Forward connected message
    A_SOCK->>A_MSG: Receive and decrypt connected
    A_MSG->>A_RTC: Process answer SDP
    A_RTC->>A_RTC: Set remote description (answer)
    A_RTC->>A_RTC: ICE connection established
    B_RTC->>B_RTC: ICE connection established

    Note over A,B: 4. Media Exchange Phase
    A_RTC->>A_RTC: Initialize video with hardware acceleration
    B_RTC->>B_RTC: Initialize video with hardware acceleration
    A_RTC->>A_RTC: Create camera capturer (Camera2Enumerator)
    B_RTC->>B_RTC: Create camera capturer (Camera2Enumerator)
    A_RTC->>A_RTC: Start video capture (1280x720 @ 25fps)
    B_RTC->>B_RTC: Start video capture (1280x720 @ 25fps)
    A_RTC->>A_RTC: Add video/audio tracks to peer connection
    B_RTC->>B_RTC: Add video/audio tracks to peer connection
    A_RTC->>B_RTC: ICE candidates exchange
    B_RTC->>A_RTC: ICE candidates exchange
    A_RTC->>A_RTC: Media stream established
    B_RTC->>B_RTC: Media stream established

    Note over A,B: 5. Data Channel Setup
    A_RTC->>B_RTC: Data channel established
    B_RTC->>A_RTC: Data channel established
    A_RTC->>A_RTC: Register data channel observer
    B_RTC->>B_RTC: Register data channel observer

    Note over A,B: 6. Camera State Management
    A_RTC->>A_RTC: Camera starts disabled by default
    B_RTC->>B_RTC: Camera starts disabled by default
    A_UI->>A_RTC: User toggles camera on/off
    B_UI->>B_RTC: User toggles camera on/off
    A_RTC->>A_MSG: Send camera state via data channel
    B_RTC->>B_MSG: Send camera state via data channel
    A_MSG->>B_MSG: Camera state messages exchange
    B_MSG->>A_MSG: Camera state messages exchange
    A_UI->>A_UI: Update video display based on remote state
    B_UI->>B_UI: Update video display based on remote state

    Note over A,B: 7. Audio Processing
    A_RTC->>A_RTC: Audio processing with hardware acceleration
    B_RTC->>B_RTC: Audio processing with hardware acceleration
    A_RTC->>A_RTC: Echo cancellation and noise suppression
    B_RTC->>B_RTC: Echo cancellation and noise suppression

    Note over A,B: 8. Call Management
    A_UI->>A_RTC: User controls (mute, camera, speaker)
    B_UI->>B_RTC: User controls (mute, camera, speaker)
    A_RTC->>A_RTC: Handle screen lock/unlock events
    B_RTC->>B_RTC: Handle screen lock/unlock events
    A_RTC->>A_RTC: Proximity sensor management
    B_RTC->>B_RTC: Proximity sensor management

    Note over A,B: 9. Call Termination
    A_UI->>A_RTC: User clicks hangup or call ends
    A_RTC->>A_MSG: Send hangup message via data channel
    A_MSG->>B_MSG: Hangup message exchange
    B_MSG->>B_RTC: Process hangup message
    B_RTC->>B_UI: Report call ended
    A_RTC->>A_RTC: Cleanup resources
    B_RTC->>B_RTC: Cleanup resources
    A_SOCK->>B_SOCK: Close TCP connection
    B_SOCK->>A_SOCK: Close TCP connection
```

## Key Protocol Details

### 1. Message Format
All messages are encrypted using the contact's public key and follow this structure:
```json
{
  "action": "call|ringing|connected|dismissed|busy|on_hold|resume",
  "offer": "sdp_offer_string",    // Only in call messages
  "answer": "sdp_answer_string"   // Only in connected messages
}
```

### 2. WebRTC Configuration
- **SDP Semantics**: Unified Plan
- **ICE Servers**: None (direct connection)
- **Bundle Policy**: Max Bundle
- **RTCP Mux Policy**: Require
- **Video Codecs**: H.264 (hardware-accelerated), VP8, VP9
- **Audio Codecs**: Opus, G.722, PCMU, PCMA
- **Video Resolution**: 1280x720 @ 25fps
- **Hardware Acceleration**: Intel VP8 encoder, H.264 high profile

### 3. Camera State Management
- Camera starts **disabled** by default (matches desktop behavior)
- State changes sent via data channel:
  ```json
  {
    "StateChange": "CameraEnabled|CameraDisabled|Hangup"
  }
  ```

### 4. Audio Processing Pipeline
- **Hardware Acceleration**: JavaAudioDeviceModule with hardware AEC/NS
- **Audio Processing**: Echo cancellation, auto gain control, noise suppression
- **Audio Visualization**: Real-time FFT analysis for visual feedback
- **Audio Devices**: Speakerphone, earpiece, wired headset, Bluetooth

### 5. Video Processing
- **Camera Management**: Camera2Enumerator for hardware access
- **Video Capture**: 1280x720 @ 25fps with hardware acceleration
- **Video Rendering**: SurfaceViewRenderer and TextureViewRenderer
- **Camera Switching**: Front/back camera switching support
- **Picture-in-Picture**: Android PiP mode support

### 6. Call States
```kotlin
enum class CallState {
    WAITING, CALLING, RINGING, CONNECTED, RECONNECTING,
    ON_HOLD, BUSY, RESUME, DISMISSED, ENDED,
    ERROR_AUTHENTICATION, ERROR_DECRYPTION, ERROR_CONNECT_PORT,
    ERROR_UNKNOWN_HOST, ERROR_COMMUNICATION, ERROR_NO_CONNECTION,
    ERROR_NO_ADDRESSES, ERROR_NO_NETWORK
}
```

### 7. Error Handling
- **Connection Timeout**: Configurable timeout settings
- **Reconnection**: Automatic reconnection with exponential backoff
- **Fallback**: Try multiple contact addresses
- **Permission Handling**: Runtime permission requests for camera/microphone

### 8. Security Features
- **End-to-End Encryption**: All signaling messages encrypted
- **Key Exchange**: Uses contact's public key for encryption
- **No Central Server**: Direct peer-to-peer communication
- **Message Integrity**: Encrypted message validation
- **Secure Communication**: FC00::/7 address detection

## Network Architecture

```
Caller App                    Callee App
    |                            |
    |-- TCP Connection ----------|
    |                            |
    |-- Encrypted Signaling -----|
    |                            |
    |-- WebRTC Media ------------|
    |                            |
    |-- Data Channel ------------|
```

## Android-Specific Features

### 1. Service Architecture
- **MainService**: Background service for call management
- **CallService**: Foreground service for call notifications
- **CallStatusService**: Service for call status monitoring

### 2. UI Management
- **CallActivity**: Main call interface with full-screen video
- **Picture-in-Picture**: Android PiP mode for background calls
- **Proximity Sensor**: Screen management during calls
- **Audio Focus**: Proper audio focus management

### 3. Hardware Integration
- **Camera2 API**: Modern camera access with hardware acceleration
- **Audio Manager**: Advanced audio routing and device management
- **Power Management**: Wake locks and screen management
- **Bluetooth**: Bluetooth headset support

### 4. Permission Handling
- **Runtime Permissions**: Camera, microphone, phone state
- **Bluetooth Permissions**: Android 12+ Bluetooth connect permission
- **Background Execution**: Proper background service management

## Message Flow Summary

1. **Call Initiation**: Caller creates offer → Encrypt → TCP to callee
2. **Call Acceptance**: Callee auto-accepts → Sends ringing → WebRTC setup
3. **Answer Exchange**: Callee creates answer → Encrypted exchange → WebRTC negotiation
4. **Media Exchange**: ICE negotiation → Media streams → Audio/Video transmission
5. **State Management**: Data channel for camera state and control messages
6. **Call Management**: User controls, screen management, proximity sensor
7. **Call Cleanup**: Hangup messages → Resource cleanup → Connection close
   
## Development

CupLink is Free and Open Source Software. Everybody can participate or even fork the software.

### Building From Sources

On Linux based systems:

```
./gradlew assembleRelease
```

Android Studio works as well.
