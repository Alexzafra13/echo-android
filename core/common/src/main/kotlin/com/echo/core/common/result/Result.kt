package com.echo.core.common.result

import com.echo.core.common.error.AppError
import com.echo.core.common.error.userMessage

/**
 * A generic class that holds a value or an error.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }

    /**
     * Returns the AppError, converting if necessary
     */
    fun appErrorOrNull(): AppError? = when (this) {
        is Error -> AppError.from(exception)
        else -> null
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
        is Loading -> Loading
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> Error(exception)
        is Loading -> Loading
    }

    inline fun mapError(transform: (Throwable) -> Throwable): Result<T> = when (this) {
        is Success -> this
        is Error -> Error(transform(exception))
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    /**
     * Get error message suitable for displaying to users
     */
    fun errorMessage(): String? = when (this) {
        is Error -> exception.userMessage
        else -> null
    }

    companion object {
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Throwable) {
                Error(AppError.from(e))
            }
        }

        /**
         * Run a suspending block and catch exceptions
         */
        suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Throwable) {
                Error(AppError.from(e))
            }
        }
    }
}

/**
 * Extension to convert Kotlin Result to our Result type.
 */
fun <T> kotlin.Result<T>.toResult(): Result<T> =
    fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppError.from(it)) }
    )

/**
 * Combine multiple Results into one
 */
inline fun <T1, T2, R> combineResults(
    r1: Result<T1>,
    r2: Result<T2>,
    transform: (T1, T2) -> R
): Result<R> {
    return when {
        r1 is Result.Error -> Result.Error(r1.exception)
        r2 is Result.Error -> Result.Error(r2.exception)
        r1 is Result.Loading || r2 is Result.Loading -> Result.Loading
        r1 is Result.Success && r2 is Result.Success -> Result.Success(transform(r1.data, r2.data))
        else -> Result.Loading
    }
}
