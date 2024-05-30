package global.genesis.symphony.gateway

import global.genesis.gen.dao.Notify
import global.genesis.gen.dao.enums.NotifySeverity
import global.genesis.session.UserEmailCache
import javax.inject.Inject

class SymphonyMessageFormatter @Inject constructor(private val emailCache: UserEmailCache) {

    fun format(message: Notify, users: Set<String> = emptySet()): String {
        val colour = when (message.notifySeverity) {
            NotifySeverity.Information -> "blue"
            NotifySeverity.Warning -> "orange"
            NotifySeverity.Serious -> "red"
            NotifySeverity.Critical -> "purple"
        }
        val mentions = buildMentions(users)

        return "<h5 style=\"color:${colour}\"><b>${message.header} [${message.notifySeverity}]</b></h5>$mentions<b/>${message.body}"
    }

    /*
    From what I can see there is no way that the username list passed to this method could
    ever have been non-empty. The routes created by the NotifyServerCreateChannelEventHandler
    will always create a route with an entity type of GATEWAY, meaning an empty user set. There
    are no examples in Production usage of us ever sending a symphony message with the 'mentions'
    populated. However, I am leaving this code as a reference.
     */
    private fun buildMentions(users: Set<String>): StringBuilder {
        val mentions = StringBuilder()
        users.mapNotNull { emailCache.userEmailAddress(it) }
            .forEach { mentions.append("<mention email=\"$it\" strict=\"false\"/> ") }
        return mentions
    }
}
