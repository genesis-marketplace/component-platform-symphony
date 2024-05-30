package global.genesis.notify.pal.extn

import global.genesis.notify.pal.NotifyFileReader
import global.genesis.symphony.gateway.SymphonyGatewayConfig
import global.genesis.testsupport.AbstractGenesisTestSupport
import global.genesis.testsupport.EventResponse
import global.genesis.testsupport.GenesisTestConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NotifyFileReaderTest : AbstractGenesisTestSupport<EventResponse>(
    GenesisTestConfig {
        packageNames = mutableListOf("global.genesis.symphony", "global.genesis.notify", "global.genesis.file.storage.provider")
        genesisHome = "/GenesisHome/"
        scriptFileName = "another-notify.kts"
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
    fun test() {
        val reader = bootstrap.injector.getInstance(NotifyFileReader::class.java)

        val symphonyConfig = reader.get().gatewayConfigs.configById["symphony1"] as SymphonyGatewayConfig
        assertEquals("abotuser@genesis.global", symphonyConfig.botUsername)
    }
}
