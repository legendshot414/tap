import io.papermc.paperweight.tasks.RemapJar

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" apply false
}

subprojects {
    // net.minecraft.server 프로젝트의 이름은 반드시 v로 시작 [v1.19]
    apply(plugin = "io.papermc.paperweight.userdev")
    dependencies {
        implementation(projectApi)
        implementation(projectCore)
        val paperweight = (this as ExtensionAware).extensions.getByName("paperweight")
                as io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension
        paperweight.paperDevBundle("${name.substring(1)}-R0.1-SNAPSHOT")
    }
    extensions.configure<io.papermc.paperweight.userdev.PaperweightUserExtension> {
        reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
    }
}

// upstream
coreDevJar {
    from(subprojects.map { it.sourceSets["main"].output })
}

coreSourcesJar {
    from(subprojects.map { it.sourceSets["main"].allSource })
}
