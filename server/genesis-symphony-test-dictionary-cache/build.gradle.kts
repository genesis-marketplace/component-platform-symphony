childProjects.values.forEach { project ->
    project.tasks {
        artifactoryPublish {
            enabled = false
        }
    }
}

// Add your genesis config dependencies here
dependencies {
    api("global.genesis:genesis-notify-test-config:${properties["notifyVersion"]}")
    api("global.genesis:genesis-notify-config:${properties["notifyVersion"]}")
}

description = "genesis-symphony-test-dictionary-cache"
