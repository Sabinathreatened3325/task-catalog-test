package com.taskcatalog.dto

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val details: List<ApiErrorDetail> = emptyList()
)
