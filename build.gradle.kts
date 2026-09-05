plugins {
    id("java")
}

group = "gg.litestrike.game"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
	maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
    implementation("gg.crystalized.lobby:Lobby_plugin:1.0-SNAPSHOT") {
        exclude(group = "com.github.bhlangonijr")
    }

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation("io.papermc.paper:paper-api:26.2.build.121-stable")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
        }
    }
}


