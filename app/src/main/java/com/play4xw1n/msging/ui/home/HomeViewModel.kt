package com.play4xw1n.msging.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.play4xw1n.msging.data.ConversationRepository
import com.play4xw1n.msging.data.Conversation
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(private val repository: ConversationRepository) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = repository.conversations

    init {
        repository.start()
    }

    override fun onCleared() {
        repository.stop()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(ConversationRepository()) as T
            }
        }
    }
}
