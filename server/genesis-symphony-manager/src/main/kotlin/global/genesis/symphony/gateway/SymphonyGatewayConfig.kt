package global.genesis.symphony.gateway

import com.google.inject.Binder
import com.google.inject.Injector
import com.google.inject.Module
import global.genesis.notify.gateway.Gateway
import global.genesis.notify.message.request.RouteInfo
import global.genesis.notify.pal.GatewayConfig
import global.genesis.notify.utils.ConfigValidator
import global.genesis.pal.shared.inject

class SymphonyGatewayConfig(override val id: String) : GatewayConfig {
    var sessionAuthHost: String? = null
    var botUsername: String? = null
    var botPrivateKeyPath: String? = null
    var botPrivateKeyName: String? = null
    var botPrivateKeyFromDb: Boolean = false
    var appId: String? = null

    override fun validate(): List<String> {
        return ConfigValidator.validateRequiredFields(
            SymphonyGatewayConfig::class,
            this,
            "botPrivateKeyPath",
            "botPrivateKeyName",
            "appId",
            "botPrivateKeyFromDb"
        ) + validatePrivateKeyAccess()
    }

    override val routeInfoSet = setOf(
        RouteInfo(
            dataServerHandler = "ALL_SYMPHONY_ROOM_ROUTES",
            createEventHandler = "SYMPHONY_ROOM_ROUTE_CREATE",
            updateEventHandler = "SYMPHONY_ROOM_ROUTE_UPDATE",
            deleteEventHandler = "SYMPHONY_ROOM_ROUTE_DELETE",
            displayName = "Symphony Room Notification"
        ),
        RouteInfo(
            dataServerHandler = "ALL_SYMPHONY_USER_ROUTES",
            createEventHandler = "SYMPHONY_BY_EMAIL_ROUTE_CREATE",
            updateEventHandler = "SYMPHONY_BY_EMAIL_ROUTE_UPDATE",
            deleteEventHandler = "SYMPHONY_BY_EMAIL_ROUTE_DELETE",
            displayName = "Symphony User Notification"
        )
    )

    override fun build(injector: Injector): Gateway {
        val childInjector = injector.createChildInjector(object : Module {
            override fun configure(binder: Binder?) {
                binder?.bind(SymphonyGatewayConfig::class.java)?.toInstance(this@SymphonyGatewayConfig)
            }
        })
        return childInjector.inject<SymphonyGateway>()
    }

    private fun validatePrivateKeyAccess(): List<String> {
        val result = mutableListOf<String>()
        if (botPrivateKeyFromDb) {
            if (botPrivateKeyPath != null) {
                result.add("using privateKey from DB so botPrivateKeyPath should not be set")
            }
            if (botPrivateKeyName != null) {
                result.add("using privateKey from DB so botPrivateKeyName should not be set")
            }
        } else {
            if (botPrivateKeyPath == null) {
                result.add("botPrivateKeyPath is a required Parameter, when botPrivateKeyFromDb is not true")
            }
            if (botPrivateKeyName == null) {
                result.add("botPrivateKeyName is a required Parameter, when botPrivateKeyFromDb is not true")
            }
        }
        return result
    }
}
