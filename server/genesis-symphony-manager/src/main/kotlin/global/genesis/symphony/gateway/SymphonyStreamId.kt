package global.genesis.symphony.gateway

import org.apache.commons.lang3.StringUtils

data class SymphonyStreamId(val originalStreamId: String) {

    // https://developers.symphony.com/restapi/docs/overview-of-streams
    val symphonyStreamId: String = StringUtils.stripEnd(originalStreamId.replace("/", "_").replace("+", "-"), "=")

    // note, we dont use the originalStream as part of the identity as it only used for display purposes
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SymphonyStreamId

        if (symphonyStreamId != other.symphonyStreamId) return false

        return true
    }

    override fun hashCode(): Int {
        return symphonyStreamId.hashCode()
    }
}
