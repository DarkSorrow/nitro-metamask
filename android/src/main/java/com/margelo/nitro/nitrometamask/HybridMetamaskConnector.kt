package com.margelo.nitro.nitrometamask

import com.margelo.nitro.Promise
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.Result
import io.metamask.androidsdk.DappMetadata
import io.metamask.androidsdk.SDKOptions
import com.facebook.react.bridge.ReactApplicationContext
import kotlinx.coroutines.suspendCoroutine

class HybridMetamaskConnector : HybridMetamaskConnectorSpec() {
    companion object {
        @Volatile
        private var reactContext: ReactApplicationContext? = null
        
        fun setReactContext(context: ReactApplicationContext) {
            reactContext = context
        }
    }
    
    // Initialize Ethereum SDK with Context, DappMetadata, and SDKOptions
    // Based on: https://github.com/MetaMask/metamask-android-sdk
    // The SDK requires a Context for initialization
    private val ethereum: Ethereum by lazy {
        val context = reactContext?.applicationContext
            ?: throw IllegalStateException("ReactApplicationContext not initialized. Make sure NitroMetamaskPackage is properly registered.")
        
        val dappMetadata = DappMetadata(
            name = "Nitro MetaMask Connector",
            url = "https://novastera.com"
        )
        val sdkOptions = SDKOptions()
        
        Ethereum(context, dappMetadata, sdkOptions)
    }

    override fun connect(): Promise<ConnectResult> {
        // Use Promise.async with coroutines for best practice in Nitro modules
        // Reference: https://nitro.margelo.com/docs/types/promises
        return Promise.async {
            // Convert callback-based connect() to suspend function using suspendCoroutine
            val result = suspendCoroutine<Result> { continuation ->
                ethereum.connect { callbackResult ->
                    continuation.resume(callbackResult)
                }
            }
            
            when (result) {
                is Result.Success.Item -> {
                    // After successful connection, get account info from SDK
                    val address = ethereum.selectedAddress
                        ?: throw IllegalStateException("MetaMask SDK returned no address after connection")
                    val chainId = ethereum.chainId
                        ?: throw IllegalStateException("MetaMask SDK returned no chainId after connection")
                    
                    ConnectResult(
                        address = address,
                        chainId = chainId.toString()
                    )
                }
                is Result.Error -> {
                    throw Exception(result.error.message ?: "Failed to connect to MetaMask")
                }
                else -> {
                    throw IllegalStateException("Unexpected result type from MetaMask connect")
                }
            }
        }
    }

    override fun signMessage(message: String): Promise<String> {
        // Use Promise.async with coroutines for best practice in Nitro modules
        // Reference: https://nitro.margelo.com/docs/types/promises
        return Promise.async {
            // Use the convenience method connectSign() which connects and signs in one call
            // Based on SDK docs: ethereum.connectSign(message) returns Result synchronously
            // Reference: https://github.com/MetaMask/metamask-android-sdk
            when (val result = ethereum.connectSign(message)) {
                is Result.Success.Item -> {
                    // Extract signature from result
                    result.value as? String
                        ?: throw IllegalStateException("Invalid signature response format")
                }
                is Result.Error -> {
                    throw Exception(result.error.message ?: "Failed to sign message")
                }
                else -> {
                    throw IllegalStateException("Unexpected result type from MetaMask signMessage")
                }
            }
        }
    }
}

