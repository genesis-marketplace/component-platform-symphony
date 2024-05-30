import com.symphony.bdk.gen.api.model.UserV2
import global.genesis.notify.NotifyServer
import global.genesis.symphony.gateway.SymphonyConnection
import global.genesis.symphony.gateway.SymphonyGateway
import kotlin.streams.asSequence

/**
 * System              : Genesis Business Library
 * Sub-System          : multi-pro-code-test Configuration
 * Version             : 1.0
 * Copyright           : (c) Genesis
 * Date                : 2022-03-18
 * Function : Provide request reply config for multi-pro-code-test.
 *
 * Modification History
 */
val notifyServer = inject<NotifyServer>()
val symphonyGateways = notifyServer.gateways
    .filter { it.value is SymphonyGateway }
    .map { it.key to it.value as SymphonyGateway }
    .toMap()
require(!symphonyGateways.isEmpty()) { "Symphony event handler API was initialised but no Symphony gateway was configured. " +
    "A symphony gateway must be configured in your *notify.kts GPAL file" }
val defaultGateway: SymphonyGateway = symphonyGateways.values.first()

fun getEmailFromId(connection: SymphonyConnection, id: Long): String? {
    val userFromId: List<UserV2> = connection.botClient.users().listUsersByIds(listOf(id))
    return if (userFromId.isEmpty()) {
        null
    } else {
        return userFromId[0].emailAddress
    }
}

requestReplies {
    requestReply<ListChannelMembersRequest, ListChannelMembersResponse>("LIST_MEMBERS_OF_CHANNEL") {
        replyList {request ->
            val connection = if (request.gatewayId != null) {
                val gateway = symphonyGateways[request.gatewayId]
                if (gateway == null) {
                    LOG.warn("${request.gatewayId} does not refer to a valid Symphony gateway, returning empty list for REQ_LIST_MEMBERS_OF_CHANNEL")
                    return@replyList emptyList<ListChannelMembersResponse>()
                }
                gateway.connection
            } else {
                defaultGateway.connection
            }

            connection.botClient
                .streams()
                .listRoomMembers(request.channelName)
                .stream()
                .asSequence()
                .map { ListChannelMembersResponse(getEmailFromId(connection, it.id!!), it.id!!) }
                .toList()
        }
    }
}
