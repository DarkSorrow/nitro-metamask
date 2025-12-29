# Nitro MetaMask Connector

`react-native-metamask-nitro` is a Nitro Module that enables **MetaMask wallet authentication** as an OpenID Connect third-party provider for social login in React Native applications. This module allows users to authenticate with their MetaMask wallet on iOS and Android, similar to how they would authenticate with Facebook, Apple, or Google.

## Purpose

This module is designed for **secure wallet-based authentication** in mobile applications. It provides:

- Native MetaMask SDK integration (iOS + Android) through a single, type-safe HybridObject
- Wallet connection and message signing capabilities for authentication flows
- Secure signature generation for OpenID Connect-style authentication

**⚠️ Security Note:** This module handles authentication credentials and signatures. All cryptographic operations are performed natively through the official MetaMask SDKs to ensure maximum security.

## Features

- `MetamaskConnector.nitro.ts`, defining a future-proof Nitro spec
- Native wrappers around `MetaMaskIOSSDK` and `io.metamask.android:sdk`
- `connect()` method that resolves with `{ address, chainId }` once the wallet session is established
- `signMessage()` method for signing authentication messages (required for login flows)

## Getting Started

```bash
# install deps
yarn install

# regenerate Nitro bindings after editing *.nitro.ts files
npx nitrogen
```

## Native Dependencies

### iOS

- `NitroMetamask.podspec` declares `s.dependency 'MetaMaskIOSSDK'`
- Run `pod install` inside your consuming app after installing this module

### Android

- `android/build.gradle` depends on `io.metamask.android:sdk:1.0.0`
- No manual manifest changes are required—Nitro handles registration

## Nitro Autolinking

`nitro.json` contains:

```json
"autolinking": {
  "MetamaskConnector": {
    "swift": "HybridMetamaskConnector",
    "kotlin": "HybridMetamaskConnector"
  }
}
```

Run `npx nitrogen` whenever the spec or native implementations change so the generated bindings stay in sync.

## TypeScript API

### Basic Connection

```ts
import { metamaskConnector } from 'react-native-metamask-nitro'

export async function connectWallet() {
  const { address, chainId } = await metamaskConnector.connect()
  console.log('Connected to MetaMask', address, chainId)
}
```

### Authentication Flow (OpenID Connect)

For social login authentication, follow this pattern:

```ts
import { metamaskConnector } from 'react-native-metamask-nitro'

export async function authenticateWithMetaMask() {
  try {
    // Step 1: Connect to MetaMask wallet
    const { address, chainId } = await metamaskConnector.connect()
    
    // Step 2: Fetch nonce from your backend
    const nonceResponse = await fetch('/social/.well-generated', {
      method: 'PATCH',
      credentials: 'include',
    })
    const nonce = await nonceResponse.text()
    
    // Step 3: Create authentication message
    const message = JSON.stringify({
      address: address,
      chainID: chainId,
      nonce: nonce,
      exp: Date.now() + 42000, // expiration timestamp
    })
    
    // Step 4: Sign the message with MetaMask
    const signature = await metamaskConnector.signMessage(message)
    
    // Step 5: Submit signature to your backend for authentication
    await fetch('/social/authenticate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        message,
        signature,
        address,
        chainId,
      }),
    })
    
    return { address, chainId, signature }
  } catch (error) {
    console.error('MetaMask authentication failed:', error)
    throw error
  }
}
```

`metamaskConnector` is created via `NitroModules.createHybridObject` and implements the `MetamaskConnector` interface defined in `src/MetamaskConnector.nitro.ts`.

## Native Implementations

- `ios/HybridMetamaskConnector.swift`: wraps `MetaMaskSDK.shared.connect` and message signing using Swift concurrency
- `android/src/main/java/com/margelo/nitro/nitrometamask/HybridMetamaskConnector.kt`: uses `EthereumClient.connect` and `personal_sign` with coroutines

Both platforms normalize responses to shared TypeScript interfaces, ensuring identical behavior across iOS and Android.

## Authentication Flow

The authentication process follows this secure pattern:

1. **Connect**: Establish a connection to the user's MetaMask wallet
2. **Nonce Request**: Fetch a unique nonce from your authentication backend
3. **Message Construction**: Create a JSON message containing address, chainId, nonce, and expiration
4. **Signature**: Request the user to sign the message via MetaMask (native UI)
5. **Verification**: Submit the signature to your backend for verification and session creation

This flow ensures that:
- The user explicitly approves each authentication attempt
- Signatures are cryptographically verifiable
- Nonces prevent replay attacks
- Expiration timestamps limit signature validity

## Security Considerations

- All cryptographic operations are performed natively through official MetaMask SDKs
- Private keys never leave the MetaMask wallet
- Message signing requires explicit user approval via MetaMask's native UI
- Always validate signatures server-side before creating sessions
- Use HTTPS for all network requests
- Implement proper nonce management and expiration handling
