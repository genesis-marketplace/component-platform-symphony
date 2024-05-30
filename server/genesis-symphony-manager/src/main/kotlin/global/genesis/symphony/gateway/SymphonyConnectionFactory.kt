package global.genesis.symphony.gateway

import global.genesis.commons.annotation.ConditionalOnMissingClass
import global.genesis.commons.annotation.Module
import global.genesis.commons.standards.GenesisPaths
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import javax.inject.Inject
import javax.inject.Named

interface SymphonyConnectionFactory {
    fun create(config: SymphonyGatewayConfig): SymphonyConnection
}

@Module
@ConditionalOnMissingClass(SymphonyConnectionFactory::class)
class SymphonyConnectionFactoryDefault @Inject constructor(
    private val db: AsyncEntityDb,
    @Named(GenesisPaths.RUNTIME) val runtimeFolder: String
) : SymphonyConnectionFactory {
    override fun create(config: SymphonyGatewayConfig): SymphonyConnection {
        return SymphonyConnection(config, db, runtimeFolder)
    }
}
