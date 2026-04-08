package com.taskcatalog.repository

import com.taskcatalog.model.NewTask
import com.taskcatalog.model.Task
import com.taskcatalog.model.TaskStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class TaskRepositoryImpl(
    private val jdbcClient: JdbcClient
) : TaskRepository {

    override fun save(task: NewTask): Task {
        val keyHolder = GeneratedKeyHolder()
        jdbcClient.sql(INSERT_TASK_SQL)
            .param("title", task.title)
            .param("description", task.description)
            .param("status", task.status.name)
            .param("createdAt", Timestamp.valueOf(task.createdAt))
            .param("updatedAt", Timestamp.valueOf(task.updatedAt))
            .update(keyHolder, "id")

        val generatedId = keyHolder.key?.toLong()
            ?: throw IllegalStateException("Failed to retrieve generated task id")

        return findById(generatedId)
            ?: throw IllegalStateException("Failed to load task with id=$generatedId after insert")
    }

    override fun findById(id: Long): Task? {
        return jdbcClient.sql("$BASE_SELECT where id = :id")
            .param("id", id)
            .query(taskRowMapper)
            .optional()
            .orElse(null)
    }

    override fun findAll(page: Int, size: Int, status: TaskStatus?): List<Task> {
        val offset = page.toLong() * size.toLong()
        val statement = jdbcClient.sql(buildFindAllSql(status))
            .param("limit", size)
            .param("offset", offset)

        if (status != null) {
            statement.param("status", status.name)
        }

        return statement.query(taskRowMapper).list()
    }

    override fun count(status: TaskStatus?): Long {
        val statement = jdbcClient.sql(if (status == null) COUNT_ALL_SQL else COUNT_BY_STATUS_SQL)
        if (status != null) {
            statement.param("status", status.name)
        }
        return statement.query(Long::class.java).single()
    }

    override fun updateStatus(id: Long, status: TaskStatus, updatedAt: LocalDateTime): Task? {
        val updatedRows = jdbcClient.sql(UPDATE_STATUS_SQL)
            .param("id", id)
            .param("status", status.name)
            .param("updatedAt", Timestamp.valueOf(updatedAt))
            .update()

        if (updatedRows == 0) {
            return null
        }

        return findById(id)
    }

    override fun deleteById(id: Long): Boolean {
        val deletedRows = jdbcClient.sql(DELETE_TASK_SQL)
            .param("id", id)
            .update()
        return deletedRows > 0
    }

    private fun buildFindAllSql(status: TaskStatus?): String {
        val filterClause = if (status == null) "" else " where status = :status"
        // TODO: Offset pagination matches the current page/size contract from the test task.
        // For large production datasets, consider a separate keyset/cursor-based API to avoid
        // deep OFFSET scans and keep pagination latency stable.
        return "$BASE_SELECT$filterClause order by created_at desc limit :limit offset :offset"
    }

    private companion object {
        private const val BASE_SELECT = """
            select id, title, description, status, created_at, updated_at
            from tasks
        """

        private const val INSERT_TASK_SQL = """
            insert into tasks (title, description, status, created_at, updated_at)
            values (:title, :description, :status, :createdAt, :updatedAt)
        """

        private const val COUNT_ALL_SQL = "select count(*) from tasks"

        private const val COUNT_BY_STATUS_SQL = "select count(*) from tasks where status = :status"

        private const val UPDATE_STATUS_SQL = """
            update tasks
            set status = :status,
                updated_at = :updatedAt
            where id = :id
        """

        private const val DELETE_TASK_SQL = "delete from tasks where id = :id"

        private val taskRowMapper = RowMapper<Task> { resultSet, _ ->
            Task(
                id = resultSet.getLong("id"),
                title = resultSet.getString("title"),
                description = resultSet.getString("description"),
                status = TaskStatus.valueOf(resultSet.getString("status")),
                createdAt = resultSet.getTimestamp("created_at").toLocalDateTime(),
                updatedAt = resultSet.getTimestamp("updated_at").toLocalDateTime()
            )
        }
    }
}
