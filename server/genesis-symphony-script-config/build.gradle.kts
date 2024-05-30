dependencies {
    implementation("global.genesis:genesis-pal-execution")
    compileOnly("global.genesis:genesis-dictionary")
    api("global.genesis:genesis-pal-dataserver")
    api("global.genesis:genesis-pal-requestserver")
    api("global.genesis:genesis-pal-streamer")
    api("global.genesis:genesis-pal-streamerclient")
    api("global.genesis:genesis-pal-eventhandler")
    api(project(":genesis-symphony-messages"))
    api(project(":genesis-symphony-manager"))
    compileOnly("global.genesis:genesis-notify-dispatcher")
    compileOnly(project(path = ":genesis-symphony-dictionary-cache", configuration = "codeGen"))
    testCompileOnly(project(":genesis-symphony-config"))
    testImplementation("global.genesis:genesis-dbtest")
    testImplementation("global.genesis:genesis-testsupport")
    testImplementation(project(path = ":genesis-symphony-dictionary-cache", configuration = "codeGen"))
}

description = "genesis-symphony-script-config"
