package com.taskcatalog.model

import java.time.LocalDateTime

data class NewTask(
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
