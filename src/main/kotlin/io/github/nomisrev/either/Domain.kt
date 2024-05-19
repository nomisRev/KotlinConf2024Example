package io.github.nomisrev.either

data class User(val id: Long)

sealed interface UserRegistrationError

sealed interface UserError : UserRegistrationError
data object UsernameMissing : UserError
data class UserExists(val username: String) : UserError

sealed interface PaymentError : UserRegistrationError
data object ExpiredCard : PaymentError
data object InsufficientFunds : PaymentError
