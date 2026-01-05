package com.nitrometamask

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.margelo.nitro.nitrometamask.NitroMetamaskOnLoad

class NitroMetamaskPackage : BaseReactPackage() {
    @Volatile
    private var contextInitialized = false

    override fun getModule(
        name: String,
        reactContext: ReactApplicationContext
    ): NativeModule? {
        // Initialize context on first call (thread-safe)
        if (!contextInitialized) {
            synchronized(this) {
                if (!contextInitialized) {
                    MetamaskContextHolder.initialize(reactContext)
                    contextInitialized = true
                }
            }
        }
        return null
    }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
        // Register a dummy module name to ensure getModule() is called
        // This guarantees context initialization
        return ReactModuleInfoProvider {
            mapOf(
                "NitroMetamaskPackage" to ReactModuleInfo(
                    "NitroMetamaskPackage",
                    "NitroMetamaskPackage",
                    false, // canOverrideExistingModule
                    true, // needsEagerInit
                    true, // hasConstants
                    false, // isCxxModule
                    true // isTurboModule
                )
            )
        }
    }

    companion object {
        init {
            NitroMetamaskOnLoad.initializeNative()
        }
    }
}
