/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `jacoco`
    id("org.seaborne.rdf-delta.java-conventions")
}

dependencies {
    implementation(project(":rdf-patch"))
    testImplementation("org.slf4j:slf4j-jdk14:${project.property("ver.slf4j")}")
}

description = "RDF Delta :: Base"

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = false
    }
}
