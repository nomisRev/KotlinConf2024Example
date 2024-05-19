package io.github.nomisrev

import arrow.autoCloseScope
import io.github.nomisrev.contextparameters.premiumRoute
import io.github.nomisrev.either.premiumRoute3
import io.github.nomisrev.raisedsl.premiumRoute2
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    autoCloseScope {
        val container = postgres()
        val database = database(container)
        transaction(database) { SchemaUtils.create(Users) }
        embeddedServer(Netty, 8080) {
            routing {
                premiumRoute()
                premiumRoute2()
                premiumRoute3()
            }
        }.start(wait = true)
    }
}
