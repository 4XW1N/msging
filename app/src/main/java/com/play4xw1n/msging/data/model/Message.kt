package com.play4xw1n.msging.data.model

data class Message(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isSystem: Boolean
)
