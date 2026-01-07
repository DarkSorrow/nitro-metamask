package com.margelo.nitro.nitrometamask

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.margelo.nitro.core.Promise
import com.margelo.nitro.core.NullType
import com.margelo.nitro.nitrometamask.HybridNitroMetamaskSpec
import com.margelo.nitro.nitrometamask.ConnectResult
import com.margelo.nitro.nitrometamask.MetamaskContextHolder
import com.margelo.nitro.nitrometamask.Variant_NullType_String
import com.margelo.nitro.nitrometamask.Variant_NullType_Long
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
    
    // Configurable deep link scheme - if not set, will attempt auto-detection
    @Volatile
    private var configuredDeepLinkScheme: String? = null
    
    // Ethereum SDK instance - lazy initialization
    @Volatile
    private var ethereumInstance: Ethereum? = null
    
    // Track the URL used when creating the current SDK instance
    @Volatile
    private var lastUsedUrl: String? = null
    
    // Cache the detected deep link scheme to avoid repeated detection
    @Volatile
    private var cachedDeepLinkScheme: String? = null
    
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
    
    
    override fun configure(dappUrl: String?, deepLinkScheme: String?) {
        synchronized(this) {
            val urlToUse = dappUrl ?: "https://novastera.com"
            val schemeToUse = deepLinkScheme?.takeIf { it.isNotEmpty() }
            
            var changed = false
            if (this.dappUrl != urlToUse) {
                this.dappUrl = urlToUse
                changed = true
            }
            if (this.configuredDeepLinkScheme != schemeToUse) {
                this.configuredDeepLinkScheme = schemeToUse
                // Clear cached detection when manually configured
                cachedDeepLinkScheme = null
                changed = true
            }
            
            if (changed) {
                // Invalidate existing instance to force recreation with new URL
                ethereumInstance = null
                lastUsedUrl = null
                if (schemeToUse != null) {
                    Log.d("NitroMetamask", "configure: Dapp URL set to $urlToUse, deep link scheme set to $schemeToUse")
                } else {
                    Log.d("NitroMetamask", "configure: Dapp URL set to $urlToUse. Deep link scheme will be auto-detected from AndroidManifest.xml")
                }
            }
        }
    }
    
    /**
     * Get the deep link scheme - uses configured value first, then attempts auto-detection.
     * Directly reads intent filters from PackageManager to find the scheme with host="mmsdk"
     * Returns the scheme if found, null otherwise
     * 
     * The scheme is cached after first detection to avoid repeated queries.
     */
    private fun getDeepLinkScheme(context: android.content.Context): String? {
        // Use configured scheme if available
        configuredDeepLinkScheme?.let { return it }
        
        // Return cached detected scheme if available
        cachedDeepLinkScheme?.let { return it }
        
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            
            // Query for activities that can handle VIEW intents with BROWSABLE category
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                addCategory(android.content.Intent.CATEGORY_DEFAULT)
                addCategory(android.content.Intent.CATEGORY_BROWSABLE)
            }
            
            val resolveList = packageManager.queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // Look for activities in our package
            for (resolveInfo in resolveList) {
                if (resolveInfo.activityInfo?.packageName == packageName) {
                    val filter = resolveInfo.filter ?: continue
                    
                    // Check if this filter has the required actions and categories
                    if (!filter.hasAction(Intent.ACTION_VIEW)) continue
                    if (!filter.hasCategory(android.content.Intent.CATEGORY_DEFAULT)) continue
                    if (!filter.hasCategory(android.content.Intent.CATEGORY_BROWSABLE)) continue
                    
                    // Get all data schemes from this filter
                    val schemeCount = filter.countDataSchemes()
                    for (schemeIdx in 0 until schemeCount) {
                        val scheme = filter.getDataScheme(schemeIdx)
                        if (scheme != null) {
                            // Check if this scheme has mmsdk host in any authority
                            val authorityCount = filter.countDataAuthorities()
                            var hasMmsdkHost = false
                            for (authIdx in 0 until authorityCount) {
                                val authority = filter.getDataAuthority(authIdx)
                                if (authority != null && authority.host == "mmsdk") {
                                    hasMmsdkHost = true
                                    break
                                }
                            }
                            
                            if (hasMmsdkHost) {
                                // Verify this scheme with mmsdk host resolves to our package
                                val testUri = Uri.parse("$scheme://mmsdk")
                                val testIntent = Intent(Intent.ACTION_VIEW, testUri).apply {
                                    addCategory(android.content.Intent.CATEGORY_DEFAULT)
                                    addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                                }
                                
                                // Verify this intent resolves to our package
                                val testResolveList = packageManager.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY)
                                for (testResolveInfo in testResolveList) {
                                    if (testResolveInfo.activityInfo?.packageName == packageName) {
                                        // Cache the detected scheme
                                        cachedDeepLinkScheme = scheme
                                        Log.d("NitroMetamask", "Detected deep link scheme: $scheme from activity ${resolveInfo.activityInfo?.name}")
                                        return scheme
                                    }
                                }
                                Log.w("NitroMetamask", "Scheme $scheme with mmsdk host found but does not resolve to package $packageName")
                            }
                        }
                    }
                }
            }
            
            Log.w("NitroMetamask", "Could not detect deep link scheme from AndroidManifest.xml. Searched ${resolveList.size} activities in package $packageName")
            null
        } catch (e: Exception) {
            Log.w("NitroMetamask", "Error detecting deep link scheme: ${e.message}", e)
            null
        }
    }
    
    /**
     * Bring app back to foreground after MetaMask operations.
     * Uses the deep link scheme detected from AndroidManifest.xml to trigger the return.
     * This works by launching the same deep link that MetaMask app would use.
     * 
     * Note: Deep links work from background, but getLaunchIntentForPackage() is blocked.
     * So we only use deep link, never fallback to launch intent.
     */
    private fun bringAppToForeground() {
        try {
            val context = MetamaskContextHolder.get()
            // Must run on main thread - use Handler to ensure we're on main thread
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    val deepLinkScheme = getDeepLinkScheme(context)
                    if (deepLinkScheme != null) {
                        // Use the configured or detected deep link scheme to bring app to foreground
                        // This is the same deep link that MetaMask app would trigger
                        // Deep links work from background (unlike getLaunchIntentForPackage)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("$deepLinkScheme://mmsdk")
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or 
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                            setPackage(context.packageName)
                        }
                        context.startActivity(intent)
                        Log.d("NitroMetamask", "Brought app to foreground using deep link: $deepLinkScheme://mmsdk")
                    } else {
                        // Cannot use getLaunchIntentForPackage() - Android blocks it from background
                        // MetaMask should handle the return via deep link automatically
                        Log.w("NitroMetamask", "Could not determine deep link scheme. Please configure it via configure(dappUrl, deepLinkScheme) or ensure AndroidManifest.xml has the correct intent filter.")
                    }
                } catch (e: Exception) {
                    // Silently fail - better than crashing
                    // This is a defensive mechanism, not critical
                    Log.e("NitroMetamask", "Failed to bring app to foreground: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            // Silently fail - this is a defensive mechanism, not critical
            Log.e("NitroMetamask", "Error scheduling bringAppToForeground: ${e.message}", e)
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
                    
                    // Parse chainId from hex string (e.g., "0x1") or decimal string to Long
                    // chainId is an integer, so we use Long (bigint in TS maps to Long in Kotlin)
                    val chainId = try {
                        if (chainIdString.startsWith("0x") || chainIdString.startsWith("0X")) {
                            chainIdString.substring(2).toLong(16)
                        } else {
                            chainIdString.toLong()
                        }
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
                    
                    // Bring app back to foreground immediately after receiving signature
                    // This must be done on the main thread
                    bringAppToForeground()
                    
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

    override fun connectSign(nonce: String, exp: Long): Promise<ConnectSignResult> {
        // Use Promise.async with coroutines for best practice in Nitro modules
        // Reference: https://nitro.margelo.com/docs/types/promises
        // Based on MetaMask Android SDK: ethereum.connectSign(message)
        // Reference: https://github.com/MetaMask/metamask-android-sdk
        // The SDK's connectSign method handles connection and signing in one call
        return Promise.async {
            try {
                // Construct JSON message with only nonce and exp
                // We don't include address or chainID - just encrypt nonce and exp
                val message = org.json.JSONObject().apply {
                    put("nonce", nonce)
                    put("exp", exp)
                }.toString()
                
                Log.d("NitroMetamask", "connectSign: Constructed message with nonce and exp: $message")
                
                // Use the SDK's connectSign method - it will connect if needed and sign the message
                // This is the recommended approach per MetaMask Android SDK documentation
                // The SDK will handle bringing the app back to foreground via deep linking
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
                        
                        // After connectSign, the SDK state might not be immediately updated
                        // Try to explicitly fetch the account and chainId to ensure they're available
                        // This will trigger the SDK to update its state if needed
                        try {
                            val addressResult = suspendCancellableCoroutine<Result> { continuation ->
                                ethereum.getEthAccounts { callbackResult ->
                                    if (continuation.isActive) {
                                        continuation.resume(callbackResult)
                                    }
                                }
                            }
                            val chainIdResult = suspendCancellableCoroutine<Result> { continuation ->
                                ethereum.getChainId { callbackResult ->
                                    if (continuation.isActive) {
                                        continuation.resume(callbackResult)
                                    }
                                }
                            }
                            
                            // getEthAccounts returns an array of addresses, we need to extract the first one
                            // The SDK may return Result.Success.Item (JSON string) or Result.Success.Items (List)
                            Log.d("NitroMetamask", "connectSign: addressResult type: ${addressResult.javaClass.simpleName}")
                            val address = when (addressResult) {
                                is Result.Success.Item -> {
                                    val value = addressResult.value
                                    Log.d("NitroMetamask", "connectSign: addressResult.Item value type: ${value.javaClass.simpleName}, value: $value")
                                    // Convert to String - value is Any? so we need to ensure it's a String
                                    val valueStr = value as? String ?: value?.toString() ?: ""
                                    // Check if it's a JSON array string that needs parsing
                                    if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                                        try {
                                            val jsonArray = org.json.JSONArray(valueStr)
                                            val firstAddr = if (jsonArray.length() > 0) jsonArray.getString(0) else null
                                            if (firstAddr != null && firstAddr.isNotEmpty()) {
                                                ethereum.updateAccount(firstAddr)
                                                Log.d("NitroMetamask", "connectSign: Extracted address from JSON array: $firstAddr")
                                            }
                                            firstAddr
                                        } catch (e: Exception) {
                                            Log.w("NitroMetamask", "connectSign: Failed to parse address array: ${e.message}")
                                            // If it's not a JSON array, treat it as a single address
                                            if (valueStr.isNotEmpty()) {
                                                ethereum.updateAccount(valueStr)
                                            }
                                            valueStr
                                        }
                                    } else {
                                        // Single address string
                                        if (valueStr.isNotEmpty()) {
                                            ethereum.updateAccount(valueStr)
                                        }
                                        valueStr
                                    }
                                }
                                is Result.Success.Items -> {
                                    // Array of addresses - get the first one
                                    Log.d("NitroMetamask", "connectSign: addressResult.Items value size: ${addressResult.value.size}")
                                    val firstAddr = addressResult.value.firstOrNull()
                                    if (firstAddr != null && firstAddr.isNotEmpty()) {
                                        ethereum.updateAccount(firstAddr)
                                        Log.d("NitroMetamask", "connectSign: Extracted address from Items: $firstAddr")
                                    }
                                    firstAddr
                                }
                                else -> {
                                    Log.w("NitroMetamask", "connectSign: Unexpected addressResult type: ${addressResult.javaClass.simpleName}")
                                    null
                                }
                            }
                            val chainIdStr = when (chainIdResult) {
                                is Result.Success.Item -> {
                                    val chainIdValue = chainIdResult.value as? String
                                    // Update the SDK state with the chainId
                                    if (chainIdValue != null && chainIdValue.isNotEmpty()) {
                                        ethereum.updateChainId(chainIdValue)
                                    }
                                    chainIdValue
                                }
                                else -> null
                            }
                            
                            // Parse chainId from hex string (e.g., "0x1") to Long
                            val chainId = chainIdStr?.let { chainId ->
                                try {
                                    if (chainId.startsWith("0x") || chainId.startsWith("0X")) {
                                        chainId.substring(2).toLong(16)
                                    } else {
                                        chainId.toLong()
                                    }
                                } catch (e: NumberFormatException) {
                                    Log.w("NitroMetamask", "Invalid chainId format: $chainId", e)
                                    null
                                }
                            }
                            
                            // Bring app back to foreground immediately after receiving signature
                            // This must be done on the main thread
                            try {
                                bringAppToForeground()
                            } catch (e: Exception) {
                                Log.w("NitroMetamask", "connectSign: Failed to bring app to foreground: ${e.message}")
                            }
                            
                            // Validate that we have all required values
                            if (address == null || address.isEmpty()) {
                                throw IllegalStateException("Failed to retrieve address after connectSign. The signature was received but the address could not be determined.")
                            }
                            if (chainId == null) {
                                throw IllegalStateException("Failed to retrieve chainId after connectSign. The signature was received but the chainId could not be determined.")
                            }
                            
                            Log.d("NitroMetamask", "connectSign: Returning ConnectSignResult with signature, address=$address, chainId=$chainId")
                            
                            // Return ConnectSignResult with signature, address, and chainId
                            ConnectSignResult(
                                signature = signature,
                                address = address,
                                chainId = chainId
                            )
                        } catch (e: Exception) {
                            Log.e("NitroMetamask", "connectSign: Error fetching address/chainId: ${e.message}", e)
                            throw e
                        }
                    }
                    is Result.Success.ItemMap -> {
                        throw IllegalStateException("Unexpected ItemMap result from MetaMask connectSign")
                    }
                    is Result.Success.Items -> {
                        throw IllegalStateException("Unexpected Items result from MetaMask connectSign")
                    }
                    is Result.Error -> {
                        val errorMessage = result.error?.message ?: result.error?.toString() ?: "MetaMask connectSign failed"
                        Log.e("NitroMetamask", "connectSign: Error from MetaMask SDK: $errorMessage")
                        throw Exception(errorMessage)
                    }
                }
            } catch (e: Exception) {
                Log.e("NitroMetamask", "connectSign: Unexpected error", e)
                throw e
            }
        }
    }

    override fun getAddress(): Promise<Variant_NullType_String> {
        return Promise.async {
            // Read from ethereumState.value directly to get the most up-to-date value
            // The SDK uses LiveData, so the cached properties might not be updated immediately
            val state = ethereum.ethereumState.value
            val address = state?.selectedAddress?.takeIf { it.isNotEmpty() } ?: ethereum.selectedAddress
            Log.d("NitroMetamask", "getAddress: ethereumState.value?.selectedAddress = ${state?.selectedAddress}, ethereum.selectedAddress = ${ethereum.selectedAddress}, final = $address")
            if (address == null || address.isEmpty()) {
                Log.w("NitroMetamask", "getAddress: Address is null or empty")
                // Use NullType.NULL singleton as per Nitro documentation: https://nitro.margelo.com/docs/types/nulls
                Variant_NullType_String.First(NullType.NULL)
            } else {
                Log.d("NitroMetamask", "getAddress: Returning address: $address")
                Variant_NullType_String.create(address)
            }
        }
    }

    override fun getChainId(): Promise<Variant_NullType_Long> {
        return Promise.async {
            // Read from ethereumState.value directly to get the most up-to-date value
            // The SDK uses LiveData, so the cached properties might not be updated immediately
            val state = ethereum.ethereumState.value
            val chainIdString = state?.chainId?.takeIf { it.isNotEmpty() } ?: ethereum.chainId
            Log.d("NitroMetamask", "getChainId: ethereumState.value?.chainId = ${state?.chainId}, ethereum.chainId = ${ethereum.chainId}, final = $chainIdString")
            if (chainIdString == null || chainIdString.isEmpty()) {
                Log.w("NitroMetamask", "getChainId: ChainId is null or empty")
                // Use NullType.NULL singleton as per Nitro documentation: https://nitro.margelo.com/docs/types/nulls
                Variant_NullType_Long.First(NullType.NULL)
            } else {
                try {
                    val chainIdLong = if (chainIdString.startsWith("0x") || chainIdString.startsWith("0X")) {
                        chainIdString.substring(2).toLong(16)
                    } else {
                        chainIdString.toLong()
                    }
                    Log.d("NitroMetamask", "getChainId: Returning chainId: $chainIdLong")
                    Variant_NullType_Long.create(chainIdLong)
                } catch (e: NumberFormatException) {
                    Log.w("NitroMetamask", "Invalid chainId format: $chainIdString", e)
                    Variant_NullType_Long.First(NullType.NULL)
                }
            }
        }
    }
}
