childProjects.values.forEach { project ->
    project.tasks {
        artifactoryPublish {
            enabled = false
        }
    }
}

// Add your genesis config dependencies here
dependencies {
    api("global.genesis:genesis-notify-config")
    api("global.genesis:genesis-notify-messages")
    api("global.genesis:auth-config")
}

description = "genesis-symphony-dictionary-cache"
