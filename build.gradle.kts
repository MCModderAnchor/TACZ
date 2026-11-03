import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

// 设置日期格式，用于填充快照版版本号
val dateFormat = SimpleDateFormat("MMdd-HHmmss")
dateFormat.timeZone = TimeZone.getTimeZone("UTC")

plugins {
    alias(libs.plugins.moddev)
    id("com.gradleup.shadow") version "8.3.6"
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
    flatDir {
        dir("libs")
    }
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
    maven("https://repo.sleeping.town") {
        content {
            includeGroup("dev.emi")
        }
    }
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
}

val simpleBedrockModelOriginal by configurations.creating

dependencies {
    simpleBedrockModelOriginal(files("libs/simplebedrockmodel-2.6.1-neoforge-mc1.21.1.jar"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    jarJar(libs.com.maydaymemory.mae)
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
    compileOnly("dev.emi:emi-neoforge:${libs.versions.emi.get()}:api")
    runtimeOnly(libs.emi.neoforge)
    compileOnly(libs.curse.maven.framework)
    compileOnly(libs.curse.maven.controllable)
}

val shadowSimpleBedrockModel by tasks.registering(
    com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class
) {
    configurations = listOf(simpleBedrockModelOriginal)
    relocate(
        "com.github.mcmodderanchor.simplebedrockmodel",
        "com.tacz.internal.sbm"
    )
    archiveFileName.set("simplebedrockmodel-2.6.1-tacz.1-unpatched.jar")
    manifest {
        attributes["Automatic-Module-Name"] = "com.tacz.internal.sbm"
    }
}

val isolatedSimpleBedrockModel = layout.buildDirectory.file(
    "isolated-sbm/simplebedrockmodel-2.6.1-tacz.1.jar"
)

val repackageSimpleBedrockModel by tasks.registering {
    dependsOn(shadowSimpleBedrockModel)
    inputs.file(shadowSimpleBedrockModel.flatMap { it.archiveFile })
    outputs.file(isolatedSimpleBedrockModel)

    doLast {
        val source = shadowSimpleBedrockModel.get().archiveFile.get().asFile
        val output = isolatedSimpleBedrockModel.get().asFile
        output.parentFile.mkdirs()

        fun replaceAscii(input: ByteArray, old: String, new: String): ByteArray {
            val search = old.toByteArray(Charsets.US_ASCII)
            val replacement = new.toByteArray(Charsets.US_ASCII)
            val result = ByteArrayOutputStream(input.size)
            var index = 0
            while (index < input.size) {
                val matches = index + search.size <= input.size &&
                    search.indices.all { input[index + it] == search[it] }
                if (matches) {
                    result.write(replacement)
                    index += search.size
                } else {
                    result.write(input[index].toInt())
                    index++
                }
            }
            return result.toByteArray()
        }

        fun rewriteClass(input: ByteArray): ByteArray {
            val data = DataInputStream(input.inputStream())
            val result = ByteArrayOutputStream(input.size)
            val rewritten = DataOutputStream(result)

            rewritten.writeInt(data.readInt())
            rewritten.writeShort(data.readUnsignedShort())
            rewritten.writeShort(data.readUnsignedShort())
            val constantPoolCount = data.readUnsignedShort()
            rewritten.writeShort(constantPoolCount)

            var index = 1
            while (index < constantPoolCount) {
                when (val tag = data.readUnsignedByte()) {
                    1 -> {
                        val length = data.readUnsignedShort()
                        val bytes = ByteArray(length)
                        data.readFully(bytes)
                        rewritten.writeByte(tag)
                        val encoded = replaceAscii(
                            bytes,
                            "simplebedrockmodel",
                            "tacz_internal_sbm"
                        )
                        if (!encoded.contentEquals(bytes)) {
                            rewritten.writeShort(encoded.size)
                            rewritten.write(encoded)
                        } else {
                            rewritten.writeShort(length)
                            rewritten.write(bytes)
                        }
                    }
                    3, 4 -> {
                        rewritten.writeByte(tag)
                        rewritten.writeInt(data.readInt())
                    }
                    5, 6 -> {
                        rewritten.writeByte(tag)
                        rewritten.writeLong(data.readLong())
                        index++
                    }
                    7, 8, 16, 19, 20 -> {
                        rewritten.writeByte(tag)
                        rewritten.writeShort(data.readUnsignedShort())
                    }
                    9, 10, 11, 12, 17, 18 -> {
                        rewritten.writeByte(tag)
                        rewritten.writeInt(data.readInt())
                    }
                    15 -> {
                        rewritten.writeByte(tag)
                        rewritten.writeByte(data.readUnsignedByte())
                        rewritten.writeShort(data.readUnsignedShort())
                    }
                    else -> error("Unsupported class constant-pool tag: $tag")
                }
                index++
            }

            data.copyTo(rewritten)
            rewritten.flush()
            return result.toByteArray()
        }

        ZipFile(source).use { input ->
            ZipOutputStream(output.outputStream()).use { archive ->
                val entries = input.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val relocatedName = entry.name
                        .replace(
                            "assets/simplebedrockmodel/",
                            "assets/tacz_internal_sbm/"
                        )
                        .replace(
                            "simplebedrockmodel.mixins.json",
                            "tacz_internal_sbm.mixins.json"
                        )
                    val content = input.getInputStream(entry).use { it.readBytes() }
                    val rewrittenContent = when {
                        entry.name.endsWith(".class") -> rewriteClass(content)
                        entry.name == "META-INF/neoforge.mods.toml" ||
                            entry.name == "simplebedrockmodel.mixins.json" ||
                            entry.name.endsWith(".json") ||
                            entry.name.endsWith(".mcmeta") ||
                            entry.name.endsWith(".properties") ||
                            entry.name.endsWith(".txt") -> {
                            String(content, Charsets.UTF_8)
                                .replace(
                                    "com.github.mcmodderanchor.simplebedrockmodel",
                                    "com.tacz.internal.sbm"
                                )
                                .replace("simplebedrockmodel", "tacz_internal_sbm")
                                .replace(
                                    "2.6.1-neoforge-mc1.21.1",
                                    "2.6.1-tacz.1"
                                )
                                .toByteArray(Charsets.UTF_8)
                        }
                        else -> content
                    }

                    archive.putNextEntry(ZipEntry(relocatedName))
                    archive.write(rewrittenContent)
                    archive.closeEntry()
                }
            }
        }
    }
}

val isolatedSimpleBedrockModelDependency =
    files(isolatedSimpleBedrockModel).builtBy(repackageSimpleBedrockModel)

dependencies {
    implementation(isolatedSimpleBedrockModelDependency)
    jarJar(isolatedSimpleBedrockModelDependency)
}

tasks.named("compileJava") {
    dependsOn(repackageSimpleBedrockModel)
}

tasks.named("jarJar") {
    dependsOn(repackageSimpleBedrockModel)
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


tasks.register("gunPackCompatibilityTest") {
    group = "verification"
    description = "Runs gun-pack overlays and universal conversion regression checks."
    dependsOn(tasks.test)
}
