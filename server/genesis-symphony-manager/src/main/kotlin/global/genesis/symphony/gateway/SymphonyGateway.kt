package global.genesis.symphony.gateway

import com.google.inject.Inject
import com.symphony.bdk.core.SymphonyBdk
import com.symphony.bdk.core.auth.AuthSession
import com.symphony.bdk.core.service.message.MessageService
import com.symphony.bdk.core.service.message.model.Message
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.gen.dao.Notify
import global.genesis.gen.dao.enums.NotifySeverity
import global.genesis.gen.view.entity.SymphonyRoomRoute
import global.genesis.gen.view.entity.SymphonyUserRoute
import global.genesis.notify.documents.FileStorageClientWrapper
import global.genesis.notify.gateway.Gateway
import global.genesis.notify.gateway.common.RouteUtils.getMatchedTopicRoutes
import global.genesis.notify.router.NotifyRouteCache
import global.genesis.notify.router.NotifyRoutingData
import global.genesis.notify.router.UserNameResolver
import global.genesis.notify.router.UserPermissionData
import global.genesis.notify.router.UserRouteData
import global.genesis.session.UserEmailCache
import org.apache.commons.lang.StringEscapeUtils.escapeHtml
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

class SymphonyGateway @Inject constructor(
    private val formatter: SymphonyMessageFormatter,
    private val fileStorageClientWrapper: FileStorageClientWrapper,
    private val emailCache: UserEmailCache,
    db: AsyncEntityDb,
    private val userNameResolver: UserNameResolver,
    symphonyConnectionFactory: SymphonyConnectionFactory,
    private val config: SymphonyGatewayConfig
) : Gateway {
    val connection: SymphonyConnection = symphonyConnectionFactory.create(config)

    private val oboAuthSessions = ConcurrentHashMap<String, AuthSession>()

    private val symphonyUserRouteCache = NotifyRouteCache(
        name = "SymphonyUserRouteCache",
        subscriber = db.bulkSubscribe(
            index = SymphonyUserRoute.ById,
            backwardJoins = true
        ),
        keyFunction = { it.notifyRouteId }
    )
    private val symphonyRoomRouteCache = NotifyRouteCache(
        name = "SymphonyRoomRouteCache",
        subscriber = db.bulkSubscribe(
            index = SymphonyRoomRoute.ById,
            backwardJoins = true
        ),
        keyFunction = { it.notifyRouteId }
    )

    override suspend fun sendViaTopic(message: Notify, routeData: NotifyRoutingData.Topic) {
        val matchedRoutes = routeData.getMatchedTopicRoutes(config.id)
        val userRoutes: Set<SymphonyUserRoute> = symphonyUserRouteCache.getNotifyRoutes(routesToFind = matchedRoutes)
        val roomRoutes: Set<SymphonyRoomRoute> = symphonyRoomRouteCache.getNotifyRoutes(routesToFind = matchedRoutes)

        val userNames = userNameResolver.getMatchedGatewayUsers(
            message,
            userRoutes,
            { it.toUserRouteData() },
            { it.toUserPermissionData() }
        )
        userNames.forEach { sendToUser(it, message) }

        sendToRooms(message, roomRoutes)

        val matchedRouteIds = userRoutes.map { it.notifyRouteId }.toSet() +
            roomRoutes.map { it.notifyRouteId }.toSet()
        val unknownRouteIds = matchedRoutes.map { it.notifyRouteId }.toSet() - matchedRouteIds
        unknownRouteIds.forEach {
            LOG.warn("Unable to find compatible routing data for matched route $it")
        }
    }

    override suspend fun sendDirect(message: Notify, routeData: NotifyRoutingData.Direct) {
        LOG.trace("Direct routing is not currently implemented for Symphony gateways, ignoring message")
    }

    private fun sendToUser(userName: String, message: Notify) {
        try {
            val botClient = connection.botClient

            val email = emailCache.userEmailAddress(userName)
            if (email != null) {
                val users = botClient.users().listUsersByEmails(listOf(email))
                for (user in users) {
                    val imStream = botClient.streams().create(user.id)
                    val outboundMessage = Message.builder().content(formatter.format(message, emptySet())).build()
                    if (imStream.id != null) botClient.messages().send(imStream.id!!, outboundMessage)
                    LOG.info("Message sent to symphony for email $email for $userName")
                }
            } else {
                LOG.warn("Could not send symphony message, could not obtain email address for user: $userName")
            }
        } catch (t: Throwable) {
            LOG.error("Could not send symphony message for $userName, connection problem", t)
        }
    }

    private suspend fun sendToRooms(
        message: Notify,
        roomRoutes: Set<SymphonyRoomRoute>
    ) {
        val botClient = connection.botClient

        val messageBuilder = Message.builder().content(formatter.format(message))
        message.documentId?.let {
            val fileContents = fileStorageClientWrapper.getFileContents(it)
            if (fileContents != null) {
                messageBuilder.addAttachment(ByteArrayInputStream(fileContents.fileContent), fileContents.fileName)
            }
        }
        val outboundMessage = messageBuilder.build()

        val roomStreamIds = roomRoutes.map { SymphonyStreamId(it.roomId) }
        roomStreamIds.forEach { roomStreamId ->
            sendToRoom(botClient, roomStreamId, outboundMessage, message.sender)
        }
    }

    private fun sendToRoom(
        botClient: SymphonyBdk,
        roomStreamId: SymphonyStreamId,
        outboundMessage: Message,
        sender: String?
    ) {
        try {
            val messages: MessageService = botClient.messages()
            val oboAuthSession = getOboAuthSession(sender)
            if (oboAuthSession != null) {
                messages.obo(oboAuthSession).send(
                    roomStreamId.symphonyStreamId,
                    outboundMessage
                )
            } else {
                messages.send(
                    roomStreamId.symphonyStreamId,
                    outboundMessage
                )
            }
        } catch (e: RuntimeException) {
            val content = Message.builder().content(
                formatter.format(
                    Notify {
                        header = "Gateway Error attempting to Send a message"
                        body =
                            "Error was '${escapeHtml(e.message)}'<br/>You are receiving this instead, see Gateway Logs for details"
                        NotifySeverity.Warning
                    },
                    emptySet()
                )
            ).build()

            botClient.messages().send(
                roomStreamId.symphonyStreamId,
                content
            )
        }
    }

    private fun getOboAuthSession(userName: String?): AuthSession? {
        if (userName == null || connection.botClient.config().app.appId == null) return null
        val email = emailCache.userEmailAddress(userName) ?: return null
        return oboAuthSessions.computeIfAbsent(email) { connection.botClient.obo(email) }
    }

    private fun SymphonyUserRoute.toUserRouteData(): UserRouteData {
        return UserRouteData(
            routeId = notifyRouteId,
            entityIdType = entityIdType,
            entityId = entityId,
            excludeSender = excludeSender
        )
    }

    private fun SymphonyUserRoute.toUserPermissionData(): UserPermissionData {
        return UserPermissionData(
            rightCode = rightCode,
            authCacheName = authCacheName
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SymphonyGateway::class.java)
    }
}
