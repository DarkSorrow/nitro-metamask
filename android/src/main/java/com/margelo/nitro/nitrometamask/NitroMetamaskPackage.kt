package com.margelo.nitro.nitrometamask

import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.BaseReactPackage

class NitroMetamaskPackage : BaseReactPackage() {
    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
        // Initialize MetamaskContextHolder with React Native application context
        // This is the ONLY way to get Context in Nitro modules - Nitro doesn't provide Context APIs
        // The context is stored in our own holder and accessed by HybridMetamaskConnector
        MetamaskContextHolder.initialize(reactContext)
        return null
    }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider = ReactModuleInfoProvider { HashMap() }

    companion object {
        init {
            NitroMetamaskOnLoad.initializeNative()
        }
    }
}
