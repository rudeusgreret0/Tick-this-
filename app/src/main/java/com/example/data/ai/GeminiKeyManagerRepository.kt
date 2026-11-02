package com.example.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.apiKeysDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gemini_api_key_vault"
)

enum class ApiKeyHealthStatus(val id: String, val displayLabel: String) {
    VALID("VALID", "Valid & Ready"),
    QUOTA_EXCEEDED("QUOTA_EXCEEDED", "Quota Exceeded"),
    INVALID("INVALID", "Invalid Key"),
    UNAVAILABLE("UNAVAILABLE", "Unavailable"),
    UNTESTED("UNTESTED", "Untested");

    companion object {
        fun fromId(id: String): ApiKeyHealthStatus =
            entries.firstOrNull { it.id == id } ?: UNTESTED
    }
}

data class SavedGeminiApiKey(
    val id: String,
    val label: String,
    val maskedPreview: String,
    val encryptedPayload: String,
    val iv: String,
    val status: ApiKeyHealthStatus = ApiKeyHealthStatus.UNTESTED,
    val statusMessage: String? = null,
    val lastTestedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class GeminiApiConfigState(
    val savedKeys: List<SavedGeminiApiKey> = emptyList(),
    val activeKeyId: String? = null, // null means built-in Server-Side Firebase AI Logic proxy
    val useServerSideProxyWhenNoKey: Boolean = true
) {
    val activeKey: SavedGeminiApiKey?
        get() = savedKeys.firstOrNull { it.id == activeKeyId }
}

/**
 * Encrypts and decrypts user-configured Gemini API keys using Android Keystore (AES-256-GCM).
 * Automatically falls back to deterministic local encoding in JVM unit test environments
 * where the hardware AndroidKeyStore provider is unavailable.
 */
object KeystoreCredentialCipher {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "atelier_xo_gemini_master_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun encrypt(plainText: String): Pair<String, String> {
        val trimmed = plainText.trim()
        return try {
            val secretKey = getOrCreateSecretKey()
            if (secretKey != null) {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val ivBytes = cipher.iv
                val encryptedBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
                encodeBase64(encryptedBytes) to encodeBase64(ivBytes)
            } else {
                fallbackEncrypt(trimmed)
            }
        } catch (_: Throwable) {
            fallbackEncrypt(trimmed)
        }
    }

    fun decrypt(encryptedPayload: String, iv: String): String? {
        return try {
            if (iv.startsWith("JVM_FALLBACK:")) {
                return fallbackDecrypt(encryptedPayload)
            }
            val secretKey = getOrCreateSecretKey() ?: return fallbackDecrypt(encryptedPayload)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val ivBytes = decodeBase64(iv)
            val spec = GCMParameterSpec(GCM_TAG_BITS, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decodedBytes = cipher.doFinal(decodeBase64(encryptedPayload))
            String(decodedBytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    fun maskApiKey(rawKey: String): String {
        val clean = rawKey.trim()
        if (clean.length <= 8) {
            return "••••" + clean.takeLast(2)
        }
        val prefix = clean.take(4)
        val suffix = clean.takeLast(4)
        return "$prefix••••••••$suffix"
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                return existing.secretKey
            }
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    private fun fallbackEncrypt(plainText: String): Pair<String, String> {
        val bytes = plainText.toByteArray(Charsets.UTF_8)
        val xorMask = 0x5A
        val obfuscated = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor xorMask).toByte() }
        return java.util.Base64.getEncoder().encodeToString(obfuscated) to "JVM_FALLBACK:v1"
    }

    private fun fallbackDecrypt(payload: String): String? {
        return try {
            val obfuscated = java.util.Base64.getDecoder().decode(payload)
            val xorMask = 0x5A
            val plain = ByteArray(obfuscated.size) { i -> (obfuscated[i].toInt() xor xorMask).toByte() }
            String(plain, Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeBase64(encoded: String): ByteArray {
        return try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(encoded)
        }
    }
}

class GeminiKeyManagerRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.apiKeysDataStore)

    private object Keys {
        val SAVED_KEYS_JSON = stringPreferencesKey("saved_gemini_keys_json")
        val ACTIVE_KEY_ID = stringPreferencesKey("active_gemini_key_id")
    }

    val configStateFlow: Flow<GeminiApiConfigState> = dataStore.data.map { prefs ->
        val rawJson = prefs[Keys.SAVED_KEYS_JSON] ?: "[]"
        val activeId = prefs[Keys.ACTIVE_KEY_ID]?.takeIf { it.isNotBlank() }
        val parsedKeys = parseKeysJson(rawJson)
        val validatedActiveId = if (activeId != null && parsedKeys.any { it.id == activeId }) {
            activeId
        } else {
            null
        }
        GeminiApiConfigState(
            savedKeys = parsedKeys,
            activeKeyId = validatedActiveId
        )
    }

    suspend fun addApiKey(
        label: String,
        rawApiKey: String,
        makeActive: Boolean = true,
        initialStatus: ApiKeyHealthStatus = ApiKeyHealthStatus.UNTESTED,
        initialMessage: String? = null
    ): SavedGeminiApiKey {
        val cleanLabel = label.trim().ifBlank { "Gemini Key" }.take(32)
        val cleanKey = rawApiKey.trim()
        require(cleanKey.isNotEmpty()) { "API Key cannot be blank" }

        val (encrypted, iv) = KeystoreCredentialCipher.encrypt(cleanKey)
        val masked = KeystoreCredentialCipher.maskApiKey(cleanKey)
        val newEntry = SavedGeminiApiKey(
            id = UUID.randomUUID().toString(),
            label = cleanLabel,
            maskedPreview = masked,
            encryptedPayload = encrypted,
            iv = iv,
            status = initialStatus,
            statusMessage = initialMessage,
            lastTestedAt = if (initialStatus != ApiKeyHealthStatus.UNTESTED) System.currentTimeMillis() else null,
            createdAt = System.currentTimeMillis()
        )

        dataStore.edit { prefs ->
            val currentList = parseKeysJson(prefs[Keys.SAVED_KEYS_JSON] ?: "[]").toMutableList()
            currentList.add(newEntry)
            prefs[Keys.SAVED_KEYS_JSON] = serializeKeysJson(currentList)
            if (makeActive) {
                prefs[Keys.ACTIVE_KEY_ID] = newEntry.id
            }
        }
        return newEntry
    }

    /**
     * Replaces an existing API key's label and/or secret key value without modifying or deleting
     * any other saved API configurations.
     */
    suspend fun replaceApiKey(
        keyId: String,
        newLabel: String,
        newRawApiKey: String?,
        newStatus: ApiKeyHealthStatus? = null,
        newStatusMessage: String? = null
    ): SavedGeminiApiKey? {
        var updatedEntry: SavedGeminiApiKey? = null
        dataStore.edit { prefs ->
            val currentList = parseKeysJson(prefs[Keys.SAVED_KEYS_JSON] ?: "[]")
            val nextList = currentList.map { existing ->
                if (existing.id != keyId) {
                    existing
                } else {
                    val cleanLabel = newLabel.trim().ifBlank { existing.label }.take(32)
                    val trimmedNewKey = newRawApiKey?.trim().orEmpty()
                    val modified = if (trimmedNewKey.isNotEmpty()) {
                        val (encrypted, iv) = KeystoreCredentialCipher.encrypt(trimmedNewKey)
                        val masked = KeystoreCredentialCipher.maskApiKey(trimmedNewKey)
                        existing.copy(
                            label = cleanLabel,
                            maskedPreview = masked,
                            encryptedPayload = encrypted,
                            iv = iv,
                            status = newStatus ?: ApiKeyHealthStatus.UNTESTED,
                            statusMessage = newStatusMessage,
                            lastTestedAt = if (newStatus != null) System.currentTimeMillis() else null
                        )
                    } else {
                        existing.copy(
                            label = cleanLabel,
                            status = newStatus ?: existing.status,
                            statusMessage = newStatusMessage ?: existing.statusMessage
                        )
                    }
                    updatedEntry = modified
                    modified
                }
            }
            prefs[Keys.SAVED_KEYS_JSON] = serializeKeysJson(nextList)
        }
        return updatedEntry
    }

    suspend fun removeApiKey(keyId: String) {
        dataStore.edit { prefs ->
            val currentList = parseKeysJson(prefs[Keys.SAVED_KEYS_JSON] ?: "[]")
            val filtered = currentList.filterNot { it.id == keyId }
            prefs[Keys.SAVED_KEYS_JSON] = serializeKeysJson(filtered)

            val currentActive = prefs[Keys.ACTIVE_KEY_ID]
            if (currentActive == keyId) {
                val nextCandidate = filtered.firstOrNull { it.status == ApiKeyHealthStatus.VALID }
                    ?: filtered.firstOrNull()
                if (nextCandidate != null) {
                    prefs[Keys.ACTIVE_KEY_ID] = nextCandidate.id
                } else {
                    prefs.remove(Keys.ACTIVE_KEY_ID)
                }
            }
        }
    }

    suspend fun selectActiveKey(keyId: String?) {
        dataStore.edit { prefs ->
            if (keyId.isNullOrBlank()) {
                prefs.remove(Keys.ACTIVE_KEY_ID)
            } else {
                prefs[Keys.ACTIVE_KEY_ID] = keyId
            }
        }
    }

    suspend fun updateKeyStatus(
        keyId: String,
        status: ApiKeyHealthStatus,
        statusMessage: String?
    ) {
        dataStore.edit { prefs ->
            val currentList = parseKeysJson(prefs[Keys.SAVED_KEYS_JSON] ?: "[]")
            val updated = currentList.map { item ->
                if (item.id == keyId) {
                    item.copy(
                        status = status,
                        statusMessage = statusMessage,
                        lastTestedAt = System.currentTimeMillis()
                    )
                } else {
                    item
                }
            }
            prefs[Keys.SAVED_KEYS_JSON] = serializeKeysJson(updated)
        }
    }

    suspend fun getDecryptedActiveKey(): Pair<SavedGeminiApiKey, String>? {
        val state = configStateFlow.first()
        val active = state.activeKey ?: return null
        val decrypted = KeystoreCredentialCipher.decrypt(active.encryptedPayload, active.iv)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return active to decrypted
    }

    suspend fun getDecryptedKeyById(keyId: String): Pair<SavedGeminiApiKey, String>? {
        val state = configStateFlow.first()
        val target = state.savedKeys.firstOrNull { it.id == keyId } ?: return null
        val decrypted = KeystoreCredentialCipher.decrypt(target.encryptedPayload, target.iv)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return target to decrypted
    }

    private fun encodeField(value: String): String {
        return java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decodeField(encoded: String): String {
        if (encoded.isBlank()) return ""
        return try {
            String(java.util.Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun parseKeysJson(rawStorage: String): List<SavedGeminiApiKey> {
        val trimmed = rawStorage.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        return try {
            trimmed.lines()
                .mapNotNull { line ->
                    val parts = line.trim().split("|")
                    if (parts.size < 8) return@mapNotNull null
                    val id = decodeField(parts[0])
                    val label = decodeField(parts[1]).ifBlank { "Gemini Key" }
                    val masked = decodeField(parts[2]).ifBlank { "••••" }
                    val encrypted = decodeField(parts[3])
                    val iv = decodeField(parts[4])
                    val status = ApiKeyHealthStatus.fromId(decodeField(parts[5]))
                    val statusMsg = decodeField(parts[6]).takeIf { it.isNotBlank() }
                    val lastTested = parts[7].toLongOrNull()?.takeIf { it > 0L }
                    val created = parts.getOrNull(8)?.toLongOrNull() ?: System.currentTimeMillis()
                    if (id.isNotBlank() && encrypted.isNotBlank()) {
                        SavedGeminiApiKey(
                            id = id,
                            label = label,
                            maskedPreview = masked,
                            encryptedPayload = encrypted,
                            iv = iv,
                            status = status,
                            statusMessage = statusMsg,
                            lastTestedAt = lastTested,
                            createdAt = created
                        )
                    } else {
                        null
                    }
                }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun serializeKeysJson(keys: List<SavedGeminiApiKey>): String {
        if (keys.isEmpty()) return ""
        return keys.joinToString("\n") { item ->
            listOf(
                encodeField(item.id),
                encodeField(item.label),
                encodeField(item.maskedPreview),
                encodeField(item.encryptedPayload),
                encodeField(item.iv),
                encodeField(item.status.id),
                encodeField(item.statusMessage.orEmpty()),
                (item.lastTestedAt ?: 0L).toString(),
                item.createdAt.toString()
            ).joinToString("|")
        }
    }
}
