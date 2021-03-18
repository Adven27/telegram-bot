package io.adven27.telegram.bots.watcher

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Service
interface ChatRepository : CrudRepository<Chat, Long> {
    fun findByChatId(chat: Long): Optional<Chat>
}

@Entity
@Table(name = "chats")
@EntityListeners(AuditingEntityListener::class)
@TypeDefs(
    TypeDef(name = "json", typeClass = JsonStringType::class),
    TypeDef(name = "jsonb", typeClass = JsonBinaryType::class),
)
data class Chat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var created: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updated: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var blocked: Boolean = false,

    @Column(nullable = false, unique = true)
    val chatId: Long,

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    @Basic(fetch = FetchType.EAGER)
    var data: ChatData
)

@Service
interface ScriptsRepository : CrudRepository<Script, Long> {
    fun findByPatternNotNull(): List<Script>
}

@Entity
@Table(name = "scripts")
data class Script(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var pattern: String? = null,
    var script: String
)