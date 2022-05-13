plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.azisaba"
version = "3.0.2"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://nexus.velocitypowered.com/repository/maven-public/") }
    maven { url = uri("https://repo.blueberrymc.net/repository/maven-releases/") }
    maven { url = uri("https://jitpack.io/") }
}

dependencies {
    implementation("net.blueberrymc:native-util:2.1.0")
    implementation("org.javassist:javassist:3.28.0-GA")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.1")
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
    compileOnly("io.netty:netty-codec-haproxy:4.1.76.Final")
    compileOnly("com.github.AzisabaNetwork:VelocityRedisBridge:b96753f4df")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    shadowJar {
        relocate("javassist", "net.azisaba.bungeeproxyannouncer.libs.javassist")
        archiveFileName.set("BungeeProxyAnnouncer-${project.version}.jar")
    }
}
