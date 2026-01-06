import { type HybridObject } from 'react-native-nitro-modules'

export interface ConnectResult {
  address: string
  chainId: bigint
}

export interface ConnectSignResult {
  signature: string
  address: string
  chainId: bigint
}

export interface NitroMetamask extends HybridObject<{ ios: 'swift', android: 'kotlin' }> {
  /**
   * Configure the dapp URL and deep link scheme for MetaMask SDK.
   * 
   * @param dappUrl - A valid HTTP/HTTPS URL (e.g., "https://yourdomain.com"). 
   *                  If not provided, defaults to "https://novastera.com".
   *                  This is used for SDK validation.
   * @param deepLinkScheme - The deep link scheme from your AndroidManifest.xml (e.g., "nitrometamask").
   *                         If not provided, the library will attempt to auto-detect it.
   *                         This is used to return to your app after MetaMask operations.
   */
  configure(dappUrl?: string, deepLinkScheme?: string): void
  connect(): Promise<ConnectResult>
  signMessage(message: string): Promise<string>
  /**
   * Connect to MetaMask (if not already connected) and sign a message containing nonce and expiration.
   * Returns the signature along with the address and chainId that were used to sign.
   * 
   * @param nonce - A unique nonce for this signing request
   * @param exp - Expiration timestamp (as bigint)
   * @returns Promise resolving to ConnectSignResult containing signature, address, and chainId
   */
  connectSign(nonce: string, exp: bigint): Promise<ConnectSignResult>
  /**
   * Get the currently connected wallet address.
   * Returns null if not connected.
   */
  getAddress(): Promise<string | null>
  /**
   * Get the current chain ID.
   * Returns null if not connected.
   */
  getChainId(): Promise<bigint | null>
}
