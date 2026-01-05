import Foundation
import MetaMaskSDK
import NitroModules

class HybridMetamaskConnector: HybridMetamaskConnectorSpec {
  private let sdk = MetaMaskSDK.shared

  func connect() -> Promise<ConnectResult> {
    // Use Promise.async with Swift async/await for best practice in Nitro modules
    // Reference: https://nitro.margelo.com/docs/types/promises
    return Promise.async {
      // Based on MetaMask iOS SDK docs: let connectResult = await metamaskSDK.connect()
      // Reference: https://github.com/MetaMask/metamask-ios-sdk
      let connectResult = try await self.sdk.connect()
      
      switch connectResult {
      case .success(let value):
        // After successful connection, get account info from SDK
        // Note: sdk.account is a String (address), not an object
        // Reference: https://raw.githubusercontent.com/MetaMask/metamask-ios-sdk/924d91bb3e98a5383c3082d6d5ba3ddac9e1c565/README.md
        guard let address = self.sdk.account, !address.isEmpty else {
          throw NSError(domain: "MetamaskConnector", code: -1, userInfo: [NSLocalizedDescriptionKey: "MetaMask SDK returned no address after connection"])
        }
        guard let chainId = self.sdk.chainId, !chainId.isEmpty else {
          throw NSError(domain: "MetamaskConnector", code: -1, userInfo: [NSLocalizedDescriptionKey: "MetaMask SDK returned no chainId after connection"])
        }
        return ConnectResult(address: address, chainId: chainId)
      case .failure(let error):
        throw error
      }
    }
  }

  func signMessage(message: String) -> Promise<String> {
    // Use Promise.async with Swift async/await for best practice in Nitro modules
    // Reference: https://nitro.margelo.com/docs/types/promises
    return Promise.async {
      // Use the convenience method connectAndSign() which connects and signs in one call
      // This is equivalent to Android's connectSign() method
      // Reference: https://raw.githubusercontent.com/MetaMask/metamask-ios-sdk/924d91bb3e98a5383c3082d6d5ba3ddac9e1c565/README.md
      // Example: https://raw.githubusercontent.com/MetaMask/metamask-ios-sdk/924d91bb3e98a5383c3082d6d5ba3ddac9e1c565/Example/metamask-ios-sdk/SignView.swift
      let connectSignResult = try await self.sdk.connectAndSign(message: message)
      
      switch connectSignResult {
      case .success(let signature):
        return signature
      case .failure(let error):
        throw error
      }
    }
  }
}

