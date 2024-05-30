package global.genesis.notify.pal.extn

import global.genesis.commons.config.GenesisConfigurationException
import global.genesis.notify.gateway.email.EmailGatewayConfig
import global.genesis.notify.pal.NotifyBuilder
import global.genesis.notify.pal.NotifyDefinition
import global.genesis.notify.pal.NotifyScript
import global.genesis.symphony.gateway.SymphonyGatewayConfig
import org.apache.commons.collections.CollectionUtils
import org.junit.jupiter.api.Test
import org.simplejavamail.api.mailer.config.TransportStrategy
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotifyDefinitionTest {

    @Test
    fun test() {
        var result: NotifyBuilder? = null

        runScript {
            result = notify {
                gateways {
                    symphony(id = "symphony1") {
                        botUsername = "test1@email.com"
                        sessionAuthHost = "abc"
                        botPrivateKeyFromDb = true
                        appId = "2"
                    }

                    symphony(id = "symphony2") {
                        botUsername = "test2@email.com"
                        sessionAuthHost = "abc"
                        botPrivateKeyFromDb = true
                        appId = "2"
                    }

                    email(id = "email1") {
                        smtpUser = "test3@email.com"
                        smtpHost = ""
                        smtpPw = ""
                        systemDefaultEmail = "abc@email.com"
                        systemDefaultUserName = "Abc"
                        smtpProtocol = TransportStrategy.SMTPS
                    }
                }
            }
        }

        val definition = (result as NotifyDefinition)

        val configById = definition.gatewayConfigs.configById
        val symphony1 = configById["symphony1"]
        val symphony2 = configById["symphony2"]
        val email1 = configById["email1"]
        assertEquals("test1@email.com", (symphony1 as SymphonyGatewayConfig).botUsername)
        assertEquals("test2@email.com", (symphony2 as SymphonyGatewayConfig).botUsername)
        assertEquals("test3@email.com", (email1 as EmailGatewayConfig).smtpUser)
        assertEquals("Abc", email1.systemDefaultUserName)
        assertEquals("abc@email.com", email1.systemDefaultEmail)
    }

    @Test
    fun test_error_accumulation() {
        var result: NotifyBuilder? = null
        try {
            runScript {
                result = notify {
                    gateways {
                        symphony(id = "symphony1") {
                            botUsername = "test1@email.com"
                        }

                        symphony(id = "symphony1") {
                            botUsername = "test2@email.com"
                            botPrivateKeyFromDb = true
                            appId = "2"
                        }

                        symphony(id = "symphony2") {
                            botUsername = "test2@email.com"
                        }

                        symphony(id = "symphony2") {
                            botUsername = "test3@email.com"
                            botPrivateKeyFromDb = true
                            appId = "3"
                        }
                    }
                }
            }
            (result as NotifyDefinition).gatewayConfigs.validate()
        } catch (ex: GenesisConfigurationException) {
            val list = listOf(
                "Detected duplicate gateway name: symphony1",
                "Detected duplicate gateway name: symphony2",
                "symphony1; sessionAuthHost is a required Parameter",
                "symphony1; botPrivateKeyPath is a required Parameter, when botPrivateKeyFromDb is not true",
                "symphony1; botPrivateKeyName is a required Parameter, when botPrivateKeyFromDb is not true",
                "symphony2; sessionAuthHost is a required Parameter",
                "symphony2; botPrivateKeyPath is a required Parameter, when botPrivateKeyFromDb is not true",
                "symphony2; botPrivateKeyName is a required Parameter, when botPrivateKeyFromDb is not true"
            )
            val errorList = (result as NotifyDefinition).gatewayConfigs.errorList
            assertTrue(CollectionUtils.isEqualCollection(list, errorList))
        }
    }

    private fun runScript(init: NotifyScript.() -> Unit) {
        val builder = NotifyScript()
        builder.init()
    }
}
