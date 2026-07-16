package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.security.CryptoManager

// --- DATABASE ENTITIES (STORED ENCRYPTED) ---

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val encryptedPhoneNumber: String,
    val encryptedContactName: String,
    val callType: String, // "SIM" or "WhatsApp"
    val timestamp: Long,
    val durationSeconds: Int,
    val encryptedTranscription: String,
    val encryptedSummary: String,
    val audioFilePath: String, // Local audio recording path
    val status: String, // "Answered by AI", "Bypassed", "Missed", "Auto-Replied"
    val encryptedSuggestedReply: String
)

@Entity(tableName = "faqs")
data class FaqEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val encryptedQuestion: String,
    val encryptedAnswer: String,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "priority_contacts")
data class PriorityContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val encryptedPhoneNumber: String,
    val encryptedName: String,
    val isEnabled: Boolean = true,
    val routeAction: String // "AUTO_ANSWER", "BYPASS_BOT", "REJECT", "AUTO_REPLY"
)


// --- DECRYPTED MODERN DATA MODELS FOR UI AND SERVICES ---

data class DecryptedCallLog(
    val id: Int = 0,
    val phoneNumber: String,
    val contactName: String,
    val callType: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val transcription: String,
    val summary: String,
    val audioFilePath: String,
    val status: String,
    val suggestedReply: String
) {
    fun toEntity(): CallLogEntity = CallLogEntity(
        id = id,
        encryptedPhoneNumber = CryptoManager.encrypt(phoneNumber),
        encryptedContactName = CryptoManager.encrypt(contactName),
        callType = callType,
        timestamp = timestamp,
        durationSeconds = durationSeconds,
        encryptedTranscription = CryptoManager.encrypt(transcription),
        encryptedSummary = CryptoManager.encrypt(summary),
        audioFilePath = CryptoManager.encrypt(audioFilePath),
        status = status,
        encryptedSuggestedReply = CryptoManager.encrypt(suggestedReply)
    )
}

data class DecryptedFaq(
    val id: Int = 0,
    val question: String,
    val answer: String,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toEntity(): FaqEntity = FaqEntity(
        id = id,
        encryptedQuestion = CryptoManager.encrypt(question),
        encryptedAnswer = CryptoManager.encrypt(answer),
        isEnabled = isEnabled,
        timestamp = timestamp
    )
}

data class DecryptedPriorityContact(
    val id: Int = 0,
    val phoneNumber: String,
    val name: String,
    val isEnabled: Boolean = true,
    val routeAction: String
) {
    fun toEntity(): PriorityContactEntity = PriorityContactEntity(
        id = id,
        encryptedPhoneNumber = CryptoManager.encrypt(phoneNumber),
        encryptedName = CryptoManager.encrypt(name),
        isEnabled = isEnabled,
        routeAction = routeAction
    )
}


// --- MAPPER EXTENSIONS ---

fun CallLogEntity.toDecrypted(): DecryptedCallLog = DecryptedCallLog(
    id = id,
    phoneNumber = CryptoManager.decrypt(encryptedPhoneNumber),
    contactName = CryptoManager.decrypt(encryptedContactName),
    callType = callType,
    timestamp = timestamp,
    durationSeconds = durationSeconds,
    transcription = CryptoManager.decrypt(encryptedTranscription),
    summary = CryptoManager.decrypt(encryptedSummary),
    audioFilePath = CryptoManager.decrypt(audioFilePath),
    status = status,
    suggestedReply = CryptoManager.decrypt(encryptedSuggestedReply)
)

fun FaqEntity.toDecrypted(): DecryptedFaq = DecryptedFaq(
    id = id,
    question = CryptoManager.decrypt(encryptedQuestion),
    answer = CryptoManager.decrypt(encryptedAnswer),
    isEnabled = isEnabled,
    timestamp = timestamp
)

fun PriorityContactEntity.toDecrypted(): DecryptedPriorityContact = DecryptedPriorityContact(
    id = id,
    phoneNumber = CryptoManager.decrypt(encryptedPhoneNumber),
    name = CryptoManager.decrypt(encryptedName),
    isEnabled = isEnabled,
    routeAction = routeAction
)
