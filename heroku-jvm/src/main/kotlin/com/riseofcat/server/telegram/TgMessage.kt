package com.riseofcat.server.telegram

import kotlinx.serialization.Serializable

@Serializable data class TgMessage(
    val message: Message
)

@Serializable data class Message(
    val message_id: Int,
    val from: From,
    val chat: Chat,
    val date: Int,
    val text: String,
    val entities: List<Entity>
)

@Serializable data class Entity(
    val offset: Int,
    val length: Int,
    val type: String
)

@Serializable data class From(
    val id: Int,
    val is_bot: Boolean,
    val first_name: String,
    val last_name: String,
    val username: String,
    val language_code: String
)

@Serializable data class Chat(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val username: String,
    val type: String
)