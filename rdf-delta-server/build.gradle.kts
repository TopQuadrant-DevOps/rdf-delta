/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.seaborne.rdf-delta.java-conventions")
}

dependencies {
    implementation(project(":rdf-delta-server-http"))
    implementation(project(":rdf-delta-cmds"))
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.0")
}

description = "RDF Delta :: Delta server combined jar"

val testsJar by tasks.registering(Jar::class) {
    archiveClassifier.set("tests")
    from(sourceSets["test"].output)
}

(publishing.publications["maven"] as MavenPublication).artifact(testsJar)
