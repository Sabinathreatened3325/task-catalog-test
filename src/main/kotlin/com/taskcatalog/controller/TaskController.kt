package com.taskcatalog.controller

import com.taskcatalog.dto.CreateTaskRequest
import com.taskcatalog.dto.PageResponse
import com.taskcatalog.dto.TaskResponse
import com.taskcatalog.dto.UpdateTaskStatusRequest
import com.taskcatalog.model.TaskStatus
import com.taskcatalog.service.TaskService
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/tasks")
class TaskController(
    private val taskService: TaskService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(@Valid @RequestBody request: CreateTaskRequest): Mono<TaskResponse> {
        return taskService.createTask(request)
    }

    @GetMapping
    fun getTasks(
        @RequestParam @Min(value = 0, message = "Page must be greater than or equal to 0") page: Int,
        @RequestParam @Positive(message = "Size must be greater than 0") size: Int,
        @RequestParam(required = false) status: TaskStatus?
    ): Mono<PageResponse<TaskResponse>> {
        return taskService.getTasks(page, size, status)
    }

    @GetMapping("/{id}")
    fun getTaskById(
        @PathVariable
        @Positive(message = "Task id must be greater than 0")
        id: Long
    ): Mono<TaskResponse> {
        return taskService.getTaskById(id)
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable
        @Positive(message = "Task id must be greater than 0")
        id: Long,
        @Valid @RequestBody request: UpdateTaskStatusRequest
    ): Mono<TaskResponse> {
        return taskService.updateStatus(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTask(
        @PathVariable
        @Positive(message = "Task id must be greater than 0")
        id: Long
    ): Mono<Void> {
        return taskService.deleteTask(id)
    }
}
