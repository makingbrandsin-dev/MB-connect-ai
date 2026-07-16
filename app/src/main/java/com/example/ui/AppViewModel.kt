package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.CallAnsweringService
import com.example.service.CallState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// --- APP NAVIGATION SCREENS ---
enum class AppScreen {
    SPLASH,
    ONBOARDING,
    SIGN_UP,
    SIGN_IN,
    SIGN_IN_OTP,
    FORGOT_PASSWORD,
    DASHBOARD
}

// --- USER PROFILE STATE ---
data class UserProfile(
    val name: String,
    val phone: String,
    val email: String,
    val profilePictureColorHex: String = "#D4B483"
)

// --- CONTACT ITEM MODEL ---
data class ContactItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val isFavorite: Boolean = false,
    val note: String = ""
)

// --- MISSED CALL ALERT MODEL ---
data class MissedCallAlert(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DataRepository(database)
    val callAnsweringService = CallAnsweringService(application, repository)

    // --- NAVIGATION STATE ---
    val currentScreen = MutableStateFlow(AppScreen.SPLASH)

    // --- ACCESSIBILITY OPTIONS ---
    val isHighContrastMode = MutableStateFlow(false)
    val isAiPaused = MutableStateFlow(false)

    // --- USER PROFILE ---
    val currentUserProfile = MutableStateFlow(UserProfile("Ravi Teja", "+91 98765 43210", "ravi.teja@mbconnect.ai"))

    // --- OTP AUTHENTICATION SIMULATOR ---
    val otpSentNumber = MutableStateFlow("")
    val simulatedOtpCode = MutableStateFlow("")
    val isOtpVerified = MutableStateFlow(false)
    val otpError = MutableStateFlow("")

    // --- CONTACTS AND FAVORITES STATE ---
    val contactsList = MutableStateFlow<List<ContactItem>>(emptyList())
    val contactsSearchQuery = MutableStateFlow("")
    val contactsSortOrder = MutableStateFlow("NAME_ASC") // "NAME_ASC", "NAME_DESC"

    // --- DIALER STATE ---
    val dialerInput = MutableStateFlow("")

    // --- OUTGOING CALL STATE ---
    val outgoingCallActive = MutableStateFlow(false)
    val outgoingCallName = MutableStateFlow("")
    val outgoingCallNumber = MutableStateFlow("")
    val isCallMuted = MutableStateFlow(false)
    val isSpeakerOn = MutableStateFlow(false)

    // --- CALL RECORDING STATE ---
    val isCallRecording = MutableStateFlow(false)
    val recordingDuration = MutableStateFlow(0) // seconds recorded

    // --- MISSED CALL NOTIFICATIONS & ALERTS ---
    val missedCallsAlerts = MutableStateFlow<List<MissedCallAlert>>(emptyList())

    // --- REACTIVE STATE FLOWS ---
    val callLogs: StateFlow<List<DecryptedCallLog>> = repository.allCallLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val faqs: StateFlow<List<DecryptedFaq>> = repository.allFaqs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val priorityContacts: StateFlow<List<DecryptedPriorityContact>> = repository.allPriorityContacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentCallState: StateFlow<CallState> = callAnsweringService.currentCallState

    // --- FILTERED AND SORTED CONTACTS FLOW ---
    val sortedFilteredContacts: StateFlow<List<ContactItem>> = combine(
        contactsList,
        contactsSearchQuery,
        contactsSortOrder
    ) { list, query, sortOrder ->
        val filtered = if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phoneNumber.contains(query, ignoreCase = true)
            }
        }
        
        if (sortOrder == "NAME_ASC") {
            filtered.sortedBy { it.name.lowercase() }
        } else {
            filtered.sortedByDescending { it.name.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Seed default business FAQs and Priority Contact if database is empty to enhance initial launch experience
        viewModelScope.launch {
            repository.allFaqs.collect { existing ->
                if (existing.isEmpty()) {
                    seedDefaultFaqs()
                }
            }
        }
        seedDefaultContacts()
    }

    private suspend fun seedDefaultFaqs() {
        val defaultFaqs = listOf(
            DecryptedFaq(
                question = "మీ వ్యాపారం ఏమిటి?",
                answer = "ఎంబీ కనెక్ట్ అనేది ఏఐ ఆధారిత స్మార్ట్ ఫోన్ సమాధానాల సర్వీస్."
            ),
            DecryptedFaq(
                question = "మీ సర్వీస్ ధర ఎంత?",
                answer = "మా బేసిక్ ప్లాన్ నెలకు 999 రూపాయల నుండి ప్రారంభమవుతుంది."
            ),
            DecryptedFaq(
                question = "మిమ్మల్ని ఎలా సంప్రదించాలి?",
                answer = "మీరు మా వెబ్‌సైట్ ద్వారా లేదా సపోర్ట్ నంబర్‌కు ఇమెయిల్ చేయవచ్చు."
            )
        )
        for (faq in defaultFaqs) {
            repository.insertFaq(faq)
        }
    }

    private fun seedDefaultContacts() {
        contactsList.value = listOf(
            ContactItem(name = "Kalyan Ram", phoneNumber = "+91 94405 12345", isFavorite = true, note = "Business Partner"),
            ContactItem(name = "Sravani", phoneNumber = "+91 81234 56789", isFavorite = true, note = "Lead Designer"),
            ContactItem(name = "Madhav Krishna", phoneNumber = "+91 99887 76655", isFavorite = false, note = "Tech Lead"),
            ContactItem(name = "Anjali Devi", phoneNumber = "+91 77665 54433", isFavorite = false, note = "Marketing"),
            ContactItem(name = "Ravi Teja (MB Connect)", phoneNumber = "+91 98765 43210", isFavorite = true, note = "Co-founder")
        )
    }

    // --- ACTIONS ---

    fun toggleHighContrastMode() {
        isHighContrastMode.value = !isHighContrastMode.value
    }

    fun toggleAiPause() {
        isAiPaused.value = !isAiPaused.value
    }

    fun addFaq(question: String, answer: String) {
        if (question.isBlank() || answer.isBlank()) return
        viewModelScope.launch {
            repository.insertFaq(DecryptedFaq(question = question, answer = answer))
        }
    }

    fun deleteFaq(id: Int) {
        viewModelScope.launch {
            repository.deleteFaq(id)
        }
    }

    fun addPriorityContact(phoneNumber: String, name: String, routeAction: String) {
        if (phoneNumber.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            repository.insertPriorityContact(
                DecryptedPriorityContact(
                    phoneNumber = phoneNumber,
                    name = name,
                    routeAction = routeAction
                )
            )
        }
    }

    fun deletePriorityContact(id: Int) {
        viewModelScope.launch {
            repository.deletePriorityContact(id)
        }
    }

    fun deleteCallLog(id: Int) {
        viewModelScope.launch {
            repository.deleteCallLog(id)
        }
    }

    fun clearAllCallLogs() {
        viewModelScope.launch {
            repository.clearAllCallLogs()
        }
    }

    fun simulateCall(phoneNumber: String, name: String, callType: String) {
        callAnsweringService.onIncomingCall(phoneNumber, name, callType)
    }

    fun stopCallSimulation() {
        // If recording was active, save call log with simulated recorded audio indicator
        val activeCall = callAnsweringService.currentCallState.value
        if (isCallRecording.value && activeCall != CallState.Idle) {
            viewModelScope.launch {
                val recordLog = DecryptedCallLog(
                    phoneNumber = when (activeCall) {
                        is CallState.Incoming -> activeCall.phoneNumber
                        is CallState.Answering -> activeCall.phoneNumber
                        is CallState.RecordingCaller -> activeCall.phoneNumber
                        else -> "+91 98765 43210"
                    },
                    contactName = when (activeCall) {
                        is CallState.Incoming -> activeCall.contactName
                        is CallState.Answering -> activeCall.contactName
                        else -> "Anonymous"
                    },
                    callType = "SIM",
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 45,
                    transcription = "Simulated Recorded VoIP / SIM Answering in Telugu.",
                    summary = "Voice Call recorded locally with AES GCM encryption keys.",
                    audioFilePath = "/recordings/sim_recorded_${System.currentTimeMillis()}.mp3",
                    status = "Answered by AI",
                    suggestedReply = "Recorded Successfully."
                )
                repository.insertCallLog(recordLog)
            }
        }
        callAnsweringService.resetCallState()
        isCallRecording.value = false
    }

    // --- AUTHENTICATION FLOW METHODS ---

    fun triggerSignUp(fullName: String, phone: String, email: String) {
        currentUserProfile.value = UserProfile(fullName, phone, email)
        currentScreen.value = AppScreen.SIGN_IN_OTP
        triggerWhatsAppOtp(phone)
    }

    fun triggerWhatsAppOtp(phone: String) {
        otpSentNumber.value = phone
        // Generate a random 4-digit code
        val randomCode = (1000..9999).random().toString()
        simulatedOtpCode.value = randomCode
        otpError.value = ""
        isOtpVerified.value = false
    }

    fun verifyOtp(codeEntered: String): Boolean {
        return if (codeEntered == simulatedOtpCode.value) {
            isOtpVerified.value = true
            otpError.value = ""
            currentScreen.value = AppScreen.DASHBOARD
            true
        } else {
            otpError.value = "Invalid OTP code entered. Please try again."
            false
        }
    }

    // --- DIALER & CALLS METHODS ---

    fun onDialKey(char: String) {
        dialerInput.value += char
    }

    fun onDialBackspace() {
        if (dialerInput.value.isNotEmpty()) {
            dialerInput.value = dialerInput.value.dropLast(1)
        }
    }

    fun startOutgoingCall(number: String, name: String = "") {
        if (number.isBlank()) return
        outgoingCallNumber.value = number
        outgoingCallName.value = name.ifEmpty { "Unknown Outgoing" }
        outgoingCallActive.value = true
        isCallMuted.value = false
        isSpeakerOn.value = false
        isCallRecording.value = false
    }

    fun endOutgoingCall() {
        if (outgoingCallActive.value) {
            // Save log of outgoing call
            viewModelScope.launch {
                val recordLog = DecryptedCallLog(
                    phoneNumber = outgoingCallNumber.value,
                    contactName = outgoingCallName.value,
                    callType = "Outgoing",
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 25,
                    transcription = "Outgoing dialed call successfully verified.",
                    summary = if (isCallRecording.value) "Outgoing Call - Call Recording active and saved locally." else "Outgoing Call completed securely.",
                    audioFilePath = if (isCallRecording.value) "/recordings/outgoing_${System.currentTimeMillis()}.mp3" else "",
                    status = "Bypassed",
                    suggestedReply = "N/A"
                )
                repository.insertCallLog(recordLog)
            }
        }
        outgoingCallActive.value = false
        isCallRecording.value = false
    }

    // --- CONTACTS MANAGEMENT ---

    fun addContact(name: String, phoneNumber: String, note: String = "") {
        if (name.isBlank() || phoneNumber.isBlank()) return
        val newContact = ContactItem(name = name, phoneNumber = phoneNumber, note = note)
        contactsList.value = contactsList.value + newContact
    }

    fun toggleContactFavorite(id: String) {
        contactsList.value = contactsList.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun deleteContact(id: String) {
        contactsList.value = contactsList.value.filter { it.id != id }
    }

    // --- MISSED CALLS ALERTS ---

    fun addSimulatedMissedCall(name: String, phoneNumber: String) {
        val alert = MissedCallAlert(name = name, phoneNumber = phoneNumber)
        missedCallsAlerts.value = missedCallsAlerts.value + alert
    }

    fun clearMissedCallAlert(id: String) {
        missedCallsAlerts.value = missedCallsAlerts.value.filter { it.id != id }
    }

    fun clearAllMissedCalls() {
        missedCallsAlerts.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        callAnsweringService.destroy()
    }
}

