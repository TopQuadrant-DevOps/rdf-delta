/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.seaborne.rdf-delta.java-conventions")
}

dependencies {
    implementation("org.apache.jena:jena-cmds:3.17.0")
    implementation(project(":rdf-patch"))
    implementation(project(":rdf-delta-base"))
    implementation(project(":rdf-delta-server-local"))
    implementation("org.apache.jena:jena-fuseki-main:3.17.0")
    implementation("org.eclipse.jetty:jetty-xml:9.4.38.v20210224")
    testImplementation("org.slf4j:slf4j-jdk14:1.7.30")
}

description = "RDF Delta :: Server (HTTP)"