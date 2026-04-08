package com.taskcatalog.controller

import com.taskcatalog.dto.PageResponse
import com.taskcatalog.dto.TaskResponse
import com.taskcatalog.dto.CreateTaskRequest
import com.taskcatalog.dto.UpdateTaskStatusRequest
import com.taskcatalog.exception.GlobalExceptionHandler
import com.taskcatalog.exception.TaskNotFoundException
import com.taskcatalog.model.TaskStatus
import com.taskcatalog.service.TaskService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.context.bean.override.mockito.MockitoBean
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@WebFluxTest(controllers = [TaskController::class])
@Import(GlobalExceptionHandler::class)
class TaskControllerTest(
    @Autowired private val webTestClient: WebTestClient
) {

    @MockitoBean
    private lateinit var taskService: TaskService

    @Test
    fun `create task returns 201 and created task body`() {
        val expectedTask = taskResponse()
        whenever(taskService.createTask(any())).thenReturn(Mono.just(expectedTask))

        webTestClient.post()
            .uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CreateTaskRequest("Prepare report", "Monthly financial report"))
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isEqualTo(expectedTask.id.toInt())
            .jsonPath("$.title").isEqualTo(expectedTask.title)
            .jsonPath("$.description").isEqualTo(expectedTask.description!!)
            .jsonPath("$.status").isEqualTo(expectedTask.status.name)
            .jsonPath("$.createdAt").isEqualTo(expectedTask.createdAt.toString())
            .jsonPath("$.updatedAt").isEqualTo(expectedTask.updatedAt.toString())
    }

    @Test
    fun `get task by id returns 200 and task body`() {
        val expectedTask = taskResponse()
        whenever(taskService.getTaskById(expectedTask.id)).thenReturn(Mono.just(expectedTask))

        webTestClient.get()
            .uri("/api/tasks/{id}", expectedTask.id)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(expectedTask.id.toInt())
            .jsonPath("$.title").isEqualTo(expectedTask.title)
            .jsonPath("$.description").isEqualTo(expectedTask.description!!)
            .jsonPath("$.status").isEqualTo(expectedTask.status.name)
            .jsonPath("$.createdAt").isEqualTo(expectedTask.createdAt.toString())
            .jsonPath("$.updatedAt").isEqualTo(expectedTask.updatedAt.toString())
    }

    @Test
    fun `get tasks returns 200 with paginated content`() {
        val firstTask = taskResponse(id = 10, title = "Prepare report")
        val secondTask = taskResponse(
            id = 11,
            title = "Check budget",
            status = TaskStatus.IN_PROGRESS,
            createdAt = FIXED_DATE_TIME.minusHours(1),
            updatedAt = FIXED_DATE_TIME.minusHours(1)
        )
        val pageResponse = PageResponse(
            content = listOf(firstTask, secondTask),
            page = 0,
            size = 10,
            totalElements = 2,
            totalPages = 1
        )
        whenever(taskService.getTasks(0, 10, TaskStatus.NEW)).thenReturn(Mono.just(pageResponse))

        webTestClient.get()
            .uri("/api/tasks?page=0&size=10&status=NEW")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.page").isEqualTo(0)
            .jsonPath("$.size").isEqualTo(10)
            .jsonPath("$.totalElements").isEqualTo(2)
            .jsonPath("$.totalPages").isEqualTo(1)
            .jsonPath("$.content[0].id").isEqualTo(firstTask.id.toInt())
            .jsonPath("$.content[0].title").isEqualTo(firstTask.title)
            .jsonPath("$.content[0].status").isEqualTo(firstTask.status.name)
            .jsonPath("$.content[1].id").isEqualTo(secondTask.id.toInt())
            .jsonPath("$.content[1].status").isEqualTo(secondTask.status.name)
    }

    @Test
    fun `update status returns 200 and updated task body`() {
        val expectedTask = taskResponse(status = TaskStatus.DONE)
        whenever(taskService.updateStatus(expectedTask.id, UpdateTaskStatusRequest(TaskStatus.DONE)))
            .thenReturn(Mono.just(expectedTask))

        webTestClient.patch()
            .uri("/api/tasks/{id}/status", expectedTask.id)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateTaskStatusRequest(TaskStatus.DONE))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(expectedTask.id.toInt())
            .jsonPath("$.status").isEqualTo(TaskStatus.DONE.name)
            .jsonPath("$.updatedAt").isEqualTo(expectedTask.updatedAt.toString())
    }

    @Test
    fun `delete task returns 204 no content`() {
        whenever(taskService.deleteTask(1L)).thenReturn(Mono.empty())

        webTestClient.delete()
            .uri("/api/tasks/{id}", 1L)
            .exchange()
            .expectStatus().isNoContent
            .expectBody().isEmpty
    }

    @Test
    fun `get task by id returns 404 when task is absent`() {
        whenever(taskService.getTaskById(404L)).thenReturn(Mono.error(TaskNotFoundException(404L)))

        webTestClient.get()
            .uri("/api/tasks/{id}", 404L)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.code").isEqualTo("TASK_NOT_FOUND")
            .jsonPath("$.message").isEqualTo("Task with id=404 was not found")
    }

    @Test
    fun `create task rejects blank title`() {
        webTestClient.post()
            .uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CreateTaskRequest("  ", "Monthly financial report"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
            .jsonPath("$.details[0].field").isEqualTo("title")
    }

    @Test
    fun `get tasks rejects invalid pagination parameters`() {
        webTestClient.get()
            .uri("/api/tasks?page=-1&size=10")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
            .jsonPath("$.message").isEqualTo("Request validation failed")
            .jsonPath("$.details[0].field").isEqualTo("page")
            .jsonPath("$.details[0].message").isEqualTo("Page must be greater than or equal to 0")
    }

    @Test
    fun `get tasks rejects zero page size`() {
        webTestClient.get()
            .uri("/api/tasks?page=0&size=0")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
            .jsonPath("$.message").isEqualTo("Request validation failed")
            .jsonPath("$.details[0].field").isEqualTo("size")
            .jsonPath("$.details[0].message").isEqualTo("Size must be greater than 0")
    }

    @Test
    fun `get task rejects non positive id`() {
        webTestClient.get()
            .uri("/api/tasks/0")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
            .jsonPath("$.message").isEqualTo("Request validation failed")
            .jsonPath("$.details[0].field").isEqualTo("id")
            .jsonPath("$.details[0].message").isEqualTo("Task id must be greater than 0")
    }

    private fun taskResponse(
        id: Long = 1L,
        title: String = "Prepare report",
        description: String? = "Monthly financial report",
        status: TaskStatus = TaskStatus.NEW,
        createdAt: LocalDateTime = FIXED_DATE_TIME,
        updatedAt: LocalDateTime = FIXED_DATE_TIME
    ): TaskResponse {
        return TaskResponse(
            id = id,
            title = title,
            description = description,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private companion object {
        private val FIXED_DATE_TIME = LocalDateTime.of(2026, 3, 26, 12, 0, 5)
    }
}
