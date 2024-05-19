package io.github.nomisrev.contextparameters

data class User(val id: Long)

sealed interface UserError
data object UsernameMissing : UserError
data class UserExists(val username: String) : UserError

sealed interface PaymentError
data object ExpiredCard : PaymentError
data object InsufficientFunds : PaymentError
