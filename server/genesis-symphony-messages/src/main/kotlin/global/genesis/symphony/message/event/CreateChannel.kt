package global.genesis.symphony.message.event

data class CreateChannel(
    val topic: String,
    val channelName: String,
    val external: Boolean = false,
    val multilateral: Boolean = false,
    val discoverable: Boolean = true,
    val public: Boolean = true,
    val viewHistory: Boolean = true,
    val gatewayId: String? = null,
    val membersCanInvite: Boolean = true
)
