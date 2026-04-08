package com.taskcatalog.repository

import com.taskcatalog.model.NewTask
import com.taskcatalog.model.Task
import com.taskcatalog.model.TaskStatus
import java.time.LocalDateTime

interface TaskRepository {

    fun save(task: NewTask): Task

    fun findById(id: Long): Task?

    fun findAll(page: Int, size: Int, status: TaskStatus?): List<Task>

    fun count(status: TaskStatus?): Long

    fun updateStatus(id: Long, status: TaskStatus, updatedAt: LocalDateTime): Task?

    fun deleteById(id: Long): Boolean
}
