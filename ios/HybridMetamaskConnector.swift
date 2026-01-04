import Foundation
import MetaMaskSDK

class HybridMetamaskConnector: HybridMetamaskConnectorSpec {
  private let sdk = MetaMaskSDK.shared

  func connect() async throws -> ConnectResult {
    // Based on MetaMask iOS SDK docs: let connectResult = await metamaskSDK.connect()
    let connectResult = await sdk.connect()
    
    switch connectResult {
    case .success(let value):
      // After successful connection, get account info from SDK
      guard let account = sdk.account else {
        throw NSError(domain: "MetamaskConnector", code: -1, userInfo: [NSLocalizedDescriptionKey: "MetaMask SDK returned no account"])
      }
      return ConnectResult(address: account.address, chainId: "\(account.chainId)")
    case .failure(let error):
      throw error
    }
  }

  func signMessage(message: String) async throws -> String {
    // Get the connected account address
    guard let account = sdk.account else {
      throw NSError(domain: "MetamaskConnector", code: -1, userInfo: [NSLocalizedDescriptionKey: "No connected account. Call connect() first."])
    }
    
    // Based on MetaMask iOS SDK docs, personal_sign params are: [account, message]
    // The SDK handles message encoding internally
    // Reference: https://github.com/MetaMask/metamask-ios-sdk
    let params: [String] = [account.address, message]
    
    // Create EthereumRequest for personal_sign JSON-RPC method
    let request = EthereumRequest(
      method: .personalSign,
      params: params
    )
    
    // Make the request using the SDK's async request method
    let result = try await sdk.request(request)
    
    // Extract signature from response
    // The signature should be a hex-encoded string (0x-prefixed)
    if let signature = result as? String {
      return signature
    } else if let dict = result as? [String: Any], let sig = dict["signature"] as? String ?? dict["result"] as? String {
      return sig
    } else {
      throw NSError(domain: "MetamaskConnector", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid signature response format"])
    }
  }
}

