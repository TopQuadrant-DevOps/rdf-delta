/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `jacoco`
    id("org.seaborne.rdf-delta.java-conventions")
}

dependencies {
    implementation(project(":rdf-patch"))
    implementation(project(":rdf-delta-base"))
    implementation(project(":rdf-delta-server"))
    implementation(project(":rdf-delta-server-local"))
    implementation(project(path = ":rdf-delta-server-local", configuration = "testJar"))
    implementation(project(":rdf-delta-server-http"))
    implementation(project(":rdf-delta-client"))
    implementation("org.apache.jena:jena-fuseki-main:${project.property("ver.jena")}")
    implementation("io.micrometer:micrometer-core:${project.property("ver.micrometer")}")
    implementation("io.micrometer:micrometer-registry-prometheus:${project.property("ver.micrometer")}")
    implementation("org.awaitility:awaitility:4.1.0")
    implementation("org.slf4j:slf4j-jdk14:${project.property("ver.slf4j")}")
    implementation("org.apache.curator:curator-recipes:${project.property("ver.curator")}")
    implementation("org.apache.curator:curator-test:${project.property("ver.curator")}")
}

description = "RDF Delta :: Integration Tests"

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = false
    }
}
