package global.genesis.symphony

import global.genesis.commons.model.GenesisSet
import global.genesis.commons.standards.MessageType
import global.genesis.gen.dao.NotifyRouteAudit
import global.genesis.message.core.event.EventReply
import global.genesis.net.channel.GenesisChannel
import global.genesis.net.handler.MessageListener
import global.genesis.symphony.message.event.AddUserToChannel
import global.genesis.symphony.message.event.CreateChannel
import global.genesis.testsupport.AbstractGenesisTestSupport
import global.genesis.testsupport.EventResponse
import global.genesis.testsupport.GenesisTestConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.has
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import java.time.Duration

class SymphonyTest : AbstractGenesisTestSupport<EventResponse>(
    GenesisTestConfig {
        addPackageName("global.genesis.notify")
        addPackageName("global.genesis.eventhandler.pal")
        addPackageName("global.genesis.symphony")
        addPackageName("global.genesis.requestreply.pal")
        addPackageName("global.genesis.file.storage.provider")
        genesisHome = "/GenesisHome/"
        initialDataFile = "notify-data.csv"
        scriptFileName = "another-notify.kts,genesis-symphony-eventhandler.kts,genesis-symphony-reqrep.kts"
        useTempClassloader = true
        parser = EventResponse
    }
) {

    override fun systemDefinition(): Map<String, Any> {
        return mutableMapOf(
            "IS_SCRIPT" to "true",
            "MESSAGE_CLIENT_PROCESS_NAME" to "UNIT_TEST_PROCESS",
            "DOCUMENT_STORE_BASEDIR" to "src/test/resources/genesisHome/incoming",
            "SYMPHONY_ENABLED_FOR_TESTING" to "true",
            "STORAGE_STRATEGY" to "LOCAL",
            "LOCAL_STORAGE_FOLDER" to "LOCAL_STORAGE"
        )
    }

    @Test
    fun testListChannelMembers() {
        val responses = mutableListOf<GenesisSet>()

        messageClient.handler.addListener(
            MessageListener { set: GenesisSet?, _: GenesisChannel? ->
                println(set)
                if (set != null) {
                    responses.add(set)
                }
            }
        )

        val set = GenesisSet()
        set.setString(MessageType.MESSAGE_TYPE, "REQ_LIST_MEMBERS_OF_CHANNEL")
        set.setDirect("REQUEST.CHANNEL_NAME", "ExatsRJgLr942Urv5u8Uj3___oCahRE7dA")

        messageClient.sendMessage(set)

        await atMost Duration.ofSeconds(20) untilCallTo { responses.firstOrNull() } has {
            getArray<GenesisSet>("REPLY")?.size == 2
        }
    }

    @Test
    fun testCreateChannel(): Unit = runBlocking {
        val createChannelEvent = CreateChannel(
            topic = "TESTING8",
            channelName = "Yes, Yet Another This is a Testing ROOM Name 4"
        )

        sendEvent(
            createChannelEvent,
            userName = "JohnDoe",
            messageType = "EVENT_GATEWAY_CREATE_CHANNEL"
        ).assertedCast<EventReply.EventAck>()
        val notifyRouteAuditRecords = entityDb.getBulk<NotifyRouteAudit>().toList()
        assert(notifyRouteAuditRecords.size == 1)
    }

    @Test
    fun testAddMemberToChannel(): Unit = runBlocking {
        sendEvent(
            AddUserToChannel("Yes, Yet Another This is a Testing ROOM Name 4", "joseph.adam@genesis.global"),
            userName = "JohnDoe",
            messageType = "EVENT_GATEWAY_ADD_MEMBER_TO_CHANNEL"
        ).assertedCast<EventReply.EventAck>()
    }
}
