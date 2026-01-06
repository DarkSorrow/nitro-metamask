import { type HybridObject } from 'react-native-nitro-modules'

export interface ConnectResult {
  address: string
  chainId: number
}

export interface NitroMetamask extends HybridObject<{ ios: 'swift', android: 'kotlin' }> {
  /**
   * Configure the dapp URL for MetaMask SDK validation.
   * This URL is only used for SDK validation - the deep link return is handled automatically via AndroidManifest.xml.
   * 
   * @param dappUrl - A valid HTTP/HTTPS URL (e.g., "https://yourdomain.com"). 
   *                  If not provided, defaults to "https://novastera.com".
   *                  This is separate from the deep link scheme which is auto-detected from your manifest.
   */
  configure(dappUrl?: string): void
  connect(): Promise<ConnectResult>
  signMessage(message: string): Promise<string>
  connectSign(nonce: string, exp: bigint): Promise<string>
}
