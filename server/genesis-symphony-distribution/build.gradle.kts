plugins {
    distribution
}

dependencies {
    implementation(project(":genesis-symphony-config"))
    implementation(project(":genesis-symphony-dictionary-cache"))
    implementation(project(":genesis-symphony-messages"))
    implementation(project(":genesis-symphony-script-config"))
    implementation(project(":genesis-symphony-manager"))
}

description = "genesis-symphony-distribution"

distributions {
    main {
        contents {
            // Octal conversion for file permissions
            val libPermissions = "600".toInt(8)
            val scriptPermissions = "700".toInt(8)
            into("genesis-symphony/bin") {
                from(configurations.runtimeClasspath)
                exclude("genesis-symphony-config*.jar")
                exclude("genesis-symphony-script-config*.jar")
                exclude("genesis-symphony-distribution*.jar")
                include("genesis-symphony-*.jar")
            }
            into("genesis-symphony/lib") {
                from("${project.rootProject.buildDir}/dependencies")
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                exclude("genesis-*.jar")
                exclude("genesis-symphony-*.jar")
                exclude("*.zip")

                fileMode = libPermissions
            }
            into("genesis-symphony/cfg") {
                from("${project.rootProject.projectDir}/genesis-symphony-config/src/main/resources/cfg")
                from(project.layout.buildDirectory.dir("generated/product-details"))
                filter(
                    org.apache.tools.ant.filters.FixCrLfFilter::class,
                    "eol" to org.apache.tools.ant.filters.FixCrLfFilter.CrLf.newInstance("lf")
                )
            }
            into("genesis-symphony/scripts") {
                from("${project.rootProject.projectDir}/genesis-symphony-config/src/main/resources/scripts")
                from("${project.rootProject.projectDir}/genesis-symphony-script-config/src/main/resources/scripts")
                filter(
                    org.apache.tools.ant.filters.FixCrLfFilter::class,
                    "eol" to org.apache.tools.ant.filters.FixCrLfFilter.CrLf.newInstance("lf")
                )
                fileMode = scriptPermissions
            }
            // Removes intermediate folder called with the same name as the zip archive.
            into("/")
        }
    }
}

val distribution by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

// To give custom name to the distribution package
tasks {
    distZip {
        archiveBaseName.set("genesisproduct-genesis-symphony")
        archiveClassifier.set("bin")
        archiveExtension.set("zip")
    }
    copyDependencies {
        enabled = false
    }
}

artifacts {
    val distzip = tasks.distZip.get()
    add("distribution", distzip.archiveFile) {
        builtBy(distzip)
    }
}

publishing {
    publications {
        create<MavenPublication>("genesis-symphonyServerDistribution") {
            artifact(tasks.distZip.get())
        }
    }
}

description = "genesis-symphony-distribution"
