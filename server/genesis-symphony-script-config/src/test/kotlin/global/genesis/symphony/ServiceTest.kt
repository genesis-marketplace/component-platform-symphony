package global.genesis.symphony

import global.genesis.commons.model.GenesisSet
import global.genesis.commons.standards.MessageType.MESSAGE_TYPE
import global.genesis.net.channel.GenesisChannel
import global.genesis.net.handler.MessageListener
import global.genesis.testsupport.AbstractGenesisTestSupport
import global.genesis.testsupport.EventResponse
import global.genesis.testsupport.GenesisTestConfig
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.has
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import java.time.Duration

class ServiceTest : AbstractGenesisTestSupport<EventResponse>(
    GenesisTestConfig {
        addPackageName("global.genesis.notify")
        addPackageName("global.genesis.eventhandler.pal")
        addPackageName("global.genesis.symphony")
        addPackageName("global.genesis.requestreply.pal")
        addPackageName("global.genesis.file.storage.provider")
        genesisHome = "/GenesisHome/"
        initialDataFile = "notify-data.csv"
        scriptFileName = "another-notify.kts,genesis-symphony-reqrep.kts"
        useTempClassloader = true
    }
) {

    override fun systemDefinition(): Map<String, Any> = mapOf(
        "IS_SCRIPT" to "true",
        "MESSAGE_CLIENT_PROCESS_NAME" to "GENESIS_ROUTER",
        "STORAGE_STRATEGY" to "LOCAL",
        "LOCAL_STORAGE_FOLDER" to "src/test/resources/genesisHome/site-specific/incoming"
    )

    @Test
    fun testReqRepListMembersOfChannel() {
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
        set.setString(MESSAGE_TYPE, "REQ_LIST_MEMBERS_OF_CHANNEL")
        set.setDirect("REQUEST.CHANNEL_NAME", "A_CHANNEL_NAME")

        messageClient.sendMessage(set)

        val response = await atMost Duration.ofSeconds(20) untilCallTo { responses.firstOrNull() } has {
            getArray<GenesisSet>("REPLY")?.size == 2
        }

        assert(response.getString(MESSAGE_TYPE) == "REP_LIST_MEMBERS_OF_CHANNEL")
    }
}
