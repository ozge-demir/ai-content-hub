package com.aicontenthub.drafts

import com.aicontenthub.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

data class Draft(
    val id: UUID,
    val userId: UUID,
    val title: String,
    val body: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

class DraftRepository {

    suspend fun listForUser(userId: UUID): List<Draft> = dbQuery {
        DraftsTable.selectAll()
            .where { DraftsTable.userId eq userId }
            .orderBy(DraftsTable.createdAt to SortOrder.DESC)
            .map { it.toDraft() }
    }

    suspend fun findOwned(id: UUID, userId: UUID): Draft? = dbQuery {
        DraftsTable.selectAll()
            .where { (DraftsTable.id eq id) and (DraftsTable.userId eq userId) }
            .singleOrNull()
            ?.toDraft()
    }

    suspend fun create(userId: UUID, title: String, body: String): Draft = dbQuery {
        val now = Instant.now()
        val id = DraftsTable.insert {
            it[DraftsTable.userId] = userId
            it[DraftsTable.title] = title
            it[DraftsTable.body] = body
            it[createdAt] = now
            it[updatedAt] = now
        } get DraftsTable.id
        Draft(id.value, userId, title, body, now, now)
    }

    suspend fun update(id: UUID, userId: UUID, title: String, body: String): Boolean = dbQuery {
        val now = Instant.now()
        DraftsTable.update({ (DraftsTable.id eq id) and (DraftsTable.userId eq userId) }) {
            it[DraftsTable.title] = title
            it[DraftsTable.body] = body
            it[updatedAt] = now
        } > 0
    }

    suspend fun delete(id: UUID, userId: UUID): Boolean = dbQuery {
        DraftsTable.deleteWhere { (DraftsTable.id eq id) and (DraftsTable.userId eq userId) } > 0
    }

    private fun ResultRow.toDraft() = Draft(
        id = this[DraftsTable.id].value,
        userId = this[DraftsTable.userId].value,
        title = this[DraftsTable.title],
        body = this[DraftsTable.body],
        createdAt = this[DraftsTable.createdAt],
        updatedAt = this[DraftsTable.updatedAt],
    )
}
