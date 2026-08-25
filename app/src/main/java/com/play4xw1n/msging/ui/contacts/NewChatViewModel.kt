package com.play4xw1n.msging.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.play4xw1n.msging.data.UserRepository
import com.play4xw1n.msging.data.User
import kotlinx.coroutines.flow.StateFlow

class NewChatViewModel(private val repository: UserRepository) : ViewModel() {

    val users: StateFlow<List<User>> = repository.users

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
                return NewChatViewModel(UserRepository()) as T
            }
        }
    }
}
