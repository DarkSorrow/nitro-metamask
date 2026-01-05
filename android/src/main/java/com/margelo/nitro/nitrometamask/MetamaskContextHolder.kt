package com.margelo.nitro.nitrometamask

import android.content.Context

/**
 * Context holder for MetaMask SDK initialization.
 * 
 * Nitro does not provide Android Context access, so we must manage it ourselves.
 * This pattern is used by all Nitro modules that need Context (VisionCamera, MMKV, etc.)
 * 
 * The context is initialized from NitroMetamaskPackage when React Native loads the module.
 */
object MetamaskContextHolder {
    @Volatile
    private var appContext: Context? = null

    /**
     * Initialize the context holder with the React Native application context.
     * This should be called once from NitroMetamaskPackage.getModule()
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Get the application context.
     * Throws if not initialized - this ensures we fail fast if the package wasn't loaded correctly.
     */
    fun get(): Context {
        return appContext
            ?: throw IllegalStateException(
                "MetamaskContextHolder not initialized. " +
                "Make sure NitroMetamaskPackage is properly registered in your React Native app."
            )
    }
}
