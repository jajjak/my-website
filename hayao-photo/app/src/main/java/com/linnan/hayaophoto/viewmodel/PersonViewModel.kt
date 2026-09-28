package com.linnan.hayaophoto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linnan.hayaophoto.AppGraph
import com.linnan.hayaophoto.data.PersonEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonViewModel : ViewModel() {
    private val repo = AppGraph.repository

    val persons: StateFlow<List<PersonEntity>> = repo.observePersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPerson(name: String, memo: String, profileBytes: ByteArray?, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.addPerson(name, memo, profileBytes)
            onDone(id)
        }
    }

    fun updatePerson(person: PersonEntity, name: String, memo: String, newProfileBytes: ByteArray?) {
        viewModelScope.launch { repo.updatePerson(person, name, memo, newProfileBytes) }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch { repo.deletePerson(person) }
    }
}
