package com.margelo.nitro.nitrometamask

import com.margelo.nitro.core.Promise
import com.margelo.nitro.modules.NitroModulesContextHolder
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.Result
import io.metamask.androidsdk.DappMetadata
import io.metamask.androidsdk.SDKOptions
import io.metamask.androidsdk.EthereumRequest
import io.metamask.androidsdk.EthereumMethod
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class HybridMetamaskConnector : HybridMetamaskConnectorSpec() {
    // Initialize Ethereum SDK with Context, DappMetadata, and SDKOptions
    // Based on: https://github.com/MetaMask/metamask-android-sdk
    // Using NitroModulesContextHolder for proper Nitro context access (survives reloads, no static leaks)
    private val ethereum: Ethereum by lazy {
        val context = NitroModulesContextHolder.getApplicationContext()
            ?: throw IllegalStateException("Application context not available")
        
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
            // Use direct signMessage() method (requires connection first via connect())
            // This is more explicit and predictable than connectSign() which forces connection
            // Based on SDK docs: ethereum.signMessage() requires address and message
            val address = ethereum.selectedAddress
                ?: throw IllegalStateException("No connected account. Call connect() first.")
            
            // Create EthereumRequest for personal_sign
            val request = EthereumRequest(
                method = EthereumMethod.PERSONAL_SIGN.value,
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

