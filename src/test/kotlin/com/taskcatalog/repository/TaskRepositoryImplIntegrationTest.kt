package com.taskcatalog.repository

import com.taskcatalog.model.NewTask
import com.taskcatalog.model.TaskStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.LocalDateTime

@SpringBootTest
class TaskRepositoryImplIntegrationTest {

    @Autowired
    lateinit var taskRepository: TaskRepositoryImpl

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun cleanTable() {
        jdbcClient.sql("delete from tasks").update()
    }

    @Test
    fun `save persists task and loads generated id`() {
        val createdAt = LocalDateTime.of(2026, 3, 26, 12, 0)
        val savedTask = taskRepository.save(
            NewTask(
                title = "Prepare report",
                description = "Monthly financial report",
                status = TaskStatus.NEW,
                createdAt = createdAt,
                updatedAt = createdAt
            )
        )

        assertThat(savedTask.id).isPositive()
        assertThat(savedTask.title).isEqualTo("Prepare report")
        assertThat(savedTask.description).isEqualTo("Monthly financial report")
        assertThat(savedTask.status).isEqualTo(TaskStatus.NEW)
        assertThat(savedTask.createdAt).isEqualTo(createdAt)
        assertThat(savedTask.updatedAt).isEqualTo(createdAt)
    }

    @Test
    fun `findById returns task when it exists and null otherwise`() {
        val savedTask = insertTask(
            title = "Draft specification",
            status = TaskStatus.IN_PROGRESS,
            createdAt = LocalDateTime.of(2026, 3, 26, 9, 30),
            updatedAt = LocalDateTime.of(2026, 3, 26, 9, 45)
        )

        val foundTask = taskRepository.findById(savedTask.id)

        assertThat(foundTask).isNotNull
        assertThat(foundTask?.id).isEqualTo(savedTask.id)
        assertThat(foundTask?.status).isEqualTo(TaskStatus.IN_PROGRESS)
        assertThat(taskRepository.findById(savedTask.id + 1000)).isNull()
    }

    @Test
    fun `findAll returns paged tasks ordered by createdAt descending and supports status filter`() {
        val oldest = insertTask(
            title = "Old task",
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.of(2026, 3, 26, 8, 0),
            updatedAt = LocalDateTime.of(2026, 3, 26, 8, 0)
        )
        val middle = insertTask(
            title = "Middle task",
            status = TaskStatus.DONE,
            createdAt = LocalDateTime.of(2026, 3, 26, 9, 0),
            updatedAt = LocalDateTime.of(2026, 3, 26, 9, 0)
        )
        val newest = insertTask(
            title = "Newest task",
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.of(2026, 3, 26, 10, 0),
            updatedAt = LocalDateTime.of(2026, 3, 26, 10, 0)
        )

        val firstPage = taskRepository.findAll(page = 0, size = 2, status = null)
        val filteredPage = taskRepository.findAll(page = 0, size = 10, status = TaskStatus.NEW)
        val secondPage = taskRepository.findAll(page = 1, size = 2, status = null)

        assertThat(firstPage.map { task -> task.title })
            .containsExactly(newest.title, middle.title)
        assertThat(secondPage.map { task -> task.title })
            .containsExactly(oldest.title)
        assertThat(filteredPage.map { task -> task.title })
            .containsExactly(newest.title, oldest.title)
    }

    @Test
    fun `count returns total and filtered totals`() {
        insertTask(
            title = "Task one",
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.of(2026, 3, 26, 8, 0),
            updatedAt = LocalDateTime.of(2026, 3, 26, 8, 0)
        )
        insertTask(
            title = "Task two",
            status = TaskStatus.DONE,
            createdAt = LocalDateTime.of(2026, 3, 26, 9, 0),
            updatedAt = LocalDateTime.of(2026, 3, 26, 9, 0)
        )
        insertTask(
            title = "Task three",
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.of(2026, 3, 26, 10, 0),
            updatedAt = LocalDateTime.of(2026, 3, 26, 10, 0)
        )

        assertThat(taskRepository.count(null)).isEqualTo(3L)
        assertThat(taskRepository.count(TaskStatus.NEW)).isEqualTo(2L)
        assertThat(taskRepository.count(TaskStatus.CANCELLED)).isEqualTo(0L)
    }

    @Test
    fun `updateStatus changes status and updatedAt`() {
        val originalUpdatedAt = LocalDateTime.of(2026, 3, 26, 11, 0)
        val savedTask = insertTask(
            title = "Review proposal",
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.of(2026, 3, 26, 10, 0),
            updatedAt = originalUpdatedAt
        )
        val newUpdatedAt = LocalDateTime.of(2026, 3, 27, 12, 15)

        val updatedTask = taskRepository.updateStatus(
            id = savedTask.id,
            status = TaskStatus.DONE,
            updatedAt = newUpdatedAt
        )

        assertThat(updatedTask).isNotNull
        assertThat(updatedTask?.id).isEqualTo(savedTask.id)
        assertThat(updatedTask?.status).isEqualTo(TaskStatus.DONE)
        assertThat(updatedTask?.updatedAt).isEqualTo(newUpdatedAt)
        assertThat(taskRepository.findById(savedTask.id)?.status).isEqualTo(TaskStatus.DONE)
        assertThat(taskRepository.updateStatus(savedTask.id + 1000, TaskStatus.CANCELLED, newUpdatedAt)).isNull()
    }

    @Test
    fun `deleteById removes task and reports missing rows`() {
        val savedTask = insertTask(
            title = "Archived task",
            status = TaskStatus.CANCELLED,
            createdAt = LocalDateTime.of(2026, 3, 26, 7, 0),
            updatedAt = LocalDateTime.of(2026, 3, 26, 7, 0)
        )

        val deleted = taskRepository.deleteById(savedTask.id)
        val deletedAgain = taskRepository.deleteById(savedTask.id)

        assertThat(deleted).isTrue
        assertThat(deletedAgain).isFalse
        assertThat(taskRepository.findById(savedTask.id)).isNull()
    }

    private fun insertTask(
        title: String,
        status: TaskStatus,
        createdAt: LocalDateTime,
        updatedAt: LocalDateTime
    ) = taskRepository.save(
        NewTask(
            title = title,
            description = "$title description",
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    )
}
