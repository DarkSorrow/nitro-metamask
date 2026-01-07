import NitroModules
import metamask_ios_sdk
import Foundation

final class HybridNitroMetamask: HybridNitroMetamaskSpec {
  // SDK instance - can be recreated when configure() is called
  // Aligned with Android: SDK is recreated when configure() changes values
  private var sdkInstance: MetaMaskSDK? = nil
  private var lastUsedUrl: String? = nil
  private var lastUsedScheme: String? = nil
  
  // Get or create MetaMask SDK instance
  // Aligned with Android: SDK is recreated when configure() changes values
  private var sdk: MetaMaskSDK {
    let currentUrl = dappUrl ?? "https://metamask.io"
    let currentScheme = deepLinkScheme ?? getDefaultDappScheme()
    
    // Check if we need to recreate the SDK
    if let existing = sdkInstance,
       lastUsedUrl == currentUrl,
       lastUsedScheme == currentScheme {
      return existing
    }
    
    // Check if there's a shared instance we should use (only if it matches our config)
    if let existing = MetaMaskSDK.sharedInstance,
       lastUsedUrl == currentUrl,
       lastUsedScheme == currentScheme {
      sdkInstance = existing
      return existing
    }
    
    // Create new SDK instance with current configuration
    let appMetadata = AppMetadata(
      name: "NitroMetamask",
      url: currentUrl,
      iconUrl: nil,
      base64Icon: nil,
      apiVersion: nil
    )
    
    NSLog("NitroMetamask: Initializing SDK with url=\(currentUrl), scheme=\(currentScheme)")
    
    let newSdk = MetaMaskSDK.shared(
      appMetadata,
      transport: .deeplinking(dappScheme: currentScheme),
      enableDebug: true,
      sdkOptions: nil
    )
    
    sdkInstance = newSdk
    lastUsedUrl = currentUrl
    lastUsedScheme = currentScheme
    
    return newSdk
  }
  
  // Configurable dapp URL and deep link scheme
  private var dappUrl: String? = nil
  private var deepLinkScheme: String? = nil
  
  func configure(dappUrl: String?, deepLinkScheme: String?) {
    let urlToUse = dappUrl ?? "https://metamask.io"
    let schemeToUse = deepLinkScheme ?? getDefaultDappScheme()
    
    var changed = false
    if self.dappUrl != urlToUse {
      self.dappUrl = urlToUse
      changed = true
    }
    if self.deepLinkScheme != schemeToUse {
      self.deepLinkScheme = schemeToUse
      changed = true
    }
    
    if changed {
      // Invalidate existing instance to force recreation with new values
      // This aligns with Android behavior
      sdkInstance = nil
      lastUsedUrl = nil
      lastUsedScheme = nil
      NSLog("NitroMetamask: configure: Dapp URL=\(urlToUse), Scheme=\(schemeToUse). SDK will be recreated on next access.")
    } else {
      NSLog("NitroMetamask: configure: No changes, keeping existing SDK instance.")
    }
  }
  
  // Helper to get default deep link scheme from Info.plist
  private func getDefaultDappScheme() -> String {
    // Try to get the first URL scheme from Info.plist
    if let urlTypes = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]],
       let firstType = urlTypes.first,
       let schemes = firstType["CFBundleURLSchemes"] as? [String],
       let firstScheme = schemes.first {
      return firstScheme
    }
    // Fallback to a default scheme
    return "nitrometamask"
  }

  func connect() -> Promise<ConnectResult> {
    // Use Promise.async with Swift async/await for best practice in Nitro modules
    // Reference: https://nitro.margelo.com/docs/types/promises
    return Promise.async {
      NSLog("NitroMetamask: connect() called")
      
      // Check if MetaMask is installed before attempting to connect
      if !self.sdk.isMetaMaskInstalled {
        let errorMessage = "MetaMask is not installed. Please install MetaMask from the App Store to continue."
        NSLog("NitroMetamask: MetaMask not installed - \(errorMessage)")
        throw NSError(
          domain: "MetamaskConnector",
          code: -2,
          userInfo: [NSLocalizedDescriptionKey: errorMessage]
        )
      }
      
      // Based on MetaMask iOS SDK docs: connect() returns Result<[String], RequestError>
      // Reference: https://github.com/MetaMask/metamask-ios-sdk
      let connectResult = await self.sdk.connect()
      
      NSLog("NitroMetamask: connect() result: \(connectResult)")
      
      switch connectResult {
      case .success:
        // After successful connection, get account info from SDK
        // Note: sdk.account is a String (not optional), check if empty
        // Reference: https://raw.githubusercontent.com/MetaMask/metamask-ios-sdk/924d91bb3e98a5383c3082d6d5ba3ddac9e1c565/README.md
        let address = self.sdk.account
        guard !address.isEmpty else {
          throw NSError(
            domain: "MetamaskConnector",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "MetaMask SDK returned no address after connection"]
          )
        }
        
        // Parse chainId from hex string (e.g., "0x1") to number
        // Nitro requires chainId to be a number, not a string, for type safety
        let chainIdHex = self.sdk.chainId
        guard !chainIdHex.isEmpty,
              let chainIdInt = Int(chainIdHex.replacingOccurrences(of: "0x", with: ""), radix: 16) else {
          throw NSError(
            domain: "MetamaskConnector",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Invalid chainId format"]
          )
        }
        
        return ConnectResult(
          address: address,
          chainId: Int64(chainIdInt)
        )
        
      case .failure(let error):
        NSLog("NitroMetamask: connect() failed: \(error)")
        throw error
      }
    }
  }

  func signMessage(message: String) -> Promise<String> {
    // Use Promise.async with Swift async/await for best practice in Nitro modules
    // Reference: https://nitro.margelo.com/docs/types/promises
    return Promise.async {
      // Use explicit sign() method (requires connection first via connect())
      // This is more explicit and predictable than connectAndSign() which forces connection
      // Nitro encourages explicit object state, not convenience shortcuts
      let account = self.sdk.account
      guard !account.isEmpty else {
        throw NSError(
          domain: "MetamaskConnector",
          code: -1,
          userInfo: [NSLocalizedDescriptionKey: "No connected account. Call connect() first."]
        )
      }
      
      // Create EthereumRequest for personal_sign
      // Based on MetaMask iOS SDK docs: params are [account, message]
      // Reference: https://raw.githubusercontent.com/MetaMask/metamask-ios-sdk/924d91bb3e98a5383c3082d6d5ba3ddac9e1c565/README.md
      let params: [String] = [account, message]
      let request = EthereumRequest(
        method: .personalSign,
        params: params
      )
      
      // Make the request using the SDK's async request method
      // request() returns Result<String, RequestError>
      NSLog("NitroMetamask: signMessage() calling request")
      let result = await self.sdk.request(request)
      
      NSLog("NitroMetamask: signMessage() result: \(result)")
      
      // Extract signature from response
      switch result {
      case .success(let signature):
        return signature
      case .failure(let error):
        NSLog("NitroMetamask: signMessage() failed: \(error)")
        throw error
      }
    }
  }

  func connectSign(nonce: String, exp: Int64) -> Promise<ConnectSignResult> {
    // Use Promise.async with Swift async/await for best practice in Nitro modules
    // Reference: https://nitro.margelo.com/docs/types/promises
    // Based on MetaMask iOS SDK: connectAndSign(message:) convenience method
    // Reference: https://github.com/MetaMask/metamask-ios-sdk
    return Promise.async {
      // Construct JSON message with only nonce and exp
      // We don't include address or chainID - just encrypt nonce and exp
      let messageDict: [String: Any] = [
        "nonce": nonce,
        "exp": exp
      ]
      
      guard let jsonData = try? JSONSerialization.data(withJSONObject: messageDict),
            let message = String(data: jsonData, encoding: .utf8) else {
        throw NSError(
          domain: "MetamaskConnector",
          code: -1,
          userInfo: [NSLocalizedDescriptionKey: "Failed to create JSON message"]
        )
      }
      
      NSLog("NitroMetamask: connectSign: Constructed message with nonce and exp: \(message)")
      
      // Use the SDK's connectAndSign convenience method - it will connect if needed and sign the message
      // This is the recommended approach per MetaMask iOS SDK documentation
      // Reference: https://github.com/MetaMask/metamask-ios-sdk
      // Check if MetaMask is installed before attempting to connect and sign
      if !self.sdk.isMetaMaskInstalled {
        let errorMessage = "MetaMask is not installed. Please install MetaMask from the App Store to continue."
        NSLog("NitroMetamask: MetaMask not installed - \(errorMessage)")
        throw NSError(
          domain: "MetamaskConnector",
          code: -2,
          userInfo: [NSLocalizedDescriptionKey: errorMessage]
        )
      }
      
      NSLog("NitroMetamask: connectSign() calling connectAndSign")
      let connectSignResult = await self.sdk.connectAndSign(message: message)
      
      NSLog("NitroMetamask: connectSign() result: \(connectSignResult)")
      
      switch connectSignResult {
      case .success(let signature):
        // After connectSign completes, get the address and chainId from the SDK
        let address = self.sdk.account
        guard !address.isEmpty else {
          throw NSError(
            domain: "MetamaskConnector",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Failed to retrieve address after connectSign"]
          )
        }
        
        let chainIdHex = self.sdk.chainId
        guard !chainIdHex.isEmpty else {
          throw NSError(
            domain: "MetamaskConnector",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Failed to retrieve chainId after connectSign"]
          )
        }
        
        // Parse chainId from hex string (e.g., "0x1") to Int64
        guard let chainId = Int64(chainIdHex.replacingOccurrences(of: "0x", with: ""), radix: 16) else {
          throw NSError(
            domain: "MetamaskConnector",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Invalid chainId format: \(chainIdHex)"]
          )
        }
        
        NSLog("NitroMetamask: connectSign: Signature received successfully, address=\(address), chainId=\(chainId)")
        
        // Return ConnectSignResult with signature, address, and chainId
        return ConnectSignResult(signature: signature, address: address, chainId: chainId)
        
      case .failure(let error):
        throw error
      }
    }
  }

  func getAddress() -> Promise<Variant_NullType_String> {
    return Promise.async {
      let account = self.sdk.account
      if !account.isEmpty {
        return Variant_NullType_String.second(account)
      } else {
        return Variant_NullType_String.first(NullType.null)
      }
    }
  }

  func getChainId() -> Promise<Variant_NullType_Int64> {
    return Promise.async {
      let chainIdHex = self.sdk.chainId
      guard !chainIdHex.isEmpty else {
        return Variant_NullType_Int64.first(NullType.null)
      }
      
      // Parse chainId from hex string (e.g., "0x1") to Int64 (bigint maps to Int64 in Swift)
      if let chainIdInt = Int64(chainIdHex.replacingOccurrences(of: "0x", with: ""), radix: 16) {
        return Variant_NullType_Int64.second(chainIdInt)
      } else {
        NSLog("NitroMetamask: Invalid chainId format: \(chainIdHex)")
        return Variant_NullType_Int64.first(NullType.null)
      }
    }
  }
}
