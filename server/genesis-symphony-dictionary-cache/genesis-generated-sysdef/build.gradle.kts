codeGen {
    configModuleFilter = setOf("genesis-symphony-config")
    useCleanerTask.set(((properties["useCleanerTask"] ?: "true") == "true"))
}

description = "genesis-generated-sysdef"
