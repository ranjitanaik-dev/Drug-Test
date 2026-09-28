package com.ncb.drugtestcompanion.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.ncb.drugtestcompanion.domain.model.TestRecord
import java.security.KeyStore
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore cryptographic manager for generating asymmetric signing keys,
 * building canonical evidence strings, and digitally signing/verifying test records.
 */
@Singleton
class KeystoreSigner @Inject constructor() {

    private val keyAlias = "dt_evidence_signing_key"
    private val keyStoreType = "AndroidKeyStore"
    val algorithmName = "SHA256withRSA"

    private fun getOrCreatePrivateKey(): PrivateKey {
        val keyStore = KeyStore.getInstance(keyStoreType).apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                keyStoreType
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()

            keyPairGenerator.initialize(parameterSpec)
            keyPairGenerator.generateKeyPair()
        }
        return (keyStore.getEntry(keyAlias, null) as KeyStore.PrivateKeyEntry).privateKey
    }

    fun getPublicKey(): PublicKey? {
        return try {
            val keyStore = KeyStore.getInstance(keyStoreType).apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.getCertificate(keyAlias)?.publicKey
            } else {
                getOrCreatePrivateKey()
                val reloadedKeyStore = KeyStore.getInstance(keyStoreType).apply { load(null) }
                reloadedKeyStore.getCertificate(keyAlias)?.publicKey
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun generateCanonicalString(record: TestRecord): String {
        return StringBuilder().apply {
            append("testId=").append(record.testId).append("\n")
            append("timestamp=").append(record.timestamp).append("\n")
            append("kitId=").append(record.kitId).append("\n")
            append("referenceCardProfileId=").append(record.referenceCardProfileId).append("\n")
            append("result=").append(record.result).append("\n")
            append("confidence=").append(record.confidence).append("\n")
            append("distance=").append(record.distance).append("\n")
            append("imageSha256=").append(record.imageSha256).append("\n")
            append("imagePath=").append(record.imagePath).append("\n")
            append("operatorId=").append(record.operatorId).append("\n")
            append("latitude=").append(record.latitude ?: "null").append("\n")
            append("longitude=").append(record.longitude ?: "null").append("\n")
            append("address=").append(record.address ?: "null").append("\n")
            append("locationStatus=").append(record.locationStatus)
        }.toString()
    }

    fun sign(record: TestRecord): String {
        return try {
            val privateKey = getOrCreatePrivateKey()
            val canonicalData = generateCanonicalString(record).toByteArray(Charsets.UTF_8)

            val signature = Signature.getInstance(algorithmName).apply {
                initSign(privateKey)
                update(canonicalData)
            }
            val signatureBytes = signature.sign()
            encodeBase64(signatureBytes)
        } catch (_: Throwable) {
            "SIGNATURE_GENERATION_FAILED"
        }
    }

    fun verifySignature(record: TestRecord, base64Signature: String, publicKeyOverride: PublicKey? = null): Boolean {
        if (base64Signature.isBlank() || base64Signature == "SIGNATURE_GENERATION_FAILED") return false
        return try {
            val publicKey = publicKeyOverride ?: getPublicKey() ?: return false
            val canonicalData = generateCanonicalString(record).toByteArray(Charsets.UTF_8)
            val signatureBytes = decodeBase64(base64Signature)

            val signature = Signature.getInstance(algorithmName).apply {
                initVerify(publicKey)
                update(canonicalData)
            }
            signature.verify(signatureBytes)
        } catch (_: Throwable) {
            false
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    private fun decodeBase64(base64Str: String): ByteArray {
        return try {
            Base64.getDecoder().decode(base64Str.trim())
        } catch (_: Throwable) {
            android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
        }
    }
}
