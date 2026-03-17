dependencies {
    api(projectApi)
}

tasks {
    jar {
        archiveClassifier.set("origin")
    }

    register<Jar>("coreDevJar") {
        from(sourceSets["main"].output)
    }

}