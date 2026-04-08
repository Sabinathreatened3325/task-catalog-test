package com.taskcatalog.dto

import com.taskcatalog.model.TaskStatus

data class UpdateTaskStatusRequest(
    val status: TaskStatus
)
