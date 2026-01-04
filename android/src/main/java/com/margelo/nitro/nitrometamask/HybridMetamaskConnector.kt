package com.margelo.nitro.nitrometamask

import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.EthereumRequest
import io.metamask.androidsdk.EthereumMethod
import io.metamask.androidsdk.Result

class HybridMetamaskConnector : HybridMetamaskConnectorSpec() {
    private val ethereum by lazy { 
        Ethereum.getInstance()
    }

    override suspend fun connect(): ConnectResult {
        val result = ethereum.connect()
        return when (result) {
            is Result.Success.Item -> {
                // result.value contains the connection result
                // Based on SDK docs, this should contain address and chainId
                val connectionResult = result.value
                // Extract address and chainId from the result
                // The SDK returns account info in result.value
                val address = ethereum.selectedAddress
                    ?: throw IllegalStateException("MetaMask SDK returned no address")
                val chainId = ethereum.chainId
                    ?: throw IllegalStateException("MetaMask SDK returned no chainId")
                
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

    override suspend fun signMessage(message: String): String {
        val address = ethereum.selectedAddress
            ?: throw IllegalStateException("No connected account. Call connect() first.")
        
        // Based on SDK documentation, personal_sign params are: [address, message]
        // The SDK handles message encoding internally
        val request = EthereumRequest(
            method = EthereumMethod.PERSONAL_SIGN.value,
            params = listOf(address, message)
        )
        
        val result = ethereum.sendRequest(request)
        return when (result) {
            is Result.Success.Item -> {
                result.value as? String ?: throw IllegalStateException("Invalid signature response")
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

