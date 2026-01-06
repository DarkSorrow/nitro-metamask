# @novastera-oss/nitro-metamask

Novastera authentication with native mobile metamask libraries. Those aims at providing native mobile support for the metamask wallet.

[![Version](https://img.shields.io/npm/v/@novastera-oss/nitro-metamask.svg)](https://www.npmjs.com/package/@novastera-oss/nitro-metamask)
[![Downloads](https://img.shields.io/npm/dm/@novastera-oss/nitro-metamask.svg)](https://www.npmjs.com/package/@novastera-oss/nitro-metamask)
[![License](https://img.shields.io/npm/l/@novastera-oss/nitro-metamask.svg)](https://github.com/patrickkabwe/@novastera-oss/nitro-metamask/LICENSE)

## Requirements

- React Native v0.76.0 or higher
- Node 18.0.0 or higher

> [!IMPORTANT]  
> To Support `Nitro Views` you need to install React Native version v0.78.0 or higher.

## Installation

```bash
npm install @novastera-oss/nitro-metamask react-native-nitro-modules
```

### Android Configuration

**Required:** Add a deep link intent filter to enable MetaMask to return to your app after connecting or signing.

**File:** `android/app/src/main/AndroidManifest.xml`

Add this inside your `MainActivity` `<activity>` tag:

```xml
<activity
  android:name=".MainActivity"
  android:launchMode="singleTask"
  ...>
  <!-- Existing intent filters -->
  
  <!-- Deep link intent filter for MetaMask callback -->
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="nitrometamask" android:host="mmsdk" />
  </intent-filter>
</activity>
```

**Important:** 
- Ensure `android:launchMode="singleTask"` is set on your MainActivity (recommended for deep linking)
- This allows MetaMask to return to your app after the user approves the connection or signature

### iOS Configuration

**For Expo projects:** The package includes an Expo config plugin that automatically adds the required AppDelegate code. Just add the plugin to your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": ["@novastera-oss/nitro-metamask"]
  }
}
```

The plugin will automatically:
- Add the `import MetaMaskSDK` statement
- Add the `application(_:open:options:)` method to handle MetaMask deep links
- Work with both Swift and Objective-C AppDelegate files

**For bare React Native projects:** Add deep link handling manually in your `AppDelegate.swift` (or `AppDelegate.m` for Objective-C):

**File:** `ios/YourAppName/AppDelegate.swift`

```swift
import MetaMaskSDK

// ... existing AppDelegate code ...

// Handle deep links from MetaMask wallet
// MetaMask returns to the app via deep link after signing/connecting
func application(
  _ app: UIApplication,
  open url: URL,
  options: [UIApplication.OpenURLOptionsKey: Any] = [:]
) -> Bool {
  // Check if this is a MetaMask deep link (host="mmsdk")
  if let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
     components.host == "mmsdk" {
    // Handle MetaMask deep link return
    MetaMaskSDK.shared.handleUrl(url)
    return true
  }
  
  // Handle other deep links (e.g., React Native Linking)
  return false
}
```

**Info.plist:** The URL scheme is automatically detected from your `Info.plist` `CFBundleURLSchemes` configuration. Ensure you have a URL scheme configured (e.g., `nitrometamask`).

## Usage

```typescript
import { NitroMetamask } from '@novastera-oss/nitro-metamask';

// Optional: Configure dapp URL and deep link scheme
// If not called, defaults to "https://novastera.com" and auto-detects deep link scheme
// This URL is ONLY used for SDK validation - the deep link return is handled automatically
NitroMetamask.configure('https://yourdomain.com', 'nitrometamask'); // Optional

// Connect to MetaMask
const connectResult = await NitroMetamask.connect();
console.log('Connected:', connectResult.address, 'Chain:', connectResult.chainId);

// Sign a message (requires connection first)
const signature = await NitroMetamask.signMessage('Hello from my app!');

// Connect and sign in one call (convenience method)
// This constructs a JSON message with nonce and exp, then signs it
// Returns signature, address, and chainId together
const nonce = 'random-nonce-123';
const exp = BigInt(Date.now() + 42000); // 42 seconds from now (use BigInt for timestamp)
const result = await NitroMetamask.connectSign(nonce, exp);
console.log('Signature:', result.signature);
console.log('Address:', result.address);
console.log('Chain ID:', result.chainId);

// Get current address and chainId (useful if you need to check after other operations)
const address = await NitroMetamask.getAddress();
const chainId = await NitroMetamask.getChainId();
```

### How Deep Linking Works

**Important:** The MetaMask SDK requires a valid HTTP/HTTPS URL for `DappMetadata.url` validation, but this is **separate** from the deep link that returns to your app.

**Two separate things:**
1. **DappMetadata.url** (optional, configurable):
   - Used only for SDK validation - the SDK checks it's a valid HTTP/HTTPS URL
   - Defaults to `"https://novastera.com"` if not configured
   - You can call `NitroMetamask.configure('https://yourdomain.com')` if you have a website
   - If you don't have a website, the default works fine - it's just for validation
   - [Reference](https://raw.githubusercontent.com/MetaMask/metamask-android-sdk/a448378fbedc3afbf70759ba71294f7819af2f37/metamask-android-sdk/src/main/java/io/metamask/androidsdk/DappMetadata.kt)

2. **Deep Link Return** (automatic or configurable):
   - Automatically detected from your `AndroidManifest.xml` intent filter
   - The SDK reads `<data android:scheme="..." android:host="mmsdk" />` and uses it to return to your app
   - You can also explicitly provide the scheme via `configure()`: `NitroMetamask.configure('https://yourdomain.com', 'nitrometamask')`
   - This is what actually makes MetaMask return to your app after operations
   - If not explicitly configured, the library will attempt to auto-detect it from your manifest

**Summary:** 
- The `configure()` URL is just for SDK validation (you can use the default if you don't have a website)
- The deep link scheme can be auto-detected or explicitly provided via `configure()`
- Your app will return correctly as long as the manifest is configured properly

## About Novastera

[Novastera](https://novastera.com) is a modern CRM and ERP platform designed to streamline business operations and customer relationship management. This library is part of Novastera's open-source ecosystem, providing native mobile MetaMask wallet integration for React Native applications.

**Key Features:**
- Native mobile MetaMask wallet support for iOS and Android
- Seamless deep linking integration
- Secure authentication and message signing
- Built with [Nitro Modules](https://nitro.margelo.com) for optimal performance

Learn more at [novastera.com](https://novastera.com)

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
