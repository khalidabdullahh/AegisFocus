package com.aegisfocus.app.core.common

sealed interface AegisResult<out T> {
    data class Success<T>(val data: T) : AegisResult<T>
    data class Error(val exception: Throwable, val message: String? = exception.message) : AegisResult<Nothing>
    data object Loading : AegisResult<Nothing>
}
