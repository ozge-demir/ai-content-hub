package com.aicontenthub.auth

import com.aicontenthub.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val createdAt: Instant,
)

class UserRepository {

    suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email eq email.lowercase() }
            .singleOrNull()
            ?.toUser()
    }

    suspend fun create(email: String, password: String): User = dbQuery {
        val now = Instant.now()
        val hash = BCrypt.hashpw(password, BCrypt.gensalt(12))
        val id = UsersTable.insert {
            it[UsersTable.email] = email.lowercase()
            it[UsersTable.passwordHash] = hash
            it[UsersTable.createdAt] = now
        } get UsersTable.id
        User(id.value, email.lowercase(), hash, now)
    }

    fun verifyPassword(plain: String, hash: String): Boolean =
        BCrypt.checkpw(plain, hash)

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        createdAt = this[UsersTable.createdAt],
    )
}
