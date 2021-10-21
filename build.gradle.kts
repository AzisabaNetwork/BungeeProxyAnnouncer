plugins {
    kotlin("jvm") version "1.5.30"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "net.azisaba"
version = "1.0.2"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo2.acrylicstyle.xyz") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.30")
    implementation("net.blueberrymc:native-util:1.2.2")
    implementation("org.javassist:javassist:3.28.0-GA")
    compileOnly("io.netty:netty-codec-haproxy:4.1.67.Final")
    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        relocate("kotlin", "net.azisaba.bungeeProxyAnnouncer.libs.kotlin")
        relocate("javassist", "net.azisaba.bungeeProxyAnnouncer.libs.javassist")
        relocate("org", "net.azisaba.bungeeProxyAnnouncer.libs.org")

        minimize()
        archiveFileName.set("BungeeProxyAnnouncer-${project.version}.jar")
    }
}
