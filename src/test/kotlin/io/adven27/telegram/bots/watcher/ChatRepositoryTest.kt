package io.adven27.telegram.bots.watcher

import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT
import com.github.springtestdbunit.bean.DatabaseConfigBean
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.dbunit.dataset.datatype.AbstractDataType
import org.dbunit.dataset.datatype.DataType
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import javax.sql.DataSource

@RunWith(SpringRunner::class)
@Import(DBUnitCfg::class)
@DataJpaTest(properties = ["spring.test.database.replace=none", "spring.jpa.show-sql=true"])
@TestExecutionListeners(value = [DependencyInjectionTestExecutionListener::class, DbUnitTestExecutionListener::class])
@DatabaseSetup("/repo/empty.xml")
@EnableJpaAuditing
class ChatRepositoryTest {
    @Autowired
    private lateinit var sut: ChatRepository

    @Test
    fun `empty chats`() {
        assertThat(sut.findAll()).isEmpty()
    }

    @Test
    @DatabaseSetup("/repo/chats-3.xml")
    fun `not empty chats`() {
        assertThat(sut.findAll())
            .hasSize(3)
            .extracting("chatId", "data", "blocked")
            .containsExactly(
                tuple(11L, ChatData(WishList(listOf(Item("http")))), false),
                tuple(22L, ChatData(WishList(listOf(Item("http1"), Item("http2")))), false),
                tuple(33L, ChatData(), true),
            )
    }

    @Test
    @DatabaseSetup("/repo/chats-3.xml")
    fun `find by chatId`() {
        assertThat(sut.findByChatId(22)).get()
            .extracting("chatId", "data", "blocked")
            .contains(22L, ChatData(WishList(listOf(Item("http1"), Item("http2")))), false)
    }

    @Test
    @ExpectedDatabase("/repo/expected-one.xml", table = "chats", assertionMode = NON_STRICT)
    fun save() {
        sut.save(Chat(chatId = 123L, data = ChatData(WishList(listOf(Item(url = "http"))))))
    }
}

@Configuration
class DBUnitCfg {
    @Bean("dbUnitDatabaseConnection")
    fun dbUnitDatabaseConnection(dataSource: DataSource): DatabaseDataSourceConnectionFactoryBean =
        DatabaseDataSourceConnectionFactoryBean(dataSource).apply {
            setDatabaseConfig(
                DatabaseConfigBean().apply {
                    datatypeFactory = CustomPostgresqlDataTypeFactory()
                }
            )
        }
}

/**
 * references:
 * https://sourceforge.net/p/dbunit/feature-requests/188/
 * https://stackoverflow.com/a/55839637
 */
class CustomPostgresqlDataTypeFactory : PostgresqlDataTypeFactory() {
    override fun createDataType(sqlType: Int, sqlTypeName: String): DataType = if (sqlTypeName == "jsonb") {
        JsonbDataType()
    } else {
        super.createDataType(sqlType, sqlTypeName)
    }

    class JsonbDataType : AbstractDataType("jsonb", Types.OTHER, String::class.java, false) {
        override fun typeCast(obj: Any): Any = obj.toString()

        override fun getSqlValue(column: Int, resultSet: ResultSet): Any = resultSet.getString(column)

        override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) = statement.setObject(
            column,
            PGobject().apply {
                type = "json"
                this.value = value.toString()
            }
        )
    }
}
