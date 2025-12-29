import type { HybridObject } from 'react-native-nitro-modules'

export interface ConnectResult {
  address: string
  chainId: string
}

export interface MetamaskConnector
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  connect(): Promise<ConnectResult>
  signMessage(message: string): Promise<string>
}

