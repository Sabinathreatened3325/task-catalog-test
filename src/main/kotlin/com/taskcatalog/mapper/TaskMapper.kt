package com.taskcatalog.mapper

import com.taskcatalog.dto.TaskResponse
import com.taskcatalog.model.Task

object TaskMapper {

    fun toResponse(task: Task): TaskResponse {
        return TaskResponse(
            id = task.id,
            title = task.title,
            description = task.description,
            status = task.status,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }
}
