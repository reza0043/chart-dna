package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BoardroomApp
import com.example.data.model.AdvisorEntity
import com.example.data.model.ChatMessage
import com.example.data.model.CouncilDataConverters
import com.example.data.model.DispatchMode
import com.example.data.model.MasterFile
import com.example.data.model.MeetingSession
import com.example.data.model.SubAgentSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class BoardroomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BoardroomApp).repository
    private val memoryManager = (application as BoardroomApp).memoryManager
    private val secureKeyStore = (application as BoardroomApp).secureKeyStore

    val advisors: StateFlow<List<AdvisorEntity>> = repository.allAdvisors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<MeetingSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val masterFiles: StateFlow<List<MasterFile>> = repository.masterFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSession = MutableStateFlow<MeetingSession?>(null)
    val currentSession: StateFlow<MeetingSession?> = _currentSession.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _dispatchMode = MutableStateFlow(DispatchMode.AUTO_TRIAGE)
    val dispatchMode: StateFlow<DispatchMode> = _dispatchMode.asStateFlow()

    private val _selectedAdvisorIds = MutableStateFlow<Set<Int>>(setOf(1, 2, 3))
    val selectedAdvisorIds: StateFlow<Set<Int>> = _selectedAdvisorIds.asStateFlow()

    private val _pipelineSequence = MutableStateFlow<List<Int>>(listOf(1, 2, 3))
    val pipelineSequence: StateFlow<List<Int>> = _pipelineSequence.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _selectedAdvisorForEdit = MutableStateFlow<AdvisorEntity?>(null)
    val selectedAdvisorForEdit: StateFlow<AdvisorEntity?> = _selectedAdvisorForEdit.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isNewSessionDialogOpen = MutableStateFlow(false)
    val isNewSessionDialogOpen: StateFlow<Boolean> = _isNewSessionDialogOpen.asStateFlow()

    private val _isPipelineEditorOpen = MutableStateFlow(false)
    val isPipelineEditorOpen: StateFlow<Boolean> = _isPipelineEditorOpen.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _voiceRmsDb = MutableStateFlow(0f)
    val voiceRmsDb: StateFlow<Float> = _voiceRmsDb.asStateFlow()

    private val _attachedFile = MutableStateFlow<Pair<String, ByteArray>?>(null)
    val attachedFile: StateFlow<Pair<String, ByteArray>?> = _attachedFile.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        viewModelScope.launch {
            repository.latestSession.collectLatest { latest ->
                if (_currentSession.value == null && latest != null) {
                    _currentSession.value = latest
                    _dispatchMode.value = try {
                        DispatchMode.valueOf(latest.dispatchMode)
                    } catch (e: Exception) {
                        DispatchMode.AUTO_TRIAGE
                    }
                }
            }
        }

        viewModelScope.launch {
            _currentSession.collectLatest { session ->
                if (session != null) {
                    repository.getMessagesForSession(session.id).collectLatest { list ->
                        _messages.value = list
                    }
                } else {
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun selectSession(session: MeetingSession) {
        _currentSession.value = session
        _dispatchMode.value = try {
            DispatchMode.valueOf(session.dispatchMode)
        } catch (e: Exception) {
            DispatchMode.AUTO_TRIAGE
        }
    }

    fun setDispatchMode(mode: DispatchMode) {
        _dispatchMode.value = mode
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            val updated = session.copy(dispatchMode = mode.name)
            (getApplication() as BoardroomApp).database.sessionDao().updateSession(updated)
            _currentSession.value = updated
        }
    }

    fun toggleAdvisorSelection(advisorId: Int) {
        val current = _selectedAdvisorIds.value.toMutableSet()
        if (current.contains(advisorId)) {
            current.remove(advisorId)
        } else {
            current.add(advisorId)
        }
        _selectedAdvisorIds.value = current
    }

    fun toggleAdvisorPermission(advisorId: Int, isAllowed: Boolean) {
        viewModelScope.launch {
            (getApplication() as BoardroomApp).database.councilDao().updatePermission(advisorId, isAllowed)
        }
    }

    fun setPipelineSequence(sequence: List<Int>) {
        _pipelineSequence.value = sequence
    }

    fun createNewSession(title: String, agenda: String, mode: DispatchMode) {
        viewModelScope.launch {
            val session = repository.createNewSession(title, agenda, mode)
            _currentSession.value = session
            _dispatchMode.value = mode
            _isNewSessionDialogOpen.value = false
        }
    }

    fun sendChairmanMessage(text: String) {
        if (_isProcessing.value) return // جلوگیری از ارسال همزمان/دوبله در حین جلسه
        if (text.isBlank() && _attachedFile.value == null) return
        val session = _currentSession.value ?: return

        val attachment = _attachedFile.value
        val attachmentName = attachment?.first
        _attachedFile.value = null

        viewModelScope.launch {
            _isProcessing.value = true
            _progressMessage.value = "در حال ثبت درخواست رییس جلسه..."

            var attachmentPath: String? = null
            if (attachment != null) {
                val savedFile = memoryManager.saveAttachment(session, attachment.first, attachment.second)
                attachmentPath = savedFile.absolutePath
            }

            // Post Chairman message
            repository.postChairmanMessage(
                session = session,
                text = text,
                attachmentPath = attachmentPath,
                attachmentName = attachmentName
            )

            // Orchestrate execution among 20 councils
            repository.executeMeetingDeliberation(
                session = session,
                userPrompt = text + if (attachmentName != null) "\n[پیوست سند: $attachmentName]" else "",
                selectedAdvisorIds = _selectedAdvisorIds.value.toList(),
                pipelineAdvisorIds = _pipelineSequence.value,
                onProgressUpdate = { progress ->
                    _progressMessage.value = progress
                }
            )

            // Refresh the current session so the Results Pane immediately shows the
            // newly written executive summary / final resolution from the database.
            val refreshed = (getApplication() as BoardroomApp).database.sessionDao()
                .getSessionById(session.id)
            _currentSession.value = refreshed ?: session

            _isProcessing.value = false
            _progressMessage.value = ""
        }
    }

    fun attachFile(fileName: String, bytes: ByteArray) {
        _attachedFile.value = Pair(fileName, bytes)
    }

    fun removeAttachment() {
        _attachedFile.value = null
    }

    fun openAdvisorEdit(advisor: AdvisorEntity) {
        _selectedAdvisorForEdit.value = advisor
    }

    fun closeAdvisorEdit() {
        _selectedAdvisorForEdit.value = null
    }

    fun saveAdvisorEdit(
        advisor: AdvisorEntity,
        name: String,
        roleTitle: String,
        colorHex: String,
        iconName: String,
        subAgents: List<SubAgentSlot>
    ) {
        viewModelScope.launch {
            // Persist each slot's API key to the encrypted key store first. subAgentsToJson()
            // strips customApiKey before writing to Room, so this is the only place the key ends up.
            subAgents.forEach { slot ->
                secureKeyStore.saveKey(advisor.id, slot.slotNumber, slot.customApiKey)
            }
            val updated = advisor.copy(
                name = name,
                roleTitle = roleTitle,
                accentColorHex = colorHex,
                iconName = iconName,
                subAgentsJson = CouncilDataConverters.subAgentsToJson(subAgents)
            )
            repository.updateAdvisor(updated)
            _selectedAdvisorForEdit.value = null
        }
    }

    /**
     * Reads the currently stored (decrypted) API keys for an advisor's 5 slots, so the
     * config dialog can pre-fill them. Keys never round-trip through Room/subAgentsJson.
     */
    fun getApiKeysForAdvisor(advisorId: Int): Map<Int, String> {
        return (1..5).associateWith { slotNumber -> secureKeyStore.getKey(advisorId, slotNumber) }
    }

    fun setTriageLead(advisorId: Int) {
        viewModelScope.launch {
            (getApplication() as BoardroomApp).database.councilDao().setTriageLead(advisorId)
        }
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openNewSessionDialog() {
        _isNewSessionDialogOpen.value = true
    }

    fun closeNewSessionDialog() {
        _isNewSessionDialogOpen.value = false
    }

    fun addMasterDocument(name: String, description: String, content: String) {
        viewModelScope.launch {
            repository.addMasterFile(name, description, content)
        }
    }

    fun updateMemoryRootPath(newPath: String) {
        memoryManager.setMemoryRootPath(newPath)
    }

    fun getMemoryRootPath(): String {
        return memoryManager.getMemoryRootPath()
    }

    // Voice recognition handling
    fun startSpeechRecognition(onResultText: (String) -> Unit) {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isVoiceListening.value = true
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {
                            _voiceRmsDb.value = rmsdB
                        }
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _isVoiceListening.value = false
                        }
                        override fun onError(error: Int) {
                            _isVoiceListening.value = false
                        }
                        override fun onResults(results: Bundle?) {
                            _isVoiceListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onResultText(matches[0])
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onResultText(matches[0])
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
                }
                speechRecognizer?.startListening(intent)
            } else {
                // Fallback toggle simulation
                _isVoiceListening.value = true
            }
        } catch (e: Exception) {
            _isVoiceListening.value = false
        }
    }

    fun stopSpeechRecognition() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // ignore
        }
        _isVoiceListening.value = false
    }

    fun openPipelineEditor() {
        _isPipelineEditorOpen.value = true
    }

    fun closePipelineEditor() {
        _isPipelineEditorOpen.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopSpeechRecognition()
    }
}
