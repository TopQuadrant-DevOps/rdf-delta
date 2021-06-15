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
    implementation(project(":rdf-delta-server-local"))
    implementation(project(path = ":rdf-delta-server-local", configuration = "testJar"))
    implementation("com.amazonaws:aws-java-sdk-s3:1.12.6")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.slf4j:jcl-over-slf4j:1.7.30")
    testImplementation(project(":rdf-delta-server-local"))
    testImplementation("org.apache.curator:curator-test:${project.property("ver.curator")}")
    testImplementation("io.findify:s3mock_2.12:0.2.6")
    testImplementation("org.slf4j:slf4j-jdk14:${project.property("ver.slf4j")}")
}

description = "RDF Delta :: Server (Extra Components)"

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = false
    }
}
