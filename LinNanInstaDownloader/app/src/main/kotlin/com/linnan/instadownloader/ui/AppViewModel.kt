package com.linnan.instadownloader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linnan.instadownloader.data.model.FailureReason
import com.linnan.instadownloader.data.model.HistoryItem
import com.linnan.instadownloader.data.model.InstaOutcome
import com.linnan.instadownloader.data.model.InstagramPost
import com.linnan.instadownloader.data.model.MediaItem
import com.linnan.instadownloader.data.repository.HistoryRepository
import com.linnan.instadownloader.data.repository.MediaRepository
import com.linnan.instadownloader.download.DownloadRepository
import com.linnan.instadownloader.download.DownloadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { HOME, RESULT, HISTORY }

sealed class AnalyzeState {
    data object Idle : AnalyzeState()
    data object Analyzing : AnalyzeState()
    data class Success(val post: InstagramPost) : AnalyzeState()
    data class Error(val reason: FailureReason) : AnalyzeState()
}

data class SavedInfo(val uri: Uri, val mimeType: String)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaRepository()
    private val downloadRepository = DownloadRepository(application)
    private val historyRepository = HistoryRepository(application)

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _analyzeState = MutableStateFlow<AnalyzeState>(AnalyzeState.Idle)
    val analyzeState: StateFlow<AnalyzeState> = _analyzeState.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _savedInfo = MutableStateFlow<SavedInfo?>(null)
    val savedInfo: StateFlow<SavedInfo?> = _savedInfo.asStateFlow()

    val history: StateFlow<List<HistoryItem>> = historyRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var analyzeJob: Job? = null
    private val downloadJobs = mutableMapOf<String, Job>()

    fun onUrlChanged(text: String) {
        _urlInput.value = text
    }

    fun onSharedUrlReceived(text: String) {
        _urlInput.value = text
        _screen.value = Screen.HOME
        analyze()
    }

    fun goHome() {
        _screen.value = Screen.HOME
    }

    fun goHistory() {
        _screen.value = Screen.HISTORY
    }

    fun analyze() {
        val url = _urlInput.value
        if (analyzeJob?.isActive == true) return
        _downloadStates.value = emptyMap()
        analyzeJob = viewModelScope.launch {
            _analyzeState.value = AnalyzeState.Analyzing
            when (val result = mediaRepository.analyze(url)) {
                is InstaOutcome.Success -> {
                    _analyzeState.value = AnalyzeState.Success(result.data)
                    _screen.value = Screen.RESULT
                }
                is InstaOutcome.Failure -> {
                    _analyzeState.value = AnalyzeState.Error(result.reason)
                }
            }
        }
    }

    fun saveItem(item: MediaItem) {
        val post = (_analyzeState.value as? AnalyzeState.Success)?.post ?: return
        if (downloadJobs[item.id]?.isActive == true) return
        downloadJobs[item.id] = viewModelScope.launch {
            downloadRepository.download(item, post.shortcode).collect { state ->
                _downloadStates.update { it + (item.id to state) }
                if (state is DownloadState.Completed) {
                    historyRepository.record(
                        shortcode = post.shortcode,
                        ownerUsername = post.ownerUsername,
                        mediaType = state.mediaType,
                        width = item.width,
                        height = item.height,
                        fileSizeBytes = state.sizeBytes,
                        contentUri = state.uri.toString(),
                        mimeType = state.mimeType
                    )
                    _savedInfo.value = SavedInfo(state.uri, state.mimeType)
                }
            }
        }
    }

    fun saveAll() {
        val post = (_analyzeState.value as? AnalyzeState.Success)?.post ?: return
        post.items.forEach { item ->
            val current = _downloadStates.value[item.id]
            if (current !is DownloadState.Completed) {
                saveItem(item)
            }
        }
    }

    fun dismissSavedInfo() {
        _savedInfo.value = null
    }
}
