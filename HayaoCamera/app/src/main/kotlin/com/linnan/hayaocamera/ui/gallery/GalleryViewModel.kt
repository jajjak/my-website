package com.linnan.hayaocamera.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linnan.hayaocamera.data.GalleryItem
import com.linnan.hayaocamera.data.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GalleryRepository(application)

    private val _items = MutableStateFlow<List<GalleryItem>>(emptyList())
    val items: StateFlow<List<GalleryItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deleteResult = MutableStateFlow<Boolean?>(null)
    val deleteResult: StateFlow<Boolean?> = _deleteResult.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _items.value = repository.loadOwnMedia()
            _isLoading.value = false
        }
    }

    fun delete(item: GalleryItem) {
        viewModelScope.launch {
            val success = repository.delete(item)
            _deleteResult.value = success
            if (success) {
                _items.value = _items.value.filterNot { it.id == item.id && it.isVideo == item.isVideo }
            }
        }
    }

    fun consumeDeleteResult() {
        _deleteResult.value = null
    }
}
