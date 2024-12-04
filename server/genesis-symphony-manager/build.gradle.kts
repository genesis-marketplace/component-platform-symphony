plugins {
    id("global.genesis.packagescan")
}

storeClasses {
    scanPackage(name = "global.genesis.symphony")
}

dependencies {
    api(project(":genesis-symphony-messages"))
    api("global.genesis:genesis-notify-dispatcher")
    api("global.genesis:genesis-net")
    api("global.genesis:genesis-messages")
    api("global.genesis:genesis-pal-execution")
    api("global.genesis:genesis-jackson")
    api("global.genesis:genesis-db-server")
    api("global.genesis:genesis-eventhandler")
    api("global.genesis:genesis-pal-eventhandler")
    api("global.genesis:genesis-pal-dataserver")
    api("global.genesis:genesis-requestserver")
    api("global.genesis:genesis-process")
    api("global.genesis:genesis-metrics")
    api("org.slf4j:slf4j-api")
    api(platform("org.finos.symphony.bdk:symphony-bdk-bom:3.0.3"))
    api("org.finos.symphony.bdk:symphony-bdk-core") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    api("org.finos.symphony.bdk:symphony-bdk-template-freemarker") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    api("org.finos.symphony.bdk:symphony-bdk-http-jersey2") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    api("org.apache.commons:commons-compress")
    implementation("global.genesis:file-server-api")
    testImplementation("global.genesis:genesis-dbtest")
    testImplementation("global.genesis:genesis-testsupport")
    testImplementation("global.genesis:genesis-messages")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.awaitility:awaitility-kotlin")
    // Jose: Is compileOnly equivalent to "provided"?
    compileOnly(project(path = ":genesis-symphony-dictionary-cache", configuration = "codeGen"))
    testImplementation(project(path = ":genesis-symphony-dictionary-cache", configuration = "codeGen"))
    testImplementation(project(":genesis-symphony-config"))
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.awaitility:awaitility-kotlin")
    testImplementation("org.hamcrest:hamcrest-library")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

tasks {
    test {
        maxHeapSize = "4g"
        minHeapSize = "256m"
    }
}

description = "genesis-symphony-manager"
