import java.text.SimpleDateFormat
import java.util.*

// 设置日期格式，用于填充快照版版本号
val dateFormat = SimpleDateFormat("MMdd-HHmmss")
dateFormat.timeZone = TimeZone.getTimeZone("UTC")

plugins {
    alias(libs.plugins.moddev)
}

val id = project.property("mod_id") as String
group = project.property("maven_group") as String
version = dateFormat.format(Date())
//version = project.property("mod_version") as String

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
            jvmArguments.addAll("-Xmx4G", "-Xms4G")
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

    unitTest {
        enable()
        testedMod.set(mods[id])
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    implementation(libs.com.github.mcmodderanchor.simplebedrockmodel)
    jarJar(libs.com.github.mcmodderanchor.simplebedrockmodel)
    compileOnly(libs.com.maydaymemory.mae)

    implementation(libs.org.apache.commons.math3)
    jarJar(libs.org.apache.commons.math3)
    implementation(libs.com.github.figuraMC.luaj.core)
    jarJar(libs.com.github.figuraMC.luaj.core)
    implementation(libs.com.github.figuraMC.luaj.jse)
    jarJar(libs.com.github.figuraMC.luaj.jse)
    implementation(libs.org.apache.bcel)
    jarJar(libs.org.apache.bcel)

    implementation(libs.me.shedaniel.cloth.config.neoforge)
    implementation(libs.dev.kosmx.player.animation.lib.forge)
    implementation(libs.maven.modrinth.sodium)
    implementation(libs.maven.modrinth.iris)
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

tasks.test {
    useJUnitPlatform()
    providers.gradleProperty("gunPackLegacyFixture").orNull?.let {
        systemProperty("tacz.gunpack.legacyFixture", file(it).absolutePath)
    }
    providers.gradleProperty("gunPackBatchDirectory").orNull?.let {
        systemProperty("tacz.gunpack.batchDirectory", file(it).absolutePath)
        systemProperty("tacz.gunpack.batchOutput", layout.buildDirectory.dir("gunpack-client-a-batch").get().asFile.absolutePath)
    }
    workingDir = layout.buildDirectory.dir("gunpack-compatibility-tests").get().asFile
    doFirst { workingDir.mkdirs() }
}

tasks.register("gunPackCompatibilityTest") {
    group = "verification"
    description = "Runs gun-pack overlays and universal conversion regression checks."
    dependsOn(tasks.test)
}
