package com.taskcatalog.exception

import com.taskcatalog.dto.ApiErrorDetail
import com.taskcatalog.dto.ApiErrorResponse
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.bind.support.WebExchangeBindException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(TaskNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(exception: TaskNotFoundException): ApiErrorResponse {
        return ApiErrorResponse(
            code = "TASK_NOT_FOUND",
            message = exception.message ?: "Task was not found"
        )
    }

    @ExceptionHandler(RequestValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleRequestValidation(exception: RequestValidationException): ApiErrorResponse {
        return ApiErrorResponse(
            code = "VALIDATION_ERROR",
            message = exception.message ?: "Request validation failed"
        )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBindException(exception: WebExchangeBindException): ApiErrorResponse {
        val details = exception.bindingResult.fieldErrors.map { fieldError ->
            ApiErrorDetail(
                field = fieldError.field,
                message = fieldError.defaultMessage ?: "Invalid value"
            )
        }
        return ApiErrorResponse(
            code = "VALIDATION_ERROR",
            message = "Request validation failed",
            details = details
        )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHandlerMethodValidation(exception: HandlerMethodValidationException): ApiErrorResponse {
        val details = exception.valueResults.flatMap(::mapParameterValidationResult)
        return ApiErrorResponse(
            code = "VALIDATION_ERROR",
            message = "Request validation failed",
            details = details
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleConstraintViolation(exception: ConstraintViolationException): ApiErrorResponse {
        val details = exception.constraintViolations.map { violation ->
            ApiErrorDetail(
                field = violation.propertyPath.toString(),
                message = violation.message
            )
        }
        return ApiErrorResponse(
            code = "VALIDATION_ERROR",
            message = "Request validation failed",
            details = details
        )
    }

    @ExceptionHandler(ServerWebInputException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleServerWebInput(exception: ServerWebInputException): ApiErrorResponse {
        return ApiErrorResponse(
            code = "BAD_REQUEST",
            message = exception.reason ?: "Malformed request"
        )
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpected(exception: Throwable): ApiErrorResponse {
        logger.error("Unhandled exception", exception)
        return ApiErrorResponse(
            code = "INTERNAL_ERROR",
            message = "Unexpected server error"
        )
    }

    private fun mapParameterValidationResult(result: ParameterValidationResult): List<ApiErrorDetail> {
        val fieldName = result.methodParameter.parameterName ?: "parameter"
        return result.resolvableErrors.map { error ->
            ApiErrorDetail(
                field = fieldName,
                message = error.defaultMessage ?: "Invalid value"
            )
        }
    }
}
