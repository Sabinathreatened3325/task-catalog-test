package com.taskcatalog.exception

class TaskNotFoundException(taskId: Long) : RuntimeException("Task with id=$taskId was not found")
