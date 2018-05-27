package com.riseofcat.server.telegram

data class TgMessage(
    val message: Message
)

data class Message(
    val message_id: Int,
    val from: From,
    val chat: Chat,
    val date: Int,
    val text: String,
    val entities: List<Entity>
)

data class Entity(
    val offset: Int,
    val length: Int,
    val type: String
)

data class From(
    val id: Int,
    val is_bot: Boolean,
    val first_name: String,
    val last_name: String,
    val username: String,
    val language_code: String
)

data class Chat(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val username: String,
    val type: String
)