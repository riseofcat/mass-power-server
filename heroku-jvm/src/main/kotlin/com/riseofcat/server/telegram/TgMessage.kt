package com.riseofcat.server.telegram

import kotlinx.serialization.*

@Serializable data class TgMessage(
    @Optional val update_id: Int?=null,
    @Optional val message: Message?=null
)

@Serializable data class Message(
    @Optional val message_id: Int?=null,
    @Optional val from: From?=null,
    @Optional val chat: Chat?=null,
    @Optional val date: Int?=null,
    @Optional val text: String?=null,
    @Optional val entities: List<Entity>?=null
)

@Serializable data class Entity(
    @Optional val offset: Int?=null,
    @Optional val length: Int?=null,
    @Optional val type: String?=null
)

@Serializable data class From(
    @Optional val id: Int?=null,
    @Optional val is_bot: Boolean?=null,
    @Optional val first_name: String?=null,
    @Optional val last_name: String?=null,
    @Optional val username: String?=null,
    @Optional val language_code: String?=null
)

@Serializable data class Chat(
    @Optional val id: Int?=null,
    @Optional val first_name: String?=null,
    @Optional val last_name: String?=null,
    @Optional val username: String?=null,
    @Optional val type: String?=null
)