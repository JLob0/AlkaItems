import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.alkacode"
version = "1.0.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    // Banco/GUI base vem do AlkaCore (DatabaseProvider/BaseGui) - AlkaItems nao abre
    // conexao JDBC propria nem registra o proprio GuiListener.
    compileOnly("com.alkacode:AlkaCore:1.0.6")
    compileOnly("me.clip:placeholderapi:2.11.6")
    // ItemsAdder e AdvancedEnchantments sao plugins de terceiro sem artefato Maven
    // publico confiavel - integrados 100% via reflection (ver hook/), sem compileOnly,
    // mesmo padrao ja usado no resto do ecossistema (AlkaVips/AlkaMines/AlkaAnvil).
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {

        expand("version" to project.version)

    }
}
