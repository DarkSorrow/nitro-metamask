import Foundation
import MetaMaskSDK

class HybridMetamaskConnector: HybridMetamaskConnectorSpec {
  private let sdk = MetaMaskSDK.shared

  func connect() async throws -> ConnectResult {
    try await withCheckedThrowingContinuation { continuation in
      sdk.connect { response in
        switch response {
        case .success(let account):
          let result = ConnectResult(address: account.address, chainId: "\(account.chainId)")
          continuation.resume(returning: result)
        case .failure(let error):
          continuation.resume(throwing: error)
        }
      }
    }
  }

  func signMessage(message: String) async throws -> String {
    // Get the connected account address
    guard let account = sdk.account else {
      throw NSError(domain: "MetamaskConnector", code: -1, userInfo: [NSLocalizedDescriptionKey: "No connected account. Call connect() first."])
    }
    
    // Convert message to hex-encoded format for personal_sign
    // personal_sign expects: personal_sign(messageHex, address)
    // where messageHex is "0x" + hex-encoded UTF-8 bytes
    // Reference: https://github.com/MetaMask/metamask-ios-sdk#5-connect-with-request
    guard let messageData = message.data(using: .utf8) else {
      throw NSError(domain: "MetamaskConnector", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to encode message"])
    }
    
    let messageHex = "0x" + messageData.map { String(format: "%02x", $0) }.joined()
    let params: [String] = [messageHex, account.address]
    
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

