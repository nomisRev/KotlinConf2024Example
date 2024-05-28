package io.github.nomisrev

import arrow.AutoCloseScope
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.testcontainers.containers.PostgreSQLContainer

context(AutoCloseScope)
@ExperimentalStdlibApi
fun postgres(): PostgreSQLContainer<*> =
    install(
        PostgreSQLContainer("postgres:13.15")
            .also { it.start() }
    )

context(AutoCloseScope)
@ExperimentalStdlibApi
private fun dataSource(container: PostgreSQLContainer<*>): HikariDataSource =
    install(
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            driverClassName = container.driverClassName
        })
    )

context(AutoCloseScope)
@ExperimentalStdlibApi
fun database(container: PostgreSQLContainer<*>): Database =
    autoClose({
        Database.connect(dataSource(container))
    }) { db, _ -> TransactionManager.closeAndUnregister(db) }


object Users : LongIdTable() {
    val username: Column<String> = varchar("username", length = 50).uniqueIndex()
}
