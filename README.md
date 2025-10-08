<img src="https://cuplink.net/img/logo.png" alt="CupLink Logo" style="height: 32px; vertical-align: middle; margin-right: 8px;"> **CupLink™**
=================================================================================

**Your privacy, uncompromised** - Serverless, encrypted video and voice calling over v6Space™

**CupLink™** offers a new paradigm in secure communication. It's completely serverless, fully encrypted, and effortlessly private. Built on **v6Space™**, the decentralized IPv6 mesh network infrastructure, CupLink™ enables true peer-to-peer voice and video calling without accounts, servers, or surveillance.

## Two Components. One Seamless Experience.

**CupLink™** is composed of two integrated layers:

### • Overlay Peer-to-peer Network
A resilient, decentralized connectivity layer powered by **v6Space™**, enabling direct communication between devices without accounts or centralized control.

### • The CupLink™ App  
An intuitive, lightweight calling interface that lets you connect instantly in up to 4K+ resolution. It's encrypted end-to-end, key-based, and fully user-controlled.

Perfect for use in home networks, company LANs, community mesh setups, or fully offline scenarios. The application uses Android's VpnService framework to establish the mesh network environment for peer-to-peer communication.

* * *

### 🔐 Core Features

#### Communication Excellence
*   **Peer-to-peer voice and video calling** in ultra-high-definition (supported up to 4K+)
*   **End-to-end+ encryption** built directly into the network layer
*   **Push-to-talk** and speaker mode for flexible communication
*   **Call-spy detection** with intelligent real-time alerts and app boot protection
*   **Android Auto integration** for seamless vehicle communication

#### Privacy & Security
*   **No accounts, servers, or internet access required** - works in same LAN or offline
*   **Private key-based identity management** - no logins, no phone numbers
*   **Self-generated Virtual Static IPv6** addresses for secure identification
*   **Encrypted local backup** of contacts, calls, and settings using libsodium
*   **Zero data collection** - no tracking, no monetization of user data

#### Network Architecture
*   **v6Space™ overlay network access** - decentralized IPv6 mesh infrastructure
*   **QR code based contact exchange** - scan-to-connect simplicity, no setup required
*   **Public peer discovery & auto-routing** using IPv6 multicast
*   **Wi-Fi Direct support** for direct local communication
*   **Self-organizing mesh topology** with automatic peer discovery
*   **Cryptographic IPv6 identities** for secure peer authentication

#### Technical Implementation
*   **Android VpnService framework** - creates secure mesh network overlay
*   **WebRTC protocols** for audio/video communication with built-in encryption
*   **libsodium encryption** for database and communication security
*   **Zero-configuration networking** - plug-and-play connectivity

* * *

### 📱 Supported Platforms

*   Android 5.0 (Lollipop) and up.
    
*   Android Auto (in-call mode).
    
*   Experimental builds for desktop coming soon.
    
*   Support for various mesh network environments including Wi-Fi Direct, IPv6 multicast, and local LAN setups.

* * *

### 🌐 Peer Architecture

CupLink™ acts as a full mesh node in the **v6Space™** decentralized IPv6 overlay network. Each peer can route traffic for others, enabling scalable, serverless communication with the following architecture:

#### Decentralized Mesh Network
*   **Full mesh topology** - every CupLink™ device is a routing node
*   **Self-organizing network** - automatic peer discovery and connection management
*   **No central servers** - completely decentralized communication infrastructure
*   **IPv6 native overlay** - built on cryptographic IPv6 addressing

#### v6Space™ Integration
*   **EUI-64 format addressing** - self-generated cryptographic IPv6 addresses
*   **Multicast peer discovery** - automatic discovery of nearby mesh nodes
*   **Mesh-based routing** - uses intermediate nodes for efficient packet forwarding
*   **Static IPv6 addressing** - predictable, secure network identification

#### Security Model
*   **Cryptographic peer authentication** - verified device identities
*   **End-to-end encryption** - secure communication at the network layer
*   **Zero-trust architecture** - no implicit trust, all connections verified
*   **Self-sovereign networking** - complete user control over network participation

* * *

### 📄 How It Works

CupLink™ operates through a sophisticated 7-step process that creates a completely decentralized communication environment:

#### Step-by-Step Process

1. **Application Installation** - User installs CupLink™ application on Android device
2. **VpnService Activation** - Application automatically starts MainService VpnService in background
3. **Mesh Network Establishment** - VpnService establishes connection using secure tunnel (v6Space™)
4. **IPv6 Address Generation** - Self-generated Virtual Static IPv6 addresses are created and assigned to devices
5. **Overlay Network Routing** - Communication is routed through mesh overlay network without traditional internet infrastructure
6. **Contact Exchange** - Contacts are exchanged via QR codes or text blobs (scan-to-connect simplicity)
7. **Direct Call Establishment** - Calls are established directly using devices' self-generated IPv6 addresses

#### Technical Implementation

*   **QR Code Contact Exchange** - Devices exchange identity and address using QR codes (contact name + static IPv6)
*   **Direct v6Space™ Connection** - CupLink™ connects directly using v6Space™'s cryptographic overlay IPv6 network
*   **Multicast Peer Discovery** - Peer discovery happens via IPv6 multicast; fallback via public peer tables
*   **Sovereign Networking** - No DHCP or external DNS required—fully self-contained networking
*   **Zero-Configuration Setup** - No manual network configuration needed

#### Why This Matters

Most apps route your calls through corporate servers and monetize your identity. CupLink™ does the opposite by removing the server, removing the account, and returning control to you. No ads. No tracking. No compromises.

* * *

### 📦 Build Instructions

#### Prerequisites
*   **Android Studio** - Latest version with Android SDK
*   **Go 1.19+** - Required for building v6Space™ infrastructure components
*   **Linux-based System** - Recommended for development environment

#### Development Setup
*   **Source Building** - Support for building from sources on Linux-based systems
*   **Android Studio Integration** - Full IDE support for Android development
*   **Maven Dependencies** - WebRTC library available through Maven repository
*   **Cross-platform Support** - Build for multiple Android versions and architectures

#### v6Space™ Integration
*   **Mobile.Mesh Library** - Provides RiV-mesh (v6Space™) infrastructure for peer-to-peer communication
*   **VpnService Framework** - Required for establishing mesh network environment
*   **IPv6 Overlay Network** - Built on v6Space™ decentralized mesh infrastructure

#### Building v6Space™ Infrastructure
To build the underlying v6Space™ mesh network infrastructure:

1. **Install Go 1.19+** - Required for building v6Space™ components
2. **Clone v6Space™ Repository**:
   ```bash
   git clone https://github.com/RiV-chain/v6Space.git
   cd v6Space
   ```
3. **Build for Your Platform**:
   ```bash
   # Linux/macOS
   ./build
   
   # Windows
   go mod tidy
   go build -o mesh.exe ./cmd/mesh
   go build -o meshctl.exe ./cmd/meshctl
   go build -o genkeys.exe ./cmd/genkeys
   ```
4. **Generate Configuration**:
   ```bash
   ./mesh -genconf > mesh.conf
   ```

#### Android Development
For Android-specific development:

1. **Android Studio Setup** - Install latest Android Studio with Android SDK
2. **NDK Installation** - Required for native library compilation
3. **Gradle Configuration** - Ensure proper dependency management
4. **Emulator Testing** - Use Android emulator with VPN support for testing

#### Cross-Platform Building
*   **Linux**: Native build support with full feature set
*   **macOS**: Intel and Apple Silicon support
*   **Windows**: MSI installer generation and manual builds
*   **Android**: AAR bundle generation for mobile integration

### 📋 Google Play Compliance

*   CupLink™ uses Android's VpnService framework to create a secure mesh network environment called v6Space™
*   This usage is compliant with Google Play's VpnService policy because it provides VPN as core functionality for peer-to-peer communication
*   No personal data collection or redirection of user traffic for monetization purposes
*   All data from device to VPN tunnel endpoint is encrypted end-to-end

### 📄 Licensing

**CupLink™** is open source software developed by **RiV Chain™ Limited**:
- **License**: GNU General Public License v3.0 (GPLv3)
- **Platform**: Android with VpnService framework
- **Infrastructure**: Built on v6Space™ mesh networking (LGPLv3)
- **Commercial Use**: Allowed with full GPLv3 compliance requirements
- **Source Code**: Available at [GitHub](https://github.com/RiV-chain/CupLink)

**v6Space™ Infrastructure** (used by CupLink™):
- **License**: GNU Lesser General Public License v3.0 (LGPLv3)
- **Scope**: All mesh networking functionality, REST API, protocol implementations, command-line tools
- **Commercial Use**: Allowed with full LGPLv3 compliance requirements
- **Source Code**: Available at [GitHub](https://github.com/RiV-chain/v6Space)

#### Key GPLv3 Requirements:
- **Source Code Sharing**: Must provide CupLink™ source code when distributing
- **Application Code Sharing**: Must share source code of applications using CupLink™
- **Build Instructions**: Must provide installation and build information
- **Modifications**: Any modifications to CupLink™ must remain open source

#### v6Space™ LGPLv3 Requirements:
- **Source Code Sharing**: Must provide v6Space™ source code when distributing
- **Application Code Sharing**: Must share source code of applications using v6Space™
- **Build Instructions**: Must provide installation and build information
- **Modifications**: Any modifications to v6Space™ must remain open source

For complete licensing details, see:
- [Core License](https://github.com/RiV-chain/v6Space/blob/main/LICENSE) - LGPLv3

### 🔧 Troubleshooting

#### Common CupLink™ Issues

##### App Won't Start
If CupLink™ fails to launch:

1. **Check Android Version**: Ensure you're running Android 5.0+ (Lollipop)
2. **Restart Device**: Try restarting your Android device
3. **Clear App Data**: Go to Settings > Apps > CupLink™ > Storage > Clear Data
4. **Reinstall App**: Uninstall and reinstall CupLink™ from Google Play

##### VpnService Permission Issues
If you get permission errors:

1. **Grant VpnService Permission**: When prompted, allow CupLink™ to create VPN connections
2. **Check Device Admin**: Ensure CupLink™ has necessary device permissions
3. **Restart App**: Close and restart CupLink™ after granting permissions
4. **Check Other VPNs**: Disable other VPN apps that might conflict

##### Cannot Make Calls
If calls fail to connect:

1. **Check Mesh Network**: Ensure both devices are connected to v6Space™ mesh
2. **QR Code Issues**: Rescan QR codes if contact exchange failed
3. **Network Connectivity**: Verify both devices have internet or local network access
4. **Contact Verification**: Ensure contact information was exchanged correctly

##### Call Quality Problems
If experiencing poor audio/video quality:

1. **Network Stability**: Check your internet connection or local network
2. **Device Performance**: Close other apps to free up resources
3. **Wi-Fi vs Mobile**: Try switching between Wi-Fi and mobile data
4. **Distance**: Ensure devices are within reasonable range for local mesh

##### Contact Exchange Issues
If QR code scanning fails:

1. **Camera Permissions**: Grant camera permission to CupLink™
2. **QR Code Quality**: Ensure QR code is clear and well-lit
3. **Manual Entry**: Try manually entering contact information
4. **Network Sync**: Wait for mesh network to fully establish before exchanging contacts

#### Getting Help

- **Documentation**: [CupLink™ Documentation](docs/documentation.md)
- **Webpage**: [CupLink™ Community](https://cuplink.net)

* * *
