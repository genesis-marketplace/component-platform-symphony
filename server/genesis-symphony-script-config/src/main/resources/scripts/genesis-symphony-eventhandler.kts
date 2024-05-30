import com.symphony.bdk.core.SymphonyBdk
import com.symphony.bdk.gen.api.model.UserV2
import com.symphony.bdk.gen.api.model.V3RoomAttributes
import global.genesis.notify.NotifyServer
import global.genesis.symphony.gateway.SymphonyGateway

/**
 * System              : Genesis Business Library
 * Sub-System          : multi-pro-code-test Configuration
 * Version             : 1.0
 * Copyright           : (c) Genesis
 * Date                : 2022-03-18
 * Function : Provide event handler config for multi-pro-code-test.
 *
 * Modification History
 */
fun getUserFromEmail(botClient: SymphonyBdk, email: String?): Long? {
    if (email == null) return null
    val userFromEmail: List<UserV2> = botClient.users().listUsersByEmails(listOf(email))
    return if (userFromEmail.isEmpty()) {
        null
    } else {
        userFromEmail[0].id
    }
}

eventHandler {
    eventHandler<SymphonyRoomRouteCreate>(name = "SYMPHONY_ROOM_ROUTE_CREATE", transactional = true) {
        onCommit { event ->
            val details = event.details
            val notifyRoute = NotifyRoute {
                this.gatewayId = details.gatewayId
                this.topicMatch = details.topicMatch
            }
            val insertResult = entityDb.insert(notifyRoute)
            val symphonyRoomNotifyRoute = SymphonyRoomNotifyRouteExt {
                this.notifyRouteId = insertResult.record.notifyRouteId
                this.roomId = details.roomId
            }
            entityDb.insert(symphonyRoomNotifyRoute)
            ack()
        }
    }

    eventHandler<SymphonyRoomRoute>(name = "SYMPHONY_ROOM_ROUTE_UPDATE", transactional = true) {
        onCommit { event ->
            val details = event.details
            val notifyRoute = NotifyRoute {
                this.notifyRouteId = details.notifyRouteId
                this.gatewayId = details.gatewayId
                this.topicMatch = details.topicMatch
            }
            val symphonyRoomNotifyRoute = SymphonyRoomNotifyRouteExt {
                this.notifyRouteId = details.notifyRouteId
                this.roomId = details.roomId
            }
            entityDb.modify(notifyRoute)
            entityDb.modify(symphonyRoomNotifyRoute)
            ack()
        }
    }

    eventHandler<SymphonyRoomRoute.ById>(name = "SYMPHONY_ROOM_ROUTE_DELETE", transactional = true) {
        onCommit { event ->
            val details = event.details
            val notifyRoute = NotifyRoute.ById(details.notifyRouteId)
            val screenNotifyRoute = SymphonyRoomNotifyRouteExt.ByNotifyRouteId(details.notifyRouteId)
            entityDb.delete(notifyRoute)
            entityDb.delete(screenNotifyRoute)
            ack()
        }
    }

    eventHandler<SymphonyByEmailRouteCreate>(name = "SYMPHONY_BY_EMAIL_ROUTE_CREATE", transactional = true) {
        onCommit { event ->
            val details = event.details
            val notifyRoute = NotifyRoute {
                this.gatewayId = details.gatewayId
                this.topicMatch = details.topicMatch
            }
            val insertResult = entityDb.insert(notifyRoute)
            val symphonyByEmailRoute = SymphonyByUserEmailNotifyRouteExt {
                this.notifyRouteId = insertResult.record.notifyRouteId
                this.entityId = details.entityId
                this.entityIdType = details.entityIdType
            }
            entityDb.insert(symphonyByEmailRoute)
            ack()
        }
    }

    eventHandler<SymphonyUserRoute>(name = "SYMPHONY_BY_EMAIL_ROUTE_UPDATE", transactional = true) {
        onCommit { event ->
            val details = event.details
            val notifyRoute = NotifyRoute {
                this.notifyRouteId = details.notifyRouteId
                this.gatewayId = details.gatewayId
                this.topicMatch = details.topicMatch
            }
            val symphonyByEmailRoute = SymphonyByUserEmailNotifyRouteExt {
                this.notifyRouteId = details.notifyRouteId
                this.entityId = details.entityId
                this.entityIdType = details.entityIdType
            }
            entityDb.modify(notifyRoute)
            entityDb.modify(symphonyByEmailRoute)
            ack()
        }
    }

    eventHandler<SymphonyUserRoute.ById>(name = "SYMPHONY_BY_EMAIL_ROUTE_DELETE", transactional = true) {
        onCommit { event ->
            val details = event.details
            val notifyRoute = NotifyRoute.ById(details.notifyRouteId)
            val symphonyByEmailRoute = SymphonyByUserEmailNotifyRouteExt.ByNotifyRouteId(details.notifyRouteId)
            entityDb.delete(notifyRoute)
            entityDb.delete(symphonyByEmailRoute)
            ack()
        }
    }

    val notifyServer = inject<NotifyServer>()
    val stringGatewayPairs = notifyServer.gateways.filter { it.value is SymphonyGateway }.toList()
    require(!stringGatewayPairs.isEmpty()) { "Symphony event handler API was initialised but no Symphony gateway was configured. " +
        "A symphony gateway must be configured in your *notify.kts GPAL file" }
    val stringGatewayPair = stringGatewayPairs.first()
    val defaultGatewayId = stringGatewayPair.first
    val defaultGateway = stringGatewayPair.second as SymphonyGateway

    eventHandler<ActionOnChannel>(name = "GATEWAY_ACTION_ON_CHANNEL", transactional = true) {
        onCommit { event ->
            defaultGateway.connection
                .botClient.streams()
                .setRoomActive(event.details.roomId, event.details.activate)
            ack()
        }
    }

    eventHandler<AddUserToChannel>(name = "GATEWAY_ADD_MEMBER_TO_CHANNEL", transactional = true) {
        onCommit { event ->
            val roomStreamId = event.details.channelName
            val userIdEmail = event.details.userId
            val botClient = defaultGateway.connection.botClient
            val symphonyUserId = getUserFromEmail(botClient, userIdEmail)
            if (symphonyUserId != null) {
                try {
                    botClient.connections().getConnection(symphonyUserId) // check connection works
                    LOG.info("User $userIdEmail appears connected, adding to room: $roomStreamId")
                    botClient.streams().addMemberToRoom(symphonyUserId, roomStreamId)
                    ack()
                } catch (e: Exception) {
                    val responseText =
                        "Adding member to room $roomStreamId rejected for user $userIdEmail, a request will be sent to the user, which they need to accept, then retry this operation"
                    LOG.info(responseText, e)
                    botClient.connections().createConnection(symphonyUserId)
                    nack(responseText)
                }
            } else {
                nack("Unable to find user in symphony given email $userIdEmail")
            }
        }
    }

    eventHandler<RemoveUserFromChannel>(name = "GATEWAY_REMOVE_MEMBER_FROM_CHANNEL", transactional = true) {
        onCommit { event ->
            val roomStreamId = event.details.channelName
            val userIdEmail = event.details.userId
            val botClient = defaultGateway.connection.botClient
            val symphonyUserId = getUserFromEmail(botClient, userIdEmail)
            if (symphonyUserId != null) {
                botClient.streams().removeMemberFromRoom(symphonyUserId, roomStreamId)
                ack()
            } else {
                nack("Unable to find user in symphony given email $userIdEmail")
            }
        }
    }

    eventHandler<CreateChannel>(name = "GATEWAY_CREATE_CHANNEL", transactional = true) {
        onCommit { event ->
            val createChannel = event.details
            val topic = createChannel.topic
            val roomName = createChannel.channelName

            val room = V3RoomAttributes()
            room.name = roomName
            room.description = roomName
            room.public = createChannel.public
            room.crossPod = createChannel.external
            room.multiLateralRoom = createChannel.multilateral
            room.discoverable = createChannel.discoverable
            room.viewHistory = createChannel.viewHistory
            room.membersCanInvite = createChannel.membersCanInvite

            val botClient = defaultGateway.connection.botClient
            val roomInfo = botClient.streams().create(room)

            val gatewayId = createChannel.gatewayId ?: defaultGatewayId

            val streamId = roomInfo.roomSystemInfo!!.id!!

            val notifyRoute = NotifyRoute {
                this.topicMatch = topic
                this.gatewayId = gatewayId
            }
            val insertResult = entityDb.insert(notifyRoute)
            val symphonyRoomRoute = SymphonyRoomNotifyRouteExt {
                this.notifyRouteId = insertResult.record.notifyRouteId
                this.roomId = streamId
            }
            entityDb.insert(symphonyRoomRoute)
            ack(listOf(mapOf("GATEWAY_VALUE" to streamId)))
        }
    }
}
