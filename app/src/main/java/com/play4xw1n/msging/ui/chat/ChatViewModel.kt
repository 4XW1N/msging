package com.play4xw1n.msging.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.play4xw1n.msging.data.ChatRepository
import com.play4xw1n.msging.data.model.Message
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    val messages: StateFlow<List<Message>> = repository.messages
    val connected: StateFlow<Boolean> = repository.connected

    fun start() = repository.start()

    fun send(sender: String, text: String) {
        if (text.isNotBlank()) repository.sendMessage(sender, text.trim())
    }

    fun stop() = repository.stop()

    companion object {
        fun factory(conversationId: String, contactName: String, isGroup: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatViewModel(ChatRepository(conversationId, contactName, isGroup)) }
        }
    }
}
