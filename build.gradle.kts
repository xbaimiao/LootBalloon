plugins {
    java
    id("com.github.johnrengelman.shadow") version ("7.1.2")
    kotlin("jvm") version "1.7.10"
}

group = "com.xbaimiao.lootballoon"
version = "1.0.6"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.xbaimiao.com/repository/maven-public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.xbaimiao:EasyLib:3.2.3")
    implementation("de.tr7zw:item-nbt-api:2.13.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("com.j256.ormlite:ormlite-core:6.1")
    implementation("com.j256.ormlite:ormlite-jdbc:6.1")
//    implementation("com.xbaimiao.ktor:ktor-plugins-bukkit:1.0.8")
//    implementation("redis.clients:jedis:5.0.1")
    // paper
//    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    // spigot
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly(fileTree("libs"))
    compileOnly("public:ModelEngine:4.0.2")
    compileOnly("public:MythicMobs:5.2.6")
    compileOnly("public:ItemsAdder:3.6.1")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.shadowJar {
    dependencies {
        exclude(dependency("org.slf4j:"))
        exclude(dependency("org.jetbrains:annotations:"))
        exclude(dependency("com.google.code.gson:gson:"))
//            exclude(dependency("org.jetbrains.kotlin:"))
//            exclude(dependency("org.jetbrains:"))
    }

    exclude("LICENSE")
    exclude("META-INF/*.SF")
    archiveClassifier.set("")

    relocate("com.xbaimiao.easylib", "${project.group}.shadow.easylib")
    relocate("com.zaxxer.hikari", "${project.group}.shadow.hikari")
    relocate("com.j256.ormlite", "${project.group}.shadow.ormlite")
    relocate("de.tr7zw.changeme.nbtapi", "${project.group}.shadow.itemnbtapi")
    relocate("kotlin", "${project.group}.shadow.kotlin")
    relocate("kotlinx", "${project.group}.shadow.kotlinx")
    relocate("redis", "${project.group}.shadow.redis")
    relocate("com.xbaimiao.ktor", "${project.group}.shadow.ktor")
    minimize()
}
