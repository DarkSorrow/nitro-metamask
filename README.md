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

**⚠️ Note**: The endpoints shown below (`/social/.well-generated` and `/social/authenticate`) are **example endpoints that you must implement on your backend**. See the [Required Backend Implementation](#required-backend-implementation) section below for details.

```ts
import { metamaskConnector } from 'react-native-metamask-nitro'

export async function authenticateWithMetaMask() {
  try {
    // Step 1: Connect to MetaMask wallet
    const { address, chainId } = await metamaskConnector.connect()
    
    // Step 2: Fetch nonce from your backend (YOU MUST IMPLEMENT THIS ENDPOINT)
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
    
    // Step 5: Submit signature to your backend for authentication (YOU MUST IMPLEMENT THIS ENDPOINT)
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

## Required Backend Implementation

**⚠️ Important**: The authentication flow shown above requires you to implement two backend endpoints. This module only handles the client-side MetaMask connection and message signing—you must build the server-side verification and session management.

### Endpoint 1: Nonce Generation (`POST /social/nonce-generated`)

This endpoint should:
- Generate a unique, cryptographically secure nonce for each authentication attempt
- Return the nonce as plain text
- Optionally associate the nonce with a session or temporary storage
- Set an expiration time for the nonce (recommended: 5-10 minutes)

**Example Implementation:**
```typescript
// Backend endpoint example
app.post('/social/nonce-generated', (req, res) => {
  const nonce = generateSecureNonce() // e.g., crypto.randomBytes(32).toString('hex')
  // Store nonce with expiration (e.g., in Redis or database)
  storeNonce(nonce, { expiresIn: '5m' })
  res.send(nonce)
})
```

### Endpoint 2: Signature Verification (`POST /social/authenticate`)

This endpoint should:
- Receive the message, signature, address, and chainId from the client
- **Verify the signature** cryptographically using the wallet address and message
- Validate the nonce (check it exists, hasn't been used, and hasn't expired)
- Validate the expiration timestamp in the message
- Create a user session or authentication token upon successful verification
- Mark the nonce as used to prevent replay attacks

**Example Implementation:**
```typescript
// Backend endpoint example
app.post('/social/authenticate', async (req, res) => {
  const { message, signature, address, chainId } = req.body
  
  // 1. Parse and validate message
  const messageData = JSON.parse(message)
  if (Date.now() > messageData.exp) {
    return res.status(401).json({ error: 'Message expired' })
  }
  
  // 2. Verify nonce
  if (!await isValidNonce(messageData.nonce)) {
    return res.status(401).json({ error: 'Invalid or expired nonce' })
  }
  
  // 3. Verify signature cryptographically
  const isValid = await verifySignature(message, signature, address)
  if (!isValid) {
    return res.status(401).json({ error: 'Invalid signature' })
  }
  
  // 4. Mark nonce as used
  await markNonceAsUsed(messageData.nonce)
  
  // 5. Create session/token
  const sessionToken = createSession({ address, chainId })
  
  res.json({ token: sessionToken, address, chainId })
})
```

### Signature Verification

You'll need to implement cryptographic signature verification. The signature is created using Ethereum's `personal_sign` standard. Use a library like:
- **Node.js**: `ethers.js` or `web3.js`
- **Python**: `eth-account` or `web3.py`
- **Other languages**: Use appropriate Ethereum signature verification libraries

**Example with ethers.js:**
```typescript
import { ethers } from 'ethers'

async function verifySignature(message: string, signature: string, address: string): Promise<boolean> {
  try {
    const recoveredAddress = ethers.utils.verifyMessage(message, signature)
    return recoveredAddress.toLowerCase() === address.toLowerCase()
  } catch {
    return false
  }
}
```

### Security Requirements

Your backend implementation must:
- ✅ **Always verify signatures server-side** - Never trust client-provided authentication without cryptographic verification
- ✅ **Validate nonces** - Ensure each nonce is used only once and within its expiration window
- ✅ **Check message expiration** - Reject expired authentication messages
- ✅ **Use HTTPS** - All endpoints must use TLS/SSL
- ✅ **Rate limiting** - Implement rate limiting to prevent abuse
- ✅ **Secure session management** - Use secure, httpOnly cookies or signed tokens for sessions

## Security Considerations

- All cryptographic operations are performed natively through official MetaMask SDKs
- Private keys never leave the MetaMask wallet
- Message signing requires explicit user approval via MetaMask's native UI
- Always validate signatures server-side before creating sessions
- Use HTTPS for all network requests
- Implement proper nonce management and expiration handling

## About

This module is actively used in the mobile app of [Novastera.com](https://novastera.com), ensuring reliability and real-world production environments. Novastera is a CRM/ERP system that supports cryptocurrency transactions and Web3 authentication.

## License

Apache License - see [LICENSE](LICENSE) file for details.
