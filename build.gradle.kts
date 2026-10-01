

plugins {
    alias(libs.plugins.moddev)
}

val id = project.property("mod_id") as String
group = project.property("maven_group") as String
version = project.property("mod_version") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

neoForge {
    version = libs.versions.neoforge.asProvider().get()
    parchment {
        mappingsVersion = libs.versions.parchment.get()
        minecraftVersion = libs.versions.minecraft.asProvider().get()
    }
    validateAccessTransformers = true

    runs {
        configureEach {
            systemProperty("forge.logging.console.level", "debug")

            dependencies {
                additionalRuntimeClasspathConfiguration(libs.org.apache.commons.math3)
                additionalRuntimeClasspathConfiguration(libs.com.github.figuraMC.luaj.core)
                additionalRuntimeClasspathConfiguration(libs.com.github.figuraMC.luaj.jse)
                additionalRuntimeClasspathConfiguration(libs.org.apache.bcel)
            }
        }

        create("client") {
            client()
            gameDirectory = file("run/client_a")
        }

        create("client2") {
            client()
            gameDirectory = file("run/client_b")
            programArguments = listOf("--username", "mayday_memory")
        }

        create("server") {
            server()
            gameDirectory = file("run/server")
        }

        create("data") {
            data()
        }
    }

    mods {
        create(id) {
            sourceSet(sourceSets["main"])
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.rtyley")
            includeGroup("com.github.FiguraMC.luaj")
        }
    }
    maven("https://maven.shedaniel.me")
    maven("https://maven.kosmx.dev")
    maven("https://maven.blamejared.com")
    maven {
        url = uri("https://maven.architectury.dev")
        content {
            includeGroup("dev.architectury")
        }
    }
    maven {
        url = uri("https://maven.latvian.dev/releases")
        content {
            includeGroup("dev.latvian.mods")
            includeGroup("dev.latvian.apps")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "CurseForge"
                url = uri("https://cursemaven.com")
            }
        }
        filter {
            includeGroup("curse.maven")
        }
    }
    flatDir {
        dir("libs")
    }
}

dependencies {
    implementation("com.github.mcmodderanchor:simplebedrockmodel:2.6.0-neoforge-mc1.21.1")
    jarJar("com.github.mcmodderanchor:simplebedrockmodel:2.6.0-neoforge-mc1.21.1")
    compileOnly(libs.com.maydaymemory.mae)

    implementation(libs.org.apache.commons.math3)
    jarJar(libs.org.apache.commons.math3)
    implementation(libs.com.github.figuraMC.luaj.core)
    jarJar(libs.com.github.figuraMC.luaj.core)
    implementation(libs.com.github.figuraMC.luaj.jse)
    jarJar(libs.com.github.figuraMC.luaj.jse)
    implementation(libs.org.apache.bcel)
    jarJar(libs.org.apache.bcel)

    compileOnly(libs.me.shedaniel.cloth.config.neoforge)
    compileOnly(libs.dev.kosmx.player.animation.lib.forge)
    compileOnly(libs.maven.modrinth.sodium)
    compileOnly(libs.maven.modrinth.iris)
    compileOnly(libs.curse.maven.acceleratedrendering)
    compileOnly(libs.maven.modrinth.carry.on)
    compileOnly(libs.maven.modrinth.shoulder.surfing.reloaded)
    compileOnly(libs.mezz.jei.common.api)
    compileOnly(libs.mezz.jei.neoforge.api)
    runtimeOnly(libs.mezz.jei.neoforge)
    compileOnly(libs.curse.maven.framework)
    compileOnly(libs.curse.maven.controllable)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val properties = mapOf(
        "id" to id,
        "version" to project.version,
        "name" to project.property("mod_name") as String,
        "minecraft_version" to libs.versions.minecraft.range.get(),
        "loader_version" to libs.versions.neoforge.range.get()
    )
    filteringCharset = "UTF-8"
    inputs.properties(properties)
    filesMatching("META-INF/neoforge.mods.toml") { expand(properties) }
}
