/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `jacoco`
    id("org.seaborne.rdf-delta.java-conventions")
}

dependencies {
    implementation("org.apache.jena:jena-cmds:${project.property("ver.jena")}")
    implementation(project(":rdf-patch"))
    implementation(project(":rdf-delta-base"))
    implementation(project(":rdf-delta-server-local"))
    implementation("org.apache.jena:jena-fuseki-main:${project.property("ver.jena")}")
    implementation("io.micrometer:micrometer-core:${project.property("ver.micrometer")}")
    implementation("io.micrometer:micrometer-registry-prometheus:${project.property("ver.micrometer")}")
    implementation("org.eclipse.jetty:jetty-xml:11.0.4")
    testImplementation("org.slf4j:slf4j-jdk14:${project.property("ver.slf4j")}")
}

description = "RDF Delta :: Server (HTTP)"

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = false
    }
}
