package com.example.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores per-advisor-slot API keys encrypted at rest, backed by an
 * Android Keystore master key. These keys are NEVER written into the Room
 * database (SubAgentSlot.customApiKey is @Transient — see CouncilModels.kt)
 * and this preference file is excluded from Auto Backup / adb backup
 * (see backup_rules.xml / data_extraction_rules.xml), so a key never leaves
 * the device in plaintext.
 */
class SecureKeyStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKey(advisorId: Int, slotNumber: Int, apiKey: String) {
        val prefKey = keyFor(advisorId, slotNumber)
        if (apiKey.isBlank()) {
            prefs.edit().remove(prefKey).apply()
        } else {
            prefs.edit().putString(prefKey, apiKey).apply()
        }
    }

    fun getKey(advisorId: Int, slotNumber: Int): String {
        return prefs.getString(keyFor(advisorId, slotNumber), "") ?: ""
    }

    fun deleteKeysForAdvisor(advisorId: Int, slotCount: Int = MAX_SLOTS_PER_ADVISOR) {
        val editor = prefs.edit()
        for (slot in 1..slotCount) {
            editor.remove(keyFor(advisorId, slot))
        }
        editor.apply()
    }

    private fun keyFor(advisorId: Int, slotNumber: Int) = "advisor_${advisorId}_slot_${slotNumber}_api_key"

    companion object {
        private const val PREFS_FILE_NAME = "boardroom_secure_keys"
        private const val MAX_SLOTS_PER_ADVISOR = 5
    }
}
