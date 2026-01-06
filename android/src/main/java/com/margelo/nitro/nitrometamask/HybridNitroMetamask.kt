package com.margelo.nitro.nitrometamask

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.margelo.nitro.core.Promise
import com.margelo.nitro.nitrometamask.HybridNitroMetamaskSpec
import com.margelo.nitro.nitrometamask.ConnectResult
import com.margelo.nitro.nitrometamask.MetamaskContextHolder
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.Result
import io.metamask.androidsdk.DappMetadata
import io.metamask.androidsdk.SDKOptions
import io.metamask.androidsdk.EthereumRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class HybridNitroMetamask : HybridNitroMetamaskSpec() {
    // Configurable dapp URL - defaults to novastera.com if not set
    // This is only used for SDK validation - the deep link return is handled via AndroidManifest.xml
    @Volatile
    private var dappUrl: String? = null
    
    // Ethereum SDK instance - lazy initialization
    @Volatile
    private var ethereumInstance: Ethereum? = null
    
    // Track the URL used when creating the current SDK instance
    @Volatile
    private var lastUsedUrl: String? = null
    
    // Get or create Ethereum SDK instance
    // Important: DappMetadata.url must be a valid HTTP/HTTPS URL (not a deep link scheme)
    // The SDK automatically detects and uses the deep link from AndroidManifest.xml
    // Reference: https://raw.githubusercontent.com/MetaMask/metamask-android-sdk/a448378fbedc3afbf70759ba71294f7819af2f37/metamask-android-sdk/src/main/java/io/metamask/androidsdk/DappMetadata.kt
    private val ethereum: Ethereum
        get() {
            val currentUrl = dappUrl ?: "https://novastera.com"
            val existing = ethereumInstance
            val lastUrl = lastUsedUrl
            
            // If not initialized or URL changed, recreate SDK
            if (existing == null || lastUrl != currentUrl) {
                synchronized(this) {
                    // Double-check after acquiring lock
                    val existingAfterLock = ethereumInstance
                    val lastUrlAfterLock = lastUsedUrl
                    if (existingAfterLock == null || lastUrlAfterLock != currentUrl) {
                        val context = MetamaskContextHolder.get()
                        
                        // DappMetadata.url must be a valid HTTP/HTTPS URL for SDK validation
                        // This is separate from the deep link scheme which is auto-detected from AndroidManifest.xml
                        // The deep link return to your app is handled automatically via the manifest
                        val dappMetadata = DappMetadata(
                            name = "Nitro MetaMask Connector",
                            url = currentUrl
                        )
                        val sdkOptions = SDKOptions(
                            infuraAPIKey = null,
                            readonlyRPCMap = null
                        )
                        
                        ethereumInstance = Ethereum(context, dappMetadata, sdkOptions)
                        lastUsedUrl = currentUrl
                        Log.d("NitroMetamask", "Ethereum SDK initialized with DappMetadata.url=$currentUrl. Deep link auto-detected from AndroidManifest.xml")
                    }
                }
            }
            return ethereumInstance!!
        }
    
    
    override fun configure(dappUrl: String?) {
        synchronized(this) {
            val urlToUse = dappUrl ?: "https://novastera.com"
            if (this.dappUrl != urlToUse) {
                this.dappUrl = urlToUse
                // Invalidate existing instance to force recreation with new URL
                ethereumInstance = null
                lastUsedUrl = null
                Log.d("NitroMetamask", "configure: Dapp URL set to $urlToUse. Deep link return is handled automatically via AndroidManifest.xml")
            }
        }
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
            // The SDK will automatically handle deep link return to the app
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
                    
                    // Bring app to foreground after receiving the result
                    // This must be done on the main thread
                    val context = MetamaskContextHolder.get()
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("nitrometamask://mmsdk")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                setPackage(context.packageName)
                            }
                            context.startActivity(intent)
                            Log.d("NitroMetamask", "Brought app to foreground after signing")
                        } catch (e: Exception) {
                            Log.w("NitroMetamask", "Failed to bring app to foreground: ${e.message}")
                        }
                    }
                    
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

    override fun connectSign(nonce: String, exp: Long): Promise<String> {
        // Use Promise.async with coroutines for best practice in Nitro modules
        // Reference: https://nitro.margelo.com/docs/types/promises
        // Based on MetaMask Android SDK: ethereum.connectSign(message)
        // Reference: https://github.com/MetaMask/metamask-android-sdk
        // The SDK's connectSign method handles connection and signing in one call
        return Promise.async {
            // Construct JSON message with only nonce and exp
            // We don't include address or chainID - just encrypt nonce and exp
            val message = org.json.JSONObject().apply {
                put("nonce", nonce)
                put("exp", exp)
            }.toString()
            
            Log.d("NitroMetamask", "connectSign: Constructed message with nonce and exp: $message")
            
            // Use the SDK's connectSign method - it will connect if needed and sign the message
            // This is the recommended approach per MetaMask Android SDK documentation
            val result = suspendCancellableCoroutine<Result> { continuation ->
                Log.d("NitroMetamask", "connectSign: Calling ethereum.connectSign with message")
                ethereum.connectSign(message) { callbackResult ->
                    Log.d("NitroMetamask", "connectSign: Received callback result: ${callbackResult.javaClass.simpleName}")
                    if (continuation.isActive) {
                        continuation.resume(callbackResult)
                    } else {
                        Log.w("NitroMetamask", "connectSign: Continuation not active, ignoring callback")
                    }
                }
            }
            
            Log.d("NitroMetamask", "connectSign: Processing result")
            when (result) {
                is Result.Success.Item -> {
                    val signature = result.value as? String
                        ?: throw Exception("Invalid signature response format")
                    
                    // After connectSign completes, check if we can get the address
                    val address = ethereum.selectedAddress
                    val chainId = ethereum.chainId
                    
                    if (address != null) {
                        Log.d("NitroMetamask", "connectSign: Signature received successfully, address=$address, chainId=$chainId")
                    } else {
                        Log.w("NitroMetamask", "connectSign: Signature received but address is null")
                    }
                    
                    // Bring app to foreground after receiving the result
                    // This must be done on the main thread
                    val context = MetamaskContextHolder.get()
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("nitrometamask://mmsdk")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                setPackage(context.packageName)
                            }
                            context.startActivity(intent)
                            Log.d("NitroMetamask", "Brought app to foreground after connectSign")
                        } catch (e: Exception) {
                            Log.w("NitroMetamask", "Failed to bring app to foreground: ${e.message}")
                        }
                    }
                    
                    signature
                }
                is Result.Success.ItemMap -> {
                    throw IllegalStateException("Unexpected ItemMap result from MetaMask connectSign")
                }
                is Result.Success.Items -> {
                    throw IllegalStateException("Unexpected Items result from MetaMask connectSign")
                }
                is Result.Error -> {
                    val errorMessage = result.error?.message ?: result.error?.toString() ?: "MetaMask connectSign failed"
                    throw Exception(errorMessage)
                }
            }
        }
    }
}
