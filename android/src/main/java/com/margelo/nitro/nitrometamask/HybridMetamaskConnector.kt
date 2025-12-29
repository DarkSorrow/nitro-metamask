package com.margelo.nitro.nitrometamask

import io.metamask.androidsdk.ConnectOptions
import io.metamask.androidsdk.EthereumClient
import io.metamask.androidsdk.Request
import io.metamask.androidsdk.RequestResult

class HybridMetamaskConnector : HybridMetamaskConnectorSpec() {
    private val client by lazy { EthereumClient.getInstance() }

    override suspend fun connect(): ConnectResult {
        val response = client.connect(ConnectOptions())
        val address = response.accounts.firstOrNull()
            ?: throw IllegalStateException("MetaMask SDK returned no accounts")

        return ConnectResult(
            address = address,
            chainId = response.chainId.toString()
        )
    }

    override suspend fun signMessage(message: String): String {
        val address = client.getSelectedAddress() 
            ?: throw IllegalStateException("No connected account. Call connect() first.")
        
        // Convert message to hex-encoded format for personal_sign
        // personal_sign expects: personal_sign(messageHex, address)
        // where messageHex is "0x" + hex-encoded UTF-8 bytes
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val messageHex = "0x" + messageBytes.joinToString("") { "%02x".format(it) }
        
        val request = Request(
            method = "personal_sign",
            params = listOf(messageHex, address)
        )
        
        val result = client.sendRequest(request)
        return when (result) {
            is RequestResult.Success -> {
                result.data as? String ?: throw IllegalStateException("Invalid signature response")
            }
            is RequestResult.Error -> {
                throw Exception(result.error.message ?: "Failed to sign message")
            }
        }
    }
}

