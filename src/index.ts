import { NitroModules } from 'react-native-nitro-modules'
import type { NitroMetamask as NitroMetamaskSpec, ConnectResult } from './specs/nitro-metamask.nitro'

/**
 * NitroMetamask - MetaMask connector for React Native
 * 
 * @example
 * ```ts
 * import { NitroMetamask } from '@novastera-oss/nitro-metamask'
 * 
 * const result = await NitroMetamask.connect()
 * const signature = await NitroMetamask.signMessage('Hello')
 * ```
 */
export const NitroMetamask = NitroModules.createHybridObject<NitroMetamaskSpec>('NitroMetamask')

export type { ConnectResult, NitroMetamaskSpec }