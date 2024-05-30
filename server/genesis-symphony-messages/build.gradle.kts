dependencies {
    implementation("global.genesis:genesis-messages")
    implementation("global.genesis:genesis-notify-messages")
    compileOnly(project(path = ":genesis-symphony-dictionary-cache", configuration = "codeGen"))
}

description = "genesis-symphony-messages"
