rootProject.name = "genesisproduct-genesis-symphony"

pluginManagement {
    pluginManagement {
        val genesisVersion: String by settings
        val deployPluginVersion: String by settings
        plugins {
            id("global.genesis.build") version genesisVersion
            id("global.genesis.packagescan") version genesisVersion
            id("global.genesis.deploy") version deployPluginVersion
        }
    }
    repositories {
        mavenLocal {
            // VERY IMPORTANT!!! EXCLUDE AGRONA AS IT IS A POM DEPENDENCY AND DOES NOT PLAY NICELY WITH MAVEN LOCAL!
            content {
                excludeGroup("org.agrona")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://genesisglobal.jfrog.io/genesisglobal/dev-repo")
            credentials {
                username = extra.properties["genesisArtifactoryUser"].toString()
                password = extra.properties["genesisArtifactoryPassword"].toString()
            }
        }
    }
}

include("genesis-symphony-config")
include("genesis-symphony-messages")
include("genesis-symphony-manager")
include("genesis-symphony-script-config")
include("genesis-symphony-distribution")
include("genesis-symphony-dictionary-cache")
include("genesis-symphony-dictionary-cache:genesis-generated-sysdef")
include("genesis-symphony-dictionary-cache:genesis-generated-fields")
include("genesis-symphony-dictionary-cache:genesis-generated-dao")
include("genesis-symphony-dictionary-cache:genesis-generated-hft")
include("genesis-symphony-dictionary-cache:genesis-generated-view")
include("genesis-symphony-test-config")
include("genesis-symphony-test-dictionary-cache")
include("genesis-symphony-test-dictionary-cache:genesis-generated-sysdef")
include("genesis-symphony-test-dictionary-cache:genesis-generated-fields")
include("genesis-symphony-test-dictionary-cache:genesis-generated-dao")
include("genesis-symphony-test-dictionary-cache:genesis-generated-hft")
include("genesis-symphony-test-dictionary-cache:genesis-generated-view")
