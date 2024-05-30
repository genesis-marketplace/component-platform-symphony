ext.set("localDaogenVersion", "GENESIS_SYMPHONY")

plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("org.jetbrains.kotlinx.kover") version "0.7.6"
    id("org.gradle.test-retry") version "1.5.8"
    id("com.jfrog.artifactory") version "5.2.0"
    id("org.sonarqube") version "5.0.0.4638"
    `maven-publish`
    id("global.genesis.build")
}

sonarqube {
    properties {
        property("sonar.projectKey", "genesislcap_genesis-symphony")
        property("sonar.projectName", "pbc-genesis-symphony")
        property("sonar.organization", "genesislcap")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

val isCiServer = System.getenv().containsKey("CI")
val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()

project(":genesis-symphony-config") {
    sonarqube {
        properties {
            property("sonar.sources", "src/main")
        }
    }
}

project(":genesis-symphony-manager") {
    sonarqube {
        properties {
            property("sonar.sources", "src/main")
            property("sonar.tests", "src/test")
        }
    }
}

project(":genesis-symphony-messages") {
    sonarqube {
        properties {
            property("sonar.sources", "src/main")
        }
    }
}

subprojects {
    afterEvaluate {
        val copyDependencies = tasks.findByName("copyDependencies") ?: return@afterEvaluate
        tasks.withType<Jar> {
            dependsOn(copyDependencies)
        }
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.jfrog.artifactory")

    dependencies {
        implementation(platform("global.genesis:genesis-bom:${properties["genesisVersion"]}"))
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        constraints {
            api("global.genesis:genesis-notify-config:${properties["notifyVersion"]}")
            api("global.genesis:genesis-notify-dispatcher:${properties["notifyVersion"]}")
            api("global.genesis:genesis-notify-messages:${properties["notifyVersion"]}")
            api("global.genesis:auth-config:${properties["authVersion"]}")
            api("global.genesis:file-server-config:${properties["fileServerVersion"]}")
            implementation("global.genesis:file-server-api:${properties["fileServerVersion"]}")
            testImplementation("global.genesis:auth-dictionary-cache:${properties["authVersion"]}")
        }
    }
    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
            }
        }
        val java = "17"

        compileKotlin {
            kotlinOptions { jvmTarget = java }
        }

        test {
            useJUnitPlatform()

            maxHeapSize = "2G"

            val testProperties = listOf(
                "DbLayer",
                "MqLayer",
                "DbHost",
                "DbUsername",
                "DbPassword",
                "AliasSource",
                "ClusterMode",
                "DictionarySource",
                "DbNamespace",
                "DbMode",
                "DbThreadsMax",
                "DbThreadsMin",
                "DbThreadKeepAliveSeconds",
                "DbSqlConnectionPoolSize",
                "DbQuotedIdentifiers"
            )
            val properties = System.getProperties()
            for (property in testProperties) {
                val value = properties.getProperty(property) ?: ext.properties[property]?.toString()
                if (value != null) {
                    inputs.property(property, value)
                    systemProperty(property, value)
                }
            }

            // Add exports and opens so ChronicleQueue can continue working in JDK 17.
            // More info in: https://chronicle.software/chronicle-support-java-17/
            jvmArgs = jvmArgs!! + listOf(
                "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
                "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED" // this one is opened for LMDB
            )

            if (os.isMacOsX) {
                // Needed to guarantee FDB java bindings will work as expected in MacOS
                environment("DYLD_LIBRARY_PATH", "/usr/local/lib")
            }
            // UK Locale changed in recent Java versions and the abbreviation for September is now Sept instead of Sep.
            // This cases our DumpTableFormattedTest.test dump table formatted to fail. Setting to COMPAT mode allows
            // same behaviour as Java 8. We should deal with this at some point.
            // More info here: https://bugs.openjdk.org/browse/JDK-8256837
            // And here: https://bugs.openjdk.org/browse/JDK-8273437
            systemProperty("java.locale.providers", "COMPAT")
            if (!isCiServer) {
                systemProperty("kotlinx.coroutines.debug", "")
            }
        }
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            exclude { element -> element.file.path.contains("generated") }
        }
    }
}

tasks {
    assemble {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("assemble"))
        }
    }
    build {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("build"))
        }
    }
    clean {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("clean"))
        }
    }
    test {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("test"))
        }
    }
    this.ktlintFormat {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("ktlintFormat"))
        }
    }
    this.ktlintCheck {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("ktlintCheck"))
        }
    }
    publishToMavenLocal {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("publishToMavenLocal"))
        }
    }
}

allprojects {

    group = "global.genesis"

    kotlin {
        jvmToolchain {
            (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(11))
        }
    }
    tasks.withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://genesisglobal.jfrog.io/genesisglobal/dev-repo")
            credentials {
                username = properties["genesisArtifactoryUser"].toString()
                password = properties["genesisArtifactoryPassword"].toString()
            }
        }
        val repoKey = buildRepoKey()
        maven {
            url = uri("https://genesisglobal.jfrog.io/genesisglobal/$repoKey")
            credentials {
                username = properties["genesisArtifactoryUser"].toString()
                password = properties["genesisArtifactoryPassword"].toString()
            }
        }
    }

    publishing {
        publications.create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

artifactory {
    setContextUrl("https://genesisglobal.jfrog.io/genesisglobal")

    publish {
        repository {
            setRepoKey(buildRepoKey())
            setUsername(property("genesisArtifactoryUser").toString())
            setPassword(property("genesisArtifactoryPassword").toString())
        }
        defaults {
            publications("ALL_PUBLICATIONS")
            setPublishArtifacts(true)
            setPublishPom(true)
        }
    }
}

fun buildRepoKey(): String {
    val buildTag = buildTagFor(project.version.toString())
    return "libs-$buildTag-local"
}

fun buildTagFor(version: String): String =
    when (version.substringAfterLast('-')) {
        "SNAPSHOT" -> "snapshot"
        in Regex("""M\d+[a-z]*$""") -> "milestone"
        else -> "release"
    }

operator fun Regex.contains(s: String) = matches(s)
