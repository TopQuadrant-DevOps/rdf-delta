import com.github.jengelman.gradle.plugins.shadow.transformers.ApacheLicenseResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.ApacheNoticeResourceTransformer

plugins {
    `jacoco`
    application
    id("org.seaborne.rdf-delta.java-conventions")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    implementation(project(":rdf-patch"))
    implementation(project(":rdf-delta-base"))
    implementation(project(":rdf-delta-fuseki"))
    implementation(project(":rdf-delta-client"))
    implementation("org.apache.jena:jena-fuseki-main:${project.property("ver.jena")}")
    implementation("io.micrometer:micrometer-core:${project.property("ver.micrometer")}")
    implementation("io.micrometer:micrometer-registry-prometheus:${project.property("ver.micrometer")}")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:${project.property("ver.log4j2")}")
    implementation("org.apache.jena:jena-text:${project.property("ver.jena")}")
}

description = "RDF Delta :: Delta + Fuseki"

application {
    mainClass.set("org.seaborne.delta.fuseki.cmd.DeltaFusekiServerCmd")
}

val testsJar by tasks.registering(Jar::class) {
    archiveClassifier.set("tests")
    from(sourceSets["test"].output)
}

(publishing.publications["maven"] as MavenPublication).artifact(testsJar)

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = false
    }
}

tasks.shadowJar {
    minimize {
        exclude(project(":rdf-delta-cmds"))
    }
    mergeServiceFiles()
    transform(ApacheLicenseResourceTransformer::class.java)
    transform(ApacheNoticeResourceTransformer::class.java)
}
