package com.example.crypto

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedEnvelope(
    val ciphertext: String,
    val iv: String,
    val keyMode: String
)

/**
 * End-to-End Encryption engine for Duet.
 * - Generates a per-user ECDH P-256 (secp256r1) key pair stored exclusively on-device.
 * - Derives pairwise 256-bit AES-GCM keys via ECDH + SHA-256 KDF when both partners are linked.
 * - Supports an invite-code-wrapped 256-bit vault key for messages/moments authored prior to partner join.
 */
class DuetCryptoManager(
    context: Context,
    private val userId: String
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "duet_e2ee_keystore_${userId.take(48)}",
        Context.MODE_PRIVATE
    )
    private val secureRandom = SecureRandom()

    fun getOrCreateKeyPair(): KeyPair {
        val savedPub = prefs.getString(KEY_PUBLIC, null)
        val savedPriv = prefs.getString(KEY_PRIVATE, null)
        if (!savedPub.isNullOrBlank() && !savedPriv.isNullOrBlank()) {
            try {
                val kf = KeyFactory.getInstance("EC")
                val pub = kf.generatePublic(X509EncodedKeySpec(Base64.decode(savedPub, Base64.NO_WRAP)))
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(savedPriv, Base64.NO_WRAP)))
                return KeyPair(pub, priv)
            } catch (e: Exception) {
                Log.w(TAG, "Stored keypair could not be loaded; regenerating", e)
            }
        }
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        val pair = kpg.generateKeyPair()
        prefs.edit()
            .putString(KEY_PUBLIC, Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP))
            .putString(KEY_PRIVATE, Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP))
            .apply()
        return pair
    }

    fun getMyPublicKeyBase64(): String {
        val pair = getOrCreateKeyPair()
        return Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP)
    }

    /**
     * Generates and persists a fresh ECDH P-256 keypair when a couple space is dissolved
     * so future pairings start with brand-new keys and a new Couple Safety Number.
     */
    fun rotateKeyPair(): String {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        val pair = kpg.generateKeyPair()
        val encodedPub = Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP)
        val encodedPriv = Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_PUBLIC, encodedPub)
            .putString(KEY_PRIVATE, encodedPriv)
            .apply()
        return encodedPub
    }

    fun createWrappedVaultKey(inviteCode: String, coupleId: String): String {
        val rawVaultKey = ByteArray(32)
        secureRandom.nextBytes(rawVaultKey)
        val wrapKey = deriveInviteWrappingKey(inviteCode, coupleId)
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, wrapKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(rawVaultKey)
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun unwrapVaultKey(wrappedVaultKey: String, inviteCode: String, coupleId: String): SecretKey? {
        return try {
            val combined = Base64.decode(wrappedVaultKey, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_BYTES) return null
            val iv = combined.copyOfRange(0, GCM_IV_BYTES)
            val encrypted = combined.copyOfRange(GCM_IV_BYTES, combined.size)
            val wrapKey = deriveInviteWrappingKey(inviteCode, coupleId)
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, wrapKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val rawVaultKey = cipher.doFinal(encrypted)
            SecretKeySpec(rawVaultKey, "AES")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unwrap couple vault key", e)
            null
        }
    }

    fun deriveEcdhSessionKey(partnerPublicKeyBase64: String, coupleId: String): SecretKey? {
        return try {
            val myPrivate: PrivateKey = getOrCreateKeyPair().private
            val partnerPubBytes = Base64.decode(partnerPublicKeyBase64, Base64.NO_WRAP)
            val partnerPublic: PublicKey = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(partnerPubBytes))

            val agreement = KeyAgreement.getInstance("ECDH")
            agreement.init(myPrivate)
            agreement.doPhase(partnerPublic, true)
            val sharedSecret = agreement.generateSecret()

            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(sharedSecret)
            digest.update(coupleId.toByteArray(Charsets.UTF_8))
            digest.update("duet-ecdh-aes256-v1".toByteArray(Charsets.UTF_8))
            val derivedKey = digest.digest()
            SecretKeySpec(derivedKey, "AES")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to derive ECDH shared key", e)
            null
        }
    }

    fun encrypt(
        plaintext: String,
        coupleId: String,
        inviteCode: String,
        wrappedVaultKey: String,
        partnerPublicKey: String?
    ): EncryptedEnvelope {
        val ecdhKey = if (!partnerPublicKey.isNullOrBlank()) {
            deriveEcdhSessionKey(partnerPublicKey, coupleId)
        } else {
            null
        }
        val activeKey: SecretKey
        val mode: String
        if (ecdhKey != null) {
            activeKey = ecdhKey
            mode = MODE_ECDH
        } else {
            activeKey = unwrapVaultKey(wrappedVaultKey, inviteCode, coupleId)
                ?: deriveInviteWrappingKey(inviteCode, coupleId)
            mode = MODE_VAULT
        }
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, activeKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedEnvelope(
            ciphertext = Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            keyMode = mode
        )
    }

    fun decrypt(
        ciphertext: String,
        ivBase64: String,
        keyMode: String,
        coupleId: String,
        inviteCode: String,
        wrappedVaultKey: String,
        partnerPublicKey: String?
    ): String? {
        val key = when (keyMode) {
            MODE_ECDH -> {
                if (!partnerPublicKey.isNullOrBlank()) {
                    deriveEcdhSessionKey(partnerPublicKey, coupleId)
                } else {
                    null
                }
            }
            else -> unwrapVaultKey(wrappedVaultKey, inviteCode, coupleId)
        } ?: return null

        return try {
            val cipherBytes = Base64.decode(ciphertext, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Decryption failed for mode=$keyMode", e)
            null
        }
    }

    fun computeSafetyNumber(keyA: String, keyB: String?): String {
        val sorted = listOfNotNull(keyA.takeIf { it.isNotBlank() }, keyB?.takeIf { it.isNotBlank() }).sorted()
        val digest = MessageDigest.getInstance("SHA-256")
        sorted.forEach { digest.update(it.toByteArray(Charsets.UTF_8)) }
        val hash = digest.digest()
        val groups = (0 until 6).map { idx ->
            val offset = idx * 4
            val value = ((hash[offset].toInt() and 0xFF) shl 8) or (hash[offset + 1].toInt() and 0xFF)
            (value % 10000).toString().padStart(4, '0')
        }
        return groups.joinToString(" ")
    }

    fun shortKeyFingerprint(publicKeyBase64: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(publicKeyBase64.toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString(":") { "%02X".format(it) }
    }

    private fun deriveInviteWrappingKey(inviteCode: String, coupleId: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("duet-pair-vault-v1".toByteArray(Charsets.UTF_8))
        digest.update(inviteCode.toByteArray(Charsets.UTF_8))
        digest.update(coupleId.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest.digest(), "AES")
    }

    companion object {
        private const val TAG = "DuetCrypto"
        private const val KEY_PUBLIC = "ecdh_pub_x509"
        private const val KEY_PRIVATE = "ecdh_priv_pkcs8"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        const val MODE_ECDH = "ECDH_P256"
        const val MODE_VAULT = "PAIR_VAULT"

        fun normalizeSafetyNumberDigits(raw: String): String =
            raw.filter { it.isDigit() }
    }
}
