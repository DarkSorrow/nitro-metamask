import { type HybridObject } from 'react-native-nitro-modules'

export interface ConnectResult {
  address: string
  chainId: number
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
  connectSign(nonce: string, exp: bigint): Promise<string>
}
