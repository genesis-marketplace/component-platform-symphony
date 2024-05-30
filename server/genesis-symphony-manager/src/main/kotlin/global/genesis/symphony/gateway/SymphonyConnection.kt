package global.genesis.symphony.gateway

import com.symphony.bdk.core.SymphonyBdk
import com.symphony.bdk.core.config.model.BdkConfig
import global.genesis.commons.config.GenesisConfigurationException
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.gen.dao.System
import kotlinx.coroutines.runBlocking
import java.io.File

class SymphonyConnection(
    private val symphonyConfig: SymphonyGatewayConfig,
    private val db: AsyncEntityDb,
    private val runtimeFolder: String
) {

    val botClient by lazy {

        val config = BdkConfig()
        config.host = symphonyConfig.sessionAuthHost
        config.app.appId = symphonyConfig.appId

        val key =
            if (symphonyConfig.botPrivateKeyFromDb) {
                createPrivateKeyFileFromDb().toString()
            } else {
                "${symphonyConfig.botPrivateKeyPath}${symphonyConfig.botPrivateKeyName}"
            }

        config.app.privateKey.path = key
        config.bot.username = symphonyConfig.botUsername
        config.bot.privateKey.path = key
        SymphonyBdk(config)
    }

    private fun createPrivateKeyFileFromDb(): File = runBlocking {
        val system: System? = db.get(System.ByKey("SymphonyRsaKey"))
        val privateKeyContents = system?.systemValue ?: throw GenesisConfigurationException("Could not locate 'SymphonyRsaKey' from DB 'SYSTEM' Table")
        val path = File("$runtimeFolder/symphony")
        path.mkdirs()
        val privateKeyFile = File(path, "file.pem")
        privateKeyFile.writeText(privateKeyContents)
        privateKeyFile
    }
}
