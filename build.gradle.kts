plugins {
    id("java")
    //Copied this from my test plugin, this allows to run server in IDEA
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "gg.litestrike.game"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")

    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
    implementation("gg.crystalized.lobby:Lobby_plugin:1.0-SNAPSHOT") {
        exclude(group = "com.github.bhlangonijr")
    }

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation("io.papermc.paper:paper-api:26.2.build.121-stable")
    testImplementation("com.github.retrooper:packetevents-spigot:2.13.0")
    testImplementation("com.google.code.gson:gson:2.11.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
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
tasks {
    runServer {
        //I just copied pasted that from my plugin to make run server work so that I can test it in IDEA
        //Default comments that come with setting up the project with a plugin
        /*
            // Configure the Minecraft version for our task.
            // This is the only required configuration besides applying the plugin.
            // Your plugin's jar (or shadowJar if present) will be used automatically.

         */
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}


