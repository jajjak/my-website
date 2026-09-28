package com.linnan.instadownloader.data.model

enum class FailureReason {
    INVALID_URL,
    NOT_FOUND,
    PRIVATE_OR_LOGIN_REQUIRED,
    MEDIA_UNAVAILABLE,
    RATE_LIMITED,
    NETWORK_ERROR,
    UNKNOWN
}

sealed class InstaOutcome<out T> {
    data class Success<T>(val data: T) : InstaOutcome<T>()
    data class Failure(val reason: FailureReason) : InstaOutcome<Nothing>()
}
