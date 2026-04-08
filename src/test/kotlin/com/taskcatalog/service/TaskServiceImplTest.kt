package com.taskcatalog.service

import com.taskcatalog.dto.CreateTaskRequest
import com.taskcatalog.dto.UpdateTaskStatusRequest
import com.taskcatalog.exception.TaskNotFoundException
import com.taskcatalog.model.NewTask
import com.taskcatalog.model.Task
import com.taskcatalog.model.TaskStatus
import com.taskcatalog.repository.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class TaskServiceImplTest {

    @Mock
    private lateinit var taskRepository: TaskRepository

    private lateinit var taskService: TaskServiceImpl

    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-03-26T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        taskService = TaskServiceImpl(taskRepository, fixedClock)
    }

    @Test
    fun `createTask should create a new task with NEW status`() {
        val request = CreateTaskRequest(
            title = "  Prepare report  ",
            description = "  Monthly financial report  "
        )
        val persistedTask = task(
            id = 1L,
            title = "Prepare report",
            description = "Monthly financial report",
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.of(2026, 3, 26, 12, 0, 0),
            updatedAt = LocalDateTime.of(2026, 3, 26, 12, 0, 0)
        )
        whenever(taskRepository.save(any())).thenReturn(persistedTask)

        StepVerifier.create(taskService.createTask(request))
            .assertNext { response ->
                assertThat(response.id).isEqualTo(1L)
                assertThat(response.title).isEqualTo("Prepare report")
                assertThat(response.description).isEqualTo("Monthly financial report")
                assertThat(response.status).isEqualTo(TaskStatus.NEW)
            }
            .verifyComplete()

        val newTaskCaptor = argumentCaptor<NewTask>()
        verify(taskRepository).save(newTaskCaptor.capture())
        val capturedTask = newTaskCaptor.firstValue
        assertThat(capturedTask.title).isEqualTo("Prepare report")
        assertThat(capturedTask.description).isEqualTo("Monthly financial report")
        assertThat(capturedTask.status).isEqualTo(TaskStatus.NEW)
        assertThat(capturedTask.createdAt).isEqualTo(LocalDateTime.of(2026, 3, 26, 12, 0, 0))
        assertThat(capturedTask.updatedAt).isEqualTo(capturedTask.createdAt)
    }

    @Test
    fun `getTaskById should return task when it exists`() {
        val existingTask = task(id = 5L)
        whenever(taskRepository.findById(5L)).thenReturn(existingTask)

        StepVerifier.create(taskService.getTaskById(5L))
            .assertNext { response ->
                assertThat(response.id).isEqualTo(5L)
                assertThat(response.title).isEqualTo(existingTask.title)
                assertThat(response.status).isEqualTo(existingTask.status)
            }
            .verifyComplete()
    }

    @Test
    fun `getTaskById should emit TaskNotFoundException when task does not exist`() {
        whenever(taskRepository.findById(42L)).thenReturn(null)

        StepVerifier.create(taskService.getTaskById(42L))
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(TaskNotFoundException::class.java)
                assertThat(error.message).isEqualTo("Task with id=42 was not found")
            }
            .verify()
    }

    @Test
    fun `updateStatus should update status and refresh updatedAt`() {
        val request = UpdateTaskStatusRequest(TaskStatus.DONE)
        val updatedTask = task(
            id = 7L,
            status = TaskStatus.DONE,
            updatedAt = LocalDateTime.of(2026, 3, 26, 12, 0, 0)
        )
        whenever(taskRepository.updateStatus(eq(7L), eq(TaskStatus.DONE), any())).thenReturn(updatedTask)

        StepVerifier.create(taskService.updateStatus(7L, request))
            .assertNext { response ->
                assertThat(response.id).isEqualTo(7L)
                assertThat(response.status).isEqualTo(TaskStatus.DONE)
                assertThat(response.updatedAt).isEqualTo(LocalDateTime.of(2026, 3, 26, 12, 0, 0))
            }
            .verifyComplete()

        verify(taskRepository).updateStatus(
            eq(7L),
            eq(TaskStatus.DONE),
            eq(LocalDateTime.of(2026, 3, 26, 12, 0, 0))
        )
    }

    @Test
    fun `deleteTask should complete when repository deletes a task`() {
        whenever(taskRepository.deleteById(9L)).thenReturn(true)

        StepVerifier.create(taskService.deleteTask(9L))
            .verifyComplete()

        verify(taskRepository).deleteById(9L)
    }

    @Test
    fun `getTasks should return paged response with filtering and pagination`() {
        val firstTask = task(id = 11L, status = TaskStatus.NEW)
        val secondTask = task(id = 10L, status = TaskStatus.NEW)
        whenever(taskRepository.findAll(0, 2, TaskStatus.NEW)).thenReturn(listOf(firstTask, secondTask))
        whenever(taskRepository.count(TaskStatus.NEW)).thenReturn(5L)

        StepVerifier.create(taskService.getTasks(0, 2, TaskStatus.NEW))
            .assertNext { response ->
                assertThat(response.page).isEqualTo(0)
                assertThat(response.size).isEqualTo(2)
                assertThat(response.totalElements).isEqualTo(5L)
                assertThat(response.totalPages).isEqualTo(3)
                assertThat(response.content).hasSize(2)
                assertThat(response.content.map { it.id }).containsExactly(11L, 10L)
                assertThat(response.content.map { it.status }).containsOnly(TaskStatus.NEW)
            }
            .verifyComplete()
    }

    @Test
    fun `deleteTask should fail when repository does not delete anything`() {
        whenever(taskRepository.deleteById(404L)).thenReturn(false)

        StepVerifier.create(taskService.deleteTask(404L))
            .expectError(TaskNotFoundException::class.java)
            .verify()
    }

    private fun task(
        id: Long,
        title: String = "Prepare report",
        description: String? = "Monthly financial report",
        status: TaskStatus = TaskStatus.NEW,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 3, 26, 11, 0, 0),
        updatedAt: LocalDateTime = LocalDateTime.of(2026, 3, 26, 11, 0, 0)
    ): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
