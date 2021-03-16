package io.adven27.telegram.bots.mamot.db

import java.net.URI
import java.net.URISyntaxException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

interface Repo {
    fun insert(user: String?, data: String?)
    fun selectAll(): Map<String, String>
    fun select(user: String?): String?
    fun update(user: String?, data: String?)
    fun delete(user: String?)
    fun exists(user: String?, data: String?): Boolean
}

open class PGSQLRepo(private val databaseUrl: String, protected val table: String, protected val dataColumn: String) :
    Repo {
    private fun connect(): Connection? {
        var conn: Connection? = null
        try {
            val uri = URI(databaseUrl)
            val username = uri.userInfo.split(":").toTypedArray()[0]
            val password = uri.userInfo.split(":").toTypedArray()[1]
            val url = String.format(DB_URL_FORMAT, uri.host, uri.port, uri.path)
            conn = DriverManager.getConnection(url, username, password)
        } catch (e: SQLException) {
            println(e.message)
        } catch (e: URISyntaxException) {
            println(e.message)
        }
        return conn
    }

    override fun insert(name: String?, data: String?) {
        try {
            connect().use { conn ->
                conn!!.prepareStatement("INSERT INTO $table (username, $dataColumn) VALUES(?,?)")
                    .use { ps ->
                        ps.setString(1, name)
                        ps.setString(2, data)
                        ps.executeUpdate()
                    }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    override fun selectAll(): Map<String, String> {
        val result = HashMap<String, String>()
        try {
            connect().use { c ->
                c!!.createStatement().executeQuery("SELECT username, $dataColumn FROM $table").use { rs ->
                    while (rs.next()) {
                        result[rs.getString("username")] = rs.getString(dataColumn)
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
        return result
    }

    override fun select(user: String?): String {
        var result = ""
        try {
            connect().use { c ->
                c!!.prepareStatement("SELECT $dataColumn FROM $table WHERE username = ?").use { ps ->
                    ps.setString(1, user)
                    val resultSet = ps.executeQuery()
                    while (resultSet.next()) {
                        result = resultSet.getString(dataColumn)
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
        return result
    }

    override fun update(user: String?, data: String?) {
        try {
            connect().use { c ->
                c!!.prepareStatement("UPDATE $table SET $dataColumn  = ? WHERE username = ?").use { ps ->
                    ps.setString(1, data)
                    ps.setString(2, user)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    override fun delete(user: String?) {
        try {
            connect().use { c ->
                c!!.prepareStatement("DELETE FROM $table WHERE username = ?").use { ps ->
                    ps.setString(1, user)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    override fun exists(user: String?, data: String?): Boolean {
        try {
            connect().use { c ->
                c!!.prepareStatement("SELECT COUNT(1) FROM $table WHERE username = ? AND $dataColumn = ? ")
                    .use { ps ->
                        ps.setString(1, user)
                        ps.setString(2, data)
                        val rs = ps.executeQuery()
                        return rs.next() && isPositiveCount(rs)
                    }
            }
        } catch (e: SQLException) {
            println(e.message)
            return false
        }
    }

    fun createTable() {
        try {
            connect().use { c ->
                c!!.createStatement()
                    .execute("CREATE TABLE IF NOT EXISTS $table (username text PRIMARY KEY, $dataColumn text NOT NULL);")
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    fun dropTable() {
        try {
            connect().use { conn -> conn!!.createStatement().execute("DROP TABLE $table;") }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    @Throws(SQLException::class)
    private fun isPositiveCount(rs: ResultSet): Boolean = rs.getLong(COUNT_COLUMN_INDEX) > 0

    companion object {
        private const val DB_URL_FORMAT = "jdbc:postgresql://%s:%d%s?sslmode=require"
        private const val COUNT_COLUMN_INDEX = 1
    }

    init {
        createTable()
    }
}