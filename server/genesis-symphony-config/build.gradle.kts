dependencies {
    compileOnly("global.genesis:genesis-dictionary")
    compileOnly("global.genesis:genesis-process")
    compileOnly("global.genesis:genesis-pal-execution")
    compileOnly(project(path = ":genesis-symphony-dictionary-cache", configuration = "codeGen"))
    testImplementation(project(path = ":genesis-symphony-dictionary-cache", configuration = "codeGen"))
}

description = "genesis-symphony-config"
