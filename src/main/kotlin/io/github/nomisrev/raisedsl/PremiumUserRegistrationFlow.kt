package io.github.nomisrev.raisedsl

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import arrow.core.raise.recover
import io.github.nomisrev.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.PaymentRequired
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.postgresql.util.PSQLState.UNIQUE_VIOLATION

/**
 * Get [username] from path parameters.
 * or [Raise] [UsernameMissing] error.
 */
fun Raise<UsernameMissing>.username(call: ApplicationCall): String =
    ensureNotNull(call.parameters["username"]) { UsernameMissing }

/**
 * Insert the user,
 * or [Raise] [UserExists] on unique violation,
 * or rethrow any other exceptions that occurred.
 */
fun Raise<UserExists>.insertUser(username: String): User =
    try {
        val id = Users.insert {
            it[Users.username] = username
        } get Users.id
        User(id.value)
    } catch (e: ExposedSQLException) {
        if (e.sqlState == UNIQUE_VIOLATION.state) raise(UserExists(username))
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
fun Raise<PaymentError>.receivePayment(user: User): Unit =
    try {
        println("User ${user.id} attempting to pay using payment service")
    } catch (e: PaymentSaasException) {
        raise(e.toPaymentError())
    }

/**
 * Service layer function which combines all features,
 * into an end-to-end feature.
 */
suspend fun Raise<UserRegistrationError>.registerPremiumUser(request: ApplicationCall) =
    newSuspendedTransaction {
        val name = username(request)
        val user = insertUser(name)
        receivePayment(user)
    }

/**
 * Actual route handler that will be installed in Ktor,
 * calls the service method,
 * provides all context dependencies,
 * and handles all errors.
 */
fun Routing.premiumRoute2() = post("/premium2/{username}") {
    newSuspendedTransaction {
        recover({
            registerPremiumUser(call)
            call.respond(HttpStatusCode.Created)
        }) { e: UserRegistrationError -> call.respond(e.toContent()) }
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