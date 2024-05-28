package io.github.nomisrev.contextparameters

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import arrow.core.raise.recover
import io.github.nomisrev.*
import io.ktor.http.*
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
context(Raise<UsernameMissing>)
fun ApplicationCall.username(): String =
    ensureNotNull(parameters["username"]) { UsernameMissing }

/**
 * Insert the user,
 * or [Raise] [UserExists] on unique violation,
 * or rethrow any other exceptions that occurred.
 */
context(Raise<UserExists>)
fun insertUser(username: String): User =
    try {
        val id = Users.insert {
            it[Users.username] = username
        } get Users.id
        User(id.value, username)
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
context(Raise<PaymentError>)
fun User.receivePayment(): Unit =
    try {
        throw PaymentSaasException()
        println("User $id attempting to pay using payment service")
    } catch (e: PaymentSaasException) {
        raise(e.toPaymentError())
    }

/**
 * Service layer function which combines all features,
 * into an end-to-end feature.
 */
context(Raise<UserError>, Raise<PaymentError>, Transaction)
suspend fun registerPremiumUser(request: ApplicationCall) {
    val name = request.username()
    val user = insertUser(name)
    user.receivePayment()
}

/**
 * Actual route handler that will be installed in Ktor,
 * calls the service method,
 * provides all context dependencies,
 * and handles all errors.
 */
context(Routing)
fun premiumRoute() = post("/premium/{username}") {
    recover({
        recover({
            newSuspendedTransaction {
                registerPremiumUser(call)
                call.respond(HttpStatusCode.Created)
            }
        }) { e: UserError -> e.respond() }
    }) { e: PaymentError -> e.respond() }
}

/** Typed Ktor error handler for [UserError]. */
context(RouteContext)
suspend fun UserError.respond(): Unit =
    when (this) {
        is UserExists ->
            call.respond(HttpStatusCode.Conflict, "Username already exists")

        UsernameMissing ->
            call.respond(HttpStatusCode.BadRequest, "Username missing")
    }

/** Typed Ktor error handler for [PaymentError]. */
context(RouteContext)
suspend fun PaymentError.respond(): Unit =
    when (this) {
        ExpiredCard ->
            call.respond(HttpStatusCode.PaymentRequired, "Card expired")

        InsufficientFunds ->
            call.respond(HttpStatusCode.PaymentRequired, "Credit maxed")
    }


/** An alias for routing receiver from Ktor */
typealias RouteContext = PipelineContext<Unit, ApplicationCall>
