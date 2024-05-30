package global.genesis.symphony.message.event

import global.genesis.gen.dao.enums.EntityIdType
import global.genesis.notify.message.event.RouteCreate

data class SymphonyByEmailRouteCreate(
    override val gatewayId: String,
    override val topicMatch: String,
    val entityId: String? = null,
    val entityIdType: EntityIdType
) : RouteCreate

data class SymphonyRoomRouteCreate(
    override val gatewayId: String,
    override val topicMatch: String,
    val roomId: String
) : RouteCreate
