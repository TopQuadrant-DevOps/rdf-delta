/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `jacoco`
    id("org.seaborne.rdf-delta.java-conventions")
}

dependencies {
    implementation("org.apache.jena:jena-cmds:3.17.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
    implementation("org.apache.logging.log4j:log4j-api:2.14.1")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation(project(":rdf-patch"))
    implementation(project(":rdf-delta-base"))
    implementation(project(":rdf-delta-client"))
    implementation(project(":rdf-delta-server-local"))
    implementation(project(":rdf-delta-server-http"))
    implementation(project(":rdf-delta-server-extra"))
    implementation(project(":rdf-delta-fuseki"))
    implementation("org.apache.jena:jena-text:3.17.0")
    implementation("org.apache.jena:jena-fuseki-main:3.17.0")
    implementation("org.apache.curator:curator-test:5.1.0")
    testImplementation("io.findify:s3mock_2.12:0.2.6")
    testImplementation(project(":rdf-delta-server-extra"))
}

description = "RDF Delta :: Command Line Utilities"

tasks {
    test {
        setForkEvery(1)
    }
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = false
    }
}
