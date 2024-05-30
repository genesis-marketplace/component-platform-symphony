// package global.genesis.notify.gateway
//
// import com.symphony.bdk.core.service.datafeed.RealTimeEventListener
// import com.symphony.bdk.gen.api.model.V4Initiator
// import com.symphony.bdk.gen.api.model.V4Message
// import com.symphony.bdk.gen.api.model.V4MessageSent
// import global.genesis.clustersupport.service.ServiceDiscovery
// import global.genesis.commons.model.GenesisSet
// import global.genesis.db.rx.AbstractEntityBulkTableSubscriber
// import global.genesis.db.rx.entity.multi.AsyncEntityDb
// import global.genesis.gen.dao.Gateway
// import global.genesis.gen.dao.Notify
// import global.genesis.gen.dao.NotifyRoute
// import global.genesis.gen.dao.description.GatewayDescription
// import global.genesis.gen.dao.enums.GatewayType
// import global.genesis.gen.dao.enums.NotifySeverity
// import global.genesis.notify.NotifyServer
// import global.genesis.notify.gateway.symphony.SymphonyConnection
// import global.genesis.notify.gateway.symphony.SymphonyStreamId
// import global.genesis.notify.documents.Document
// import global.genesis.notify.documents.DocumentStore
// import global.genesis.session.UserEmailCache
// import kotlinx.coroutines.runBlocking
// import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
// import org.slf4j.LoggerFactory
// import java.text.SimpleDateFormat
// import java.util.Base64
// import java.util.Date
// import java.util.concurrent.ConcurrentHashMap
// import java.util.concurrent.Executors
// import java.util.concurrent.TimeUnit
// import java.util.concurrent.TimeoutException
//
// class SymphonyGatewayIncoming(
//     private val connectionId: String,
//     private val entityDb: AsyncEntityDb,
//     private val connection: SymphonyConnection,
//     private val documentStore: DocumentStore,
//     private val emailCache: UserEmailCache,
//     private val notifyServer: NotifyServer,
//     private val serviceDiscovery: ServiceDiscovery,
// ) : AbstractEntityBulkTableSubscriber<Gateway>(entityDb, GatewayDescription) {
//
//     private val topicByStreamId = ConcurrentHashMap<SymphonyStreamId, String>()
//     private val eventByStreamId = ConcurrentHashMap<SymphonyStreamId, String>()
//
//     private val executors = Executors.newFixedThreadPool(1)
//     private val executorsDataFeed = Executors.newFixedThreadPool(1)
//
//     @Volatile
//     private var listening = false
//
//     override fun onPrime(record: Gateway) {
//         onInsert(record)
//     }
//
//     override fun onInsert(record: Gateway) {
//         getIncomingSymphonyGateway(record)?.let {
//             startListeningFeed()
//             when (it.gatewayType) {
//                 GatewayType.SymphonyRoomReqRep -> {
//                     eventByStreamId[SymphonyStreamId(it.getGatewayValue())] = record.incomingTopic!!
//                 }
//
//                 GatewayType.SymphonyRoom -> {
//                     topicByStreamId[SymphonyStreamId(it.getGatewayValue())] = record.incomingTopic!!
//                 }
//
//                 else -> IllegalStateException("Unexpected gateway type ${record.gatewayType} when trying to insert Symphony gateway")
//             }
//         }
//     }
//
//     override fun onDelete(record: Gateway) {
//         getIncomingSymphonyGateway(record)?.let {
//             when (it.gatewayType) {
//                 GatewayType.SymphonyRoomReqRep -> {
//                     eventByStreamId.remove(SymphonyStreamId(it.getGatewayValue()))
//                 }
//
//                 GatewayType.SymphonyRoom -> {
//                     topicByStreamId.remove(SymphonyStreamId(it.getGatewayValue()))
//                 }
//
//                 else -> IllegalStateException("Unexpected gateway type ${record.gatewayType} when trying to insert Symphony gateway")
//             }
//         }
//     }
//
//     override fun onModify(
//         newRecord: Gateway,
//         oldRecord: Gateway,
//         modifiedFields: List<String>,
//     ) {
//         onDelete(oldRecord)
//         onInsert(newRecord)
//     }
//
//     private fun onRoomMessage(event: V4MessageSent) {
//
//         val message = event.message
//
//         if (message == null) {
//             LOG.warn("Null Message Received")
//             return
//         }
//         LOG.info(
//             "SymphonyRoomGateway received inbound messageId: ${message.messageId}"
//         )
//
//         val streamId = message.stream?.streamId
//         val incomingTopic = if (streamId == null) null else topicByStreamId[SymphonyStreamId(streamId)]
//         val incomingEventHandler = if (streamId == null) null else eventByStreamId[SymphonyStreamId(streamId)]
//
//         if (incomingTopic == null && incomingEventHandler == null) {
//             LOG.warn("Ignoring messageId: ${message.messageId} as it streamId: $streamId does not have an associated input topic or eventHandler defined")
//             return
//         }
//         val str = if (incomingTopic != null) "topic $incomingTopic" else "eventHandler $incomingEventHandler"
//         LOG.debug("Processing messageId: ${message.messageId} will send to $str")
//
//         val attachments = message.attachments ?: emptyList()
//
//         val storeId = attachments.ifNotEmpty {
//             val fileAttachment = attachments[0]
//             documentStore.storeDocumentsGroup(
//                 Document(
//                     Base64.getDecoder().decode(
//                         connection.botClient.messages()
//                             .getAttachment(streamId!!, message.messageId!!, fileAttachment.id!!)
//                     ),
//                     fileAttachment.name
//                 )
//             )
//         }
//         if (storeId != null) {
//             LOG.info("Storing attachment to $storeId")
//         }
//
//         val body = message.message!!
//         val senderUserName = emailCache.userNameByEmailAddress(message.user!!.email!!)
//         // todo should be moved gateway agnostic
//         if (incomingEventHandler != null) {
//             executors.submit { processEventHandler(message, incomingEventHandler, body, senderUserName) }
//         } else {
//             val incomingTopic =
//                 requireNotNull(incomingTopic) { "Expected incoming topic to not be null for message with ID ${message.messageId}" }
//             val notifyMessage = Notify {
//                 sender = senderUserName
//                 header = "Incoming: SymphonyRoomGateway: ${message.stream!!.roomName!!}"
//                 this.body = body
//                 notifySeverity = NotifySeverity.Information
//                 documentId = storeId
//                 topic = incomingTopic
//             }
//             publish(notifyMessage)
//         }
//     }
//
//     private fun Gateway.getGatewayValue() =
//         requireNotNull(this.gatewayValue) { "Expected gatewayValue to not be null for gateway type ${this.gatewayType}" }
//
//     private fun processEventHandler(
//         inboundMessage: V4Message,
//         eventHandler: String,
//         messageBody: String,
//         senderUserName: String?,
//     ) {
//
//         val df = SimpleDateFormat("dd MMM yyyy @ HH:mm")
//         val replyString = """<card accent="tempo-bg-color--blue">
//         <header>
//         <div style="padding-left: 10px;"><span style="font-size:12px"><span class="tempo-text-color--secondary">In Reply to:
//         <br/><mention uid="${inboundMessage.user!!.userId!!}"/> ${df.format(Date(inboundMessage.timestamp!!))}
//         <br/>${inboundMessage.message!!}</span>
//         </span>
//         </div>
//         </header>
//         </card>"""
//
//         val (processName, feature) = eventHandler.split(":")
//         // FIXME What happens if we can't connect?
//         val genesisMessageClient = serviceDiscovery.resolveClient(processName)!!
//
//         val request = GenesisSet()
//         request.setString("MESSAGE_TYPE", feature)
//         request.setString("USER_NAME", senderUserName)
//         request.setDirect("DETAILS.BODY", messageBody)
//
//         val notifyMessage: Notify =
//             try {
//                 val response = genesisMessageClient.sendReqRep(request, 1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES)
//                 val responseType = response.getString("DETAILS.TYPE")
//
//                 Notify {
//                     header = ""
//                     body = replyString + response.getString(
//                         "DETAILS.BODY",
//                         "Missing response from server<br/>Please contact sysadmin@genesis.global"
//                     )
//                     notifySeverity = NotifySeverity.Information
//                 }
//             } catch (e: Exception) {
//                 val sb = StringBuilder()
//                 when (e) {
//
//                     is TimeoutException -> {
//                         sb.append("Unable to connect to Service to make request, timed out</br>")
//                     }
//
//                     else -> {
//                         sb.append("Unable to connect to Service to make request</br>")
//                     }
//                 }
//                 sb.append("action was not successful</br>")
//                 sb.append("Please contact sysadmin@genesis.global</br>")
//                 Notify {
//                     header = ""
//                     body = replyString + sb.toString()
//                     NotifySeverity.Critical
//                 }
//             }
//         // TODO this is a bit ass, it seems like this is a bit of an over-engineered solution
//         // anyway, why not just create a notification from the response and let it go through
//         // the normal routing system?
//         notifyServer.getMatchedGatewayImpliedResponse(eventHandler)
//             ?.let {
//                 notifyServer.send(
//                     setOf(
//                         NotifyRoute {
//                             gatewayId = it
//                         }
//                     ),
//                     notifyMessage
//                 )
//             }
//     }
//
//     private fun getIncomingSymphonyGateway(record: Gateway): Gateway? {
//         return when (record.gatewayType) {
//             GatewayType.SymphonyRoom,
//             GatewayType.SymphonyRoomReqRep -> {
//                 if (record.incomingTopic != null &&
//                     record.gatewayValue != null
//                 ) {
//                     record
//                 } else {
//                     null
//                 }
//             }
//
//             else -> null
//         }
//     }
//
//     companion object {
//         private val LOG = LoggerFactory.getLogger(SymphonyGatewayIncoming::class.java)
//     }
//
//     @Synchronized
//     private fun startListeningFeed() {
//
//         if (listening) return
//
//         val botClient = connection.botClient
//         botClient.datafeed().subscribe(object : RealTimeEventListener {
//
//             override fun onMessageSent(initiator: V4Initiator?, event: V4MessageSent) {
//
//                 this@SymphonyGatewayIncoming.onRoomMessage(event)
//             }
//         })
//         executorsDataFeed.submit {
//             LOG.info("Starting Data Feed listener for connection: $connectionId")
//             botClient.datafeed().start()
//             LOG.info("Creating Symphony Room Listener for: $topicByStreamId.entries")
//         }
//         listening = true
//     }
//
//     private fun publish(message: Notify) = runBlocking {
//         entityDb.audited(
//             "SYMPHONY_INCOMING_GATEWAY",
//             "INSERT",
//             "Notification created from symphony listener"
//         ).insert(message)
//     }
// }
