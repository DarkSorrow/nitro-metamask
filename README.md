# @novastera-oss/nitro-metamask

Novastera authentication with native mobile libraries

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

For iOS, the MetaMask SDK handles deep linking automatically. No additional configuration is required in `Info.plist`.

## Usage

```typescript
import { NitroMetamask } from '@novastera-oss/nitro-metamask';

// Optional: Configure dapp URL (only needed if you have a website)
// If not called, defaults to "https://novastera.com"
// This URL is ONLY used for SDK validation - the deep link return is handled automatically
NitroMetamask.configure('https://yourdomain.com'); // Optional

// Connect to MetaMask
const connectResult = await NitroMetamask.connect();
console.log('Connected:', connectResult.address, 'Chain:', connectResult.chainId);

// Sign a message (requires connection first)
const signature = await NitroMetamask.signMessage('Hello from my app!');

// Connect and sign in one call (convenience method)
// This constructs a JSON message with address, chainID, nonce, and exp
const nonce = 'random-nonce-123';
const exp = BigInt(Date.now() + 42000); // 42 seconds from now (use BigInt for timestamp)
const signature = await NitroMetamask.connectSign(nonce, exp);
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

2. **Deep Link Return** (automatic):
   - Automatically detected from your `AndroidManifest.xml` intent filter
   - The SDK reads `<data android:scheme="..." android:host="mmsdk" />` and uses it to return to your app
   - This is what actually makes MetaMask return to your app after operations
   - No configuration needed - it's handled automatically

**Summary:** 
- The `configure()` URL is just for SDK validation (you can use the default if you don't have a website)
- The deep link return is handled automatically via your `AndroidManifest.xml`
- Your app will return correctly as long as the manifest is configured properly

## Credits

Bootstrapped with [create-nitro-module](https://github.com/patrickkabwe/create-nitro-module).

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
