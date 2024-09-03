package com.silencio.peaqexample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.silencio.peaq.Peaq
import com.silencio.peaq.model.DIDData
import com.silencio.peaq.model.DIDDocumentCustomData
import com.silencio.peaq.utils.EncryptionType
import io.peaq.did.Document
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.lang.Integer.parseInt
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val issuerSeed = "ADD_ISSUER_SEED_HERE"
        val peaqInstance = Peaq(
            baseURL = "ADD_SOCKET_BASE_URL_HERE",
            seed =  issuerSeed
        )

        lifecycleScope.launch {
            val (issuerPublicKey, issuerPrivateKey, issuerAddress) = peaqInstance.getPublicPrivateKeyAddressFromMachineSeed(
                mnemonicWord = issuerSeed
            )

            val ownerSeed = peaqInstance.generateMnemonicSeed()
            val (ownerPublicKey, ownerPrivateKey, ownerAddress) = peaqInstance.getPublicPrivateKeyAddressFromMachineSeed(
                mnemonicWord = ownerSeed
            )

            val machineSeed = peaqInstance.generateMnemonicSeed()
            val (machinePublicKey, machinePrivateKey, machineAddress) = peaqInstance.getPublicPrivateKeyAddressFromMachineSeed(
                mnemonicWord = machineSeed
            )

            val document = peaqInstance.createDidDocument(
                ownerAddress = ownerAddress,
                machineAddress = machineAddress,
                machinePublicKey = machinePublicKey
            )
            Log.e("Document", "Document : ${document}")
            val map = peaqInstance.createDid(
                secretPhrase = machineSeed,
                name = "did:peaq:$machineAddress",
                value = document.toByteArray().toHexString()
            )
            map.collectLatest {
                if (it.inBlock != null) {
                    Log.e("Hash Key", "Hash Key ${it.inBlock}")
                }
                if (it.error != null) {
                    Log.e("Error", "Error ${it.error}")
                }


                val payloadData = DIDDocumentCustomData(
                    id = "machineAddress",
                    type = "Custom_data",
                    data = "a@gmail.com"
                )
                val payload = Gson().toJson(payloadData)
                val payloadHex =
                    peaqInstance.signData(payload, issuerSeed, format = EncryptionType.ED25519)

                Log.e("PayLoadHex", "PayLoadHex ${payloadHex}")

                val store = peaqInstance.storeMachineDataHash(
                    payloadData = payloadHex,
                    itemType = "peaq_123",
                    machineSeed = machineSeed
                )
                if (store?.error != null) {
                    Log.e(
                        "Store Error",
                        "Store Error  ${store.error?.code}  ${store.error?.message}"
                    )
                }
                if (store?.result != null) {
                    Log.e("Store Result", "Store Result ${store.result.toString()}")
                }
                peaqInstance.disconnect()
            }


        }
    }
}
