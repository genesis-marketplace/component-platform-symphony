package global.genesis.symphony

import com.symphony.bdk.gen.api.model.MemberInfo
import com.symphony.bdk.gen.api.model.RoomSystemInfo
import com.symphony.bdk.gen.api.model.UserV2
import com.symphony.bdk.gen.api.model.V3RoomAttributes
import com.symphony.bdk.gen.api.model.V3RoomDetail
import global.genesis.commons.annotation.Module
import global.genesis.symphony.gateway.SymphonyConnection
import global.genesis.symphony.gateway.SymphonyConnectionFactory
import global.genesis.symphony.gateway.SymphonyGatewayConfig
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`

@Module
class MockSymphonyConnectionFactory : SymphonyConnectionFactory {

    private val mock = Mockito.mock(SymphonyConnection::class.java, Mockito.RETURNS_DEEP_STUBS)

    init {
        val member1 = MemberInfo()
        member1.id = 1
        val member2 = MemberInfo()
        member2.id = 2
        `when`(mock.botClient.streams().listRoomMembers(Mockito.anyString())).thenReturn(listOf(member1, member2))
        val user1 = UserV2()
        user1.id = 1
        user1.emailAddress = "user@genesis.com"

        `when`(mock.botClient.users().listUsersByIds(listOf(1))).thenReturn(listOf(user1))

        val v3RoomDetail = V3RoomDetail()
        v3RoomDetail.roomSystemInfo = RoomSystemInfo().id("foo")
        `when`(mock.botClient.streams().create(any<V3RoomAttributes>())).thenReturn(v3RoomDetail)
    }

    override fun create(config: SymphonyGatewayConfig): SymphonyConnection {
        return mock
    }
}
