package global.genesis.notify.pal.extn

import global.genesis.notify.pal.GatewaysBuilder
import global.genesis.symphony.gateway.SymphonyGatewayConfig

fun GatewaysBuilder.symphony(id: String, init: SymphonyGatewayConfig.() -> Unit) {
    val symphonyConfig = SymphonyGatewayConfig(id)
    symphonyConfig.init()
    registerGateway(id, symphonyConfig)
}
