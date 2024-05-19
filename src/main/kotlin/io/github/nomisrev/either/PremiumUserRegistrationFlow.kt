package io.github.nomisrev.either

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.recover
import arrow.core.right
import io.github.nomisrev.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.PaymentRequired
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.postgresql.util.PSQLState.UNIQUE_VIOLATION

/**
 * Get [username] from path parameters.
 * or [Raise] [UsernameMissing] error.
 */
fun username(call: ApplicationCall): Either<UsernameMissing, String> =
    call.parameters["username"]?.right() ?: UsernameMissing.left()

/**
 * Insert the user,
 * or [Raise] [UserExists] on unique violation,
 * or rethrow any other exceptions that occurred.
 */
fun insertUser(username: String): Either<UserExists, User> =
    try {
        val id = Users.insert {
            it[Users.username] = username
        } get Users.id
        User(id.value).right()
    } catch (e: ExposedSQLException) {
        if (e.sqlState == UNIQUE_VIOLATION.state) UserExists(username).left()
        else throw e
    }

/** Fake exception from 3rd party SDK */
class PaymentSaasException : RuntimeException()

/** Stub impl for our fake exception */
fun PaymentSaasException.toPaymentError(): PaymentError =
    ExpiredCard

/**
 * Request SAAS payment service to process the payment,
 * or transform any [PaymentSaasException] that occurred into a typed error.
 */
fun receivePayment(user: User): Either<PaymentError, Unit> =
    Either.catchOrThrow<PaymentSaasException, _> {
        println("User ${user.id} attempting to pay using payment service")
    }.mapLeft(PaymentSaasException::toPaymentError)

/**
 * Service layer function which combines all features,
 * into an end-to-end feature.
 */
fun Transaction.registerPremiumUser(request: ApplicationCall): Either<UserRegistrationError, Unit> =
    either {
        val name = username(request).bind()
        val user = insertUser(name).bind()
        receivePayment(user).bind()
    }

/**
 * Actual route handler that will be installed in Ktor,
 * calls the service method,
 * provides all context dependencies,
 * and handles all errors.
 */
context(Routing)
fun premiumRoute3() = post("/premium3/{username}") {
    newSuspendedTransaction {
        when (val result = registerPremiumUser(call)) {
            is Either.Left -> call.respond(result.value.toContent())
            is Either.Right -> call.respond(HttpStatusCode.Created)
        }
    }
}

/** Typed Ktor error handler for [UserError]. */
fun UserRegistrationError.toContent(): TextContent =
    when (this) {
        is UserExists ->
            TextContent("Username already exists", Conflict)

        UsernameMissing ->
            TextContent("Username already exists", BadRequest)

        ExpiredCard ->
            TextContent("Card expired", PaymentRequired)

        InsufficientFunds ->
            TextContent("Credit maxed", PaymentRequired)
    }

private fun TextContent(
    content: String,
    statusCode: HttpStatusCode
): TextContent =
    TextContent(content, ContentType.Text.Plain, statusCode)