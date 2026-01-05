import NitroModules
import MetaMaskSDK
import Foundation

final class HybridNitroMetamask: HybridNitroMetamaskSpec {
  private let sdk = MetaMaskSDK.shared

  func connect() -> Promise<ConnectResult> {
    // Use Promise.async with Swift async/await for best practice in Nitro modules
    // Reference: https://nitro.margelo.com/docs/types/promises
    return Promise.async {
      // Based on MetaMask iOS SDK docs: let connectResult = await metamaskSDK.connect()
      // Reference: https://github.com/MetaMask/metamask-ios-sdk
      let connectResult = try await self.sdk.connect()
      
      switch connectResult {
      case .success:
        // After successful connection, get account info from SDK
        // Note: sdk.account is a String (address), not an object
        // Reference: https://raw.githubusercontent.com/MetaMask/metamask-ios-sdk/924d91bb3e98a5383c3082d6d5ba3ddac9e1c565/README.md
        guard let address = self.sdk.account, !address.isEmpty else {
          throw NSError(
            domain: "MetamaskConnector",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "MetaMask SDK returned no address after connection"]
          )
        }
        
        // Parse chainId from hex string (e.g., "0x1") to number
        // Nitro requires chainId to be a number, not a string, for type safety
        guard
          let chainIdHex = self.sdk.chainId,
          !chainIdHex.isEmpty,
          let chainIdInt = Int(chainIdHex.replacingOccurrences(of: "0x", with: ""), radix: 16)
        else {
          throw NSError(
            domain: "MetamaskConnector",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Invalid chainId format"]
          )
        }
        
        return ConnectResult(
          address: address,
          chainId: Double(chainIdInt)
        )
        
      case .failure(let error):
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
      guard let account = self.sdk.account, !account.isEmpty else {
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
      let result = try await self.sdk.request(request)
      
      // Extract signature from response
      // The signature should be a hex-encoded string (0x-prefixed)
      if let signature = result as? String {
        return signature
      } else if let dict = result as? [String: Any], let sig = dict["signature"] as? String ?? dict["result"] as? String {
        return sig
      } else {
        throw NSError(
          domain: "MetamaskConnector",
          code: -1,
          userInfo: [NSLocalizedDescriptionKey: "Invalid signature response format"]
        )
      }
    }
  }
}
