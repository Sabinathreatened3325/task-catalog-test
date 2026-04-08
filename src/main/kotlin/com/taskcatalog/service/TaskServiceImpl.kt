package com.taskcatalog.service

import com.taskcatalog.dto.CreateTaskRequest
import com.taskcatalog.dto.PageResponse
import com.taskcatalog.dto.TaskResponse
import com.taskcatalog.dto.UpdateTaskStatusRequest
import com.taskcatalog.exception.TaskNotFoundException
import com.taskcatalog.mapper.TaskMapper
import com.taskcatalog.model.NewTask
import com.taskcatalog.model.TaskStatus
import com.taskcatalog.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Clock
import java.time.LocalDateTime

@Service
class TaskServiceImpl(
    private val taskRepository: TaskRepository,
    private val clock: Clock
) : TaskService {

    private val logger = LoggerFactory.getLogger(javaClass)

    // JdbcClient is blocking, so every repository call must run off the event-loop threads.
    private val jdbcScheduler: Scheduler = Schedulers.boundedElastic()

    override fun createTask(request: CreateTaskRequest): Mono<TaskResponse> {
        val createdAt = LocalDateTime.now(clock)
        val newTask = NewTask(
            title = request.title.trim(),
            description = request.description?.trim()?.takeIf { description -> description.isNotEmpty() },
            status = TaskStatus.NEW,
            createdAt = createdAt,
            updatedAt = createdAt
        )

        return Mono.fromCallable { taskRepository.save(newTask) }
            .subscribeOn(jdbcScheduler)
            .map(TaskMapper::toResponse)
            .doOnSuccess { response -> logger.info("Created task with id={}", response.id) }
    }

    override fun getTaskById(id: Long): Mono<TaskResponse> {
        return Mono.fromCallable {
            taskRepository.findById(id) ?: throw TaskNotFoundException(id)
        }
            .subscribeOn(jdbcScheduler)
            .map(TaskMapper::toResponse)
    }

    override fun getTasks(page: Int, size: Int, status: TaskStatus?): Mono<PageResponse<TaskResponse>> {
        val tasksMono = Mono.fromCallable { taskRepository.findAll(page, size, status) }
            .subscribeOn(jdbcScheduler)

        val totalMono = Mono.fromCallable { taskRepository.count(status) }
            .subscribeOn(jdbcScheduler)

        return Mono.zip(tasksMono, totalMono)
            .map { tuple ->
                val tasks = tuple.t1.map(TaskMapper::toResponse)
                val totalElements = tuple.t2
                PageResponse(
                    content = tasks,
                    page = page,
                    size = size,
                    totalElements = totalElements,
                    totalPages = calculateTotalPages(totalElements, size)
                )
            }
    }

    override fun updateStatus(id: Long, request: UpdateTaskStatusRequest): Mono<TaskResponse> {
        val updatedAt = LocalDateTime.now(clock)
        return Mono.fromCallable {
            taskRepository.updateStatus(id, request.status, updatedAt) ?: throw TaskNotFoundException(id)
        }
            .subscribeOn(jdbcScheduler)
            .map(TaskMapper::toResponse)
            .doOnSuccess { response ->
                logger.info("Updated task status for id={} to {}", response.id, response.status)
            }
    }

    override fun deleteTask(id: Long): Mono<Void> {
        return Mono.fromCallable {
            val deleted = taskRepository.deleteById(id)
            if (!deleted) {
                throw TaskNotFoundException(id)
            }
            id
        }
            .subscribeOn(jdbcScheduler)
            .doOnSuccess { taskId -> logger.info("Deleted task with id={}", taskId) }
            .then()
    }

    private fun calculateTotalPages(totalElements: Long, size: Int): Int {
        if (totalElements == 0L) {
            return 0
        }
        return ((totalElements + size - 1) / size).toInt()
    }
}
