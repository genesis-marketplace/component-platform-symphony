package global.genesis.symphony.message.request

data class ListChannelMembersRequest(
    val gatewayId: String?,
    val channelName: String
)

data class ListChannelMembersResponse(
    val userEmail: String?,
    val userId: Long
)
