rootProject.name = "genesis-symphony"

// servers
includeBuild("server") {
    name = "genesisproduct-genesis-symphony"
}

pluginManagement {
    val genesisVersion: String by settings
    plugins {
        id("global.genesis.pbc") version genesisVersion
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        maven {
            url = uri("https://genesisglobal.jfrog.io/genesisglobal/dev-repo")
            credentials {
                username = extra.properties["genesisArtifactoryUser"].toString()
                password = extra.properties["genesisArtifactoryPassword"].toString()
            }
        }
    }
}
