package com.taskcatalog.service

import com.taskcatalog.dto.CreateTaskRequest
import com.taskcatalog.dto.PageResponse
import com.taskcatalog.dto.TaskResponse
import com.taskcatalog.dto.UpdateTaskStatusRequest
import com.taskcatalog.model.TaskStatus
import reactor.core.publisher.Mono

interface TaskService {

    fun createTask(request: CreateTaskRequest): Mono<TaskResponse>

    fun getTaskById(id: Long): Mono<TaskResponse>

    fun getTasks(page: Int, size: Int, status: TaskStatus?): Mono<PageResponse<TaskResponse>>

    fun updateStatus(id: Long, request: UpdateTaskStatusRequest): Mono<TaskResponse>

    fun deleteTask(id: Long): Mono<Void>
}
