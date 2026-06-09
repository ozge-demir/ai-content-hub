package com.aicontenthub.drafts

import com.aicontenthub.auth.UsersTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object DraftsTable : UUIDTable("drafts", "id") {
    val userId = reference("user_id", UsersTable)
    val title = varchar("title", 200)
    val body = text("body")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
