import { NitroModules } from 'react-native-nitro-modules'

import type { ConnectResult, MetamaskConnector } from './MetamaskConnector.nitro'

export const metamaskConnector =
  NitroModules.createHybridObject<MetamaskConnector>('MetamaskConnector')

export type { ConnectResult, MetamaskConnector }
