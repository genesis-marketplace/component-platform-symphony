dependencies {
    compileOnly("global.genesis:genesis-dictionary")
    compileOnly("global.genesis:genesis-process")
    compileOnly("global.genesis:genesis-pal-execution")
    compileOnly(project(path = ":genesis-symphony-test-dictionary-cache", configuration = "codeGen"))
    implementation(project(":genesis-symphony-config"))
    implementation("global.genesis:genesis-notify-config")
}

description = "genesis-symphony-test-config"

tasks {
    copyDependencies {
        enabled = false
    }
}
