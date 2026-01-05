package com.nitrometamask

import com.margelo.nitro.core.Promise
import com.margelo.nitro.nitrometamask.HybridNitroMetamaskSpec
import com.margelo.nitro.nitrometamask.ConnectResult
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.Result
import io.metamask.androidsdk.DappMetadata
import io.metamask.androidsdk.SDKOptions
import io.metamask.androidsdk.EthereumRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class HybridNitroMetamask : HybridNitroMetamaskSpec() {
    // Initialize Ethereum SDK with Context, DappMetadata, and SDKOptions
    // Based on: https://github.com/MetaMask/metamask-android-sdk
    // Using MetamaskContextHolder for Context access (Nitro doesn't provide Context APIs)
    // This pattern matches how other Nitro modules handle Context (VisionCamera, MMKV, etc.)
    private val ethereum: Ethereum by lazy {
        val context = MetamaskContextHolder.get()
        
        val dappMetadata = DappMetadata(
            name = "Nitro MetaMask Connector",
            url = "https://novastera.com"
        )
        // SDKOptions constructor requires infuraAPIKey and readonlyRPCMap parameters
        // They can be null for basic usage without Infura or custom RPC
        val sdkOptions = SDKOptions(
            infuraAPIKey = null,
            readonlyRPCMap = null
        )
        
        Ethereum(context, dappMetadata, sdkOptions)
    }

    override fun connect(): Promise<ConnectResult> {
        // Use Promise.async with coroutines for best practice in Nitro modules
        // Reference: https://nitro.margelo.com/docs/types/promises
        return Promise.async {
            // Convert callback-based connect() to suspend function using suspendCancellableCoroutine
            // This handles cancellation properly when JS GC disposes the promise
            val result = suspendCancellableCoroutine<Result> { continuation ->
                ethereum.connect { callbackResult ->
                    if (continuation.isActive) {
                        continuation.resume(callbackResult)
                    }
                }
            }
            
            when (result) {
                is Result.Success.Item -> {
                    // After successful connection, get account info from SDK
                    val address = ethereum.selectedAddress
                        ?: throw IllegalStateException("MetaMask SDK returned no address after connection")
                    val chainIdString = ethereum.chainId
                        ?: throw IllegalStateException("MetaMask SDK returned no chainId after connection")
                    
                    // Parse chainId from hex string (e.g., "0x1") or decimal string to number
                    // Nitro requires chainId to be Double (number in TS maps to Double in Kotlin)
                    val chainId = try {
                        val chainIdInt = if (chainIdString.startsWith("0x") || chainIdString.startsWith("0X")) {
                            chainIdString.substring(2).toLong(16).toInt()
                        } else {
                            chainIdString.toLong().toInt()
                        }
                        chainIdInt.toDouble()
                    } catch (e: NumberFormatException) {
                        throw IllegalStateException("Invalid chainId format: $chainIdString")
                    }
                    
                    ConnectResult(
                        address = address,
                        chainId = chainId
                    )
                }
                is Result.Success.ItemMap -> {
                    // Handle ItemMap case (shouldn't happen for connect, but make exhaustive)
                    throw IllegalStateException("Unexpected ItemMap result from MetaMask connect")
                }
                is Result.Success.Items -> {
                    // Handle Items case (shouldn't happen for connect, but make exhaustive)
                    throw IllegalStateException("Unexpected Items result from MetaMask connect")
                }
                is Result.Error -> {
                    // Result.Error contains the error directly
                    val errorMessage = result.error?.message ?: result.error?.toString() ?: "MetaMask connection failed"
                    throw Exception(errorMessage)
                }
            }
        }
    }

    override fun signMessage(message: String): Promise<String> {
        // Use Promise.async with coroutines for best practice in Nitro modules
        // Reference: https://nitro.margelo.com/docs/types/promises
        return Promise.async {
            // Verify connection state before attempting to sign
            // MetaMask SDK requires an active connection to sign messages
            val address = ethereum.selectedAddress
            if (address.isNullOrEmpty()) {
                throw IllegalStateException("No connected account. Please call connect() first to establish a connection with MetaMask.")
            }
            
            // Create EthereumRequest for personal_sign
            // Based on MetaMask Android SDK docs: params are [account, message]
            // Reference: https://github.com/MetaMask/metamask-android-sdk
            // EthereumRequest constructor expects method as String
            val request = EthereumRequest(
                method = "personal_sign",
                params = listOf(address, message)
            )
            
            // Convert callback-based sendRequest() to suspend function
            val result = suspendCancellableCoroutine<Result> { continuation ->
                ethereum.sendRequest(request) { callbackResult ->
                    if (continuation.isActive) {
                        continuation.resume(callbackResult)
                    }
                }
            }
            
            when (result) {
                is Result.Success.Item -> {
                    // Extract signature from response
                    // The signature should be a hex-encoded string (0x-prefixed)
                    val signature = result.value as? String
                        ?: throw Exception("Invalid signature response format")
                    signature
                }
                is Result.Success.ItemMap -> {
                    // Handle ItemMap case (shouldn't happen for signMessage, but make exhaustive)
                    throw IllegalStateException("Unexpected ItemMap result from MetaMask signMessage")
                }
                is Result.Success.Items -> {
                    // Handle Items case (shouldn't happen for signMessage, but make exhaustive)
                    throw IllegalStateException("Unexpected Items result from MetaMask signMessage")
                }
                is Result.Error -> {
                    // Result.Error contains the error directly
                    val errorMessage = result.error?.message ?: result.error?.toString() ?: "MetaMask signing failed"
                    throw Exception(errorMessage)
                }
            }
        }
    }
}
