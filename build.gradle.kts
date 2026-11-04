import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins{
    java
}

sourceSets{
    main{
        java.setSrcDirs(listOf("src"))
    }
    // Harness de test headless (voir tests/ et AGENTS.md)
    test{
        java.setSrcDirs(listOf("tests"))
        // assets internes du jeu nécessaires au boot headless (maps par défaut)
        resources.setSrcDirs(listOf("tests/resources"))
    }
}

// Mindustry version to depend on.
// Valid values:
// - latest: depend on the latest release of mindustry
// - be: depend on the very latest commit of mindustry
// - v<number>: depend on a specific version
val mindustryVersion = "v160"
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val sdkRoot: String? = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
val projectName = project.name

repositories{
    mavenCentral()

    //Downloads the dependencies JAR file from Mindustry releases; does not use any real repository. Surprisingly, this is the most reliable option.
    ivy{
        url = uri("https://github.com/")
        patternLayout{
            val pattern = when(mindustryVersion){
                "latest" -> "/[organisation]/[module]/releases/[revision]/download/dependencies.jar" //latest stable release
                "be" -> "/[organisation]/[module]/releases/download/master/[revision].jar" //latest commit (BE)
                else -> "/[organisation]/[module]/releases/download/[revision]/dependencies.jar" //specific release
            }
            val path = pattern.substring(0, pattern.lastIndexOf('/'))

            artifact("$path/[classifier].jar")
            artifact(pattern.replaceFirst(Regex("""\.jar$"""), "(-[classifier]).jar"))
        }
        metadataSources{ artifact() }

        content{
            if(mindustryVersion == "be"){
                //BE artifact version is always 'latest'
                includeVersion("Anuken", "MindustryBuilds", "latest")
            }else{
                includeVersion("Anuken", "Mindustry", mindustryVersion)
            }
        }
    }
}

java{
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies{
    compileOnly(if(mindustryVersion == "be") "Anuken:MindustryBuilds:latest" else "Anuken:Mindustry:$mindustryVersion")

    // Harness de test headless : le JAR Mindustry embarque arc + mindustry
    testImplementation(if(mindustryVersion == "be") "Anuken:MindustryBuilds:latest" else "Anuken:Mindustry:$mindustryVersion")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.test{
    useJUnitPlatform()
}

// Build info (git SHA/branch/dirty) embedded in the jar as mti-build.properties,
// so the loaded build can be verified on the mod card (meta.version) in-game.
val buildInfoDir = layout.buildDirectory.dir("buildInfo")
// Compute the git identity at configuration time so it can be declared as a
// task input: without this, Gradle keeps generateBuildInfo UP-TO-DATE across
// commits and the jar keeps a stale SHA baked in.
fun gitInfo(vararg args: String): String = try{
    val p = ProcessBuilder(listOf("git", *args)).directory(projectDir).redirectErrorStream(true).start()
    val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
    p.waitFor()
    out.trim()
}catch(e: Exception){
    ""
}
val buildSha = gitInfo("rev-parse", "--short", "HEAD").ifEmpty{ "unknown" }
val buildBranch = gitInfo("rev-parse", "--abbrev-ref", "HEAD").ifEmpty{ "unknown" }
val buildDirty = if(gitInfo("status", "--porcelain").isNotEmpty()) "-dirty" else ""
val generateBuildInfo = tasks.register("generateBuildInfo"){
    inputs.property("gitSha", "$buildSha$buildDirty")
    inputs.property("gitBranch", buildBranch)
    outputs.dir(buildInfoDir)
    doLast{
        val f = buildInfoDir.get().file("mti-build.properties").asFile
        f.parentFile.mkdirs()
        f.writeText("gitSha=$buildSha$buildDirty\ngitBranch=$buildBranch\nbuildTime=${Instant.now()}\n")
        println("[buildInfo] gitSha=$buildSha$buildDirty branch=$buildBranch")
    }
}
sourceSets{
    main{
        resources.srcDir(buildInfoDir)
    }
}
tasks.named("processResources"){ dependsOn(generateBuildInfo) }
tasks.named("processTestResources"){ dependsOn(generateBuildInfo) }

// mod.hjson with the build identity (branch + SHA + build time) baked into the
// version, so the mod card in the loaded-mods dialog shows exactly which build
// is loaded (e.g. 0.1.0 [reward-pacing-and-boss-window bd3cecb 2026-11-04T08:48Z])
val generateModHjson = tasks.register("generateModHjson"){
    dependsOn(generateBuildInfo)
    val out = layout.buildDirectory.file("modHjson/mod.hjson")
    // re-bake whenever the build info changes (new commit), not just on clean
    inputs.file(buildInfoDir.get().file("mti-build.properties"))
    inputs.property("gitSha", "$buildSha$buildDirty")
    outputs.file(out)
    doLast{
        val props = Properties()
        props.load(buildInfoDir.get().file("mti-build.properties").asFile.inputStream())
        val sha = props.getProperty("gitSha", "unknown")
        val branch = props.getProperty("gitBranch", "?")
        // compact UTC stamp for the mod card (full ISO time stays in mti-build.properties)
        val stamp = try{
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.parse(props.getProperty("buildTime", "")))
        }catch(e: Exception){ "?" }
        val original = File(projectDir, "mod.hjson").readText()
        val patched = Regex("(?m)^version:.*$").replace(original, "version: \"0.1.0 [$branch $sha $stamp]\"")
        val f = out.get().asFile
        f.parentFile.mkdirs()
        f.writeText(patched)
        println("[modHjson] version = 0.1.0 [$branch $sha $stamp]")
    }
}
tasks.named("processTestResources"){ dependsOn(generateModHjson) }

// Default mods dir for deployMod.
// The Steam build of Mindustry runs with its data dir at the RELATIVE "saves"
// folder (DesktopLauncher: Version.isSteam → new Fi("saves")), and Steam
// launches with CWD = install dir — so the Steam game reads mods from
// <steam root>/steamapps/common/Mindustry/saves/mods, NOT ~/.local/share/Mindustry/mods
// (that's the non-Steam/itch build's data dir). Auto-detect the Steam install;
// fall back to the non-Steam location; -PmindustryModsDir=... always wins
// (e.g. a Proton prefix's compatdata/1127400/pfx/drive_c/users/steamuser/AppData/Roaming/Mindustry/mods).
fun detectSteamModsDir(): String?{
    val home = System.getProperty("user.home")
    val roots = mutableListOf(
        "$home/.steam/steam", "$home/.steam/debian-installation", "$home/.steam/root",
        "$home/.local/share/Steam", "$home/.var/app/com.valve.Steam/.local/share/Steam"
    )
    for(root in roots.toList()){
        val vdf = File(root, "steamapps/libraryfolders.vdf")
        if(vdf.isFile){
            vdf.readLines().forEach{ line ->
                Regex("\"path\"\\s+\"([^\"]+)\"").find(line)?.let{ roots.add(it.groupValues[1]) }
            }
        }
    }
    for(root in roots.distinct()){
        val install = File(root, "steamapps/common/Mindustry")
        if(install.isDirectory) return File(install, "saves/mods").absolutePath
    }
    return null
}

// Build + copy the mod into the local game's mods folder, so a restart picks
// it up. Deployed as a fixed-name .jar: the engine scans .jar and .zip
// identically, and a .jar name never collides with the in-game "Import mod"
// flow, which copies imports as .zip (re-importing the same mod then creates
// a duplicate entry instead of overwriting). The mod stays enabled once
// toggled in the in-game Mods menu — no manual re-import needed, same file
// name each time.
tasks.register<Copy>("deployMod"){
    dependsOn(tasks.jar)
    from(layout.buildDirectory.file("libs/${projectName}Desktop.jar"))
    val modsDir = project.findProperty("mindustryModsDir")?.toString()
        ?: detectSteamModsDir()
        ?: "${System.getProperty("user.home")}/.local/share/Mindustry/mods"
    into(modsDir)
    rename{ "mindtheindustry.jar" }
    // remove a legacy .zip deployment: two files with the same internal mod
    // name make the load order-dependent ("already imported" for the loser)
    doLast{
        File(modsDir, "mindtheindustry.zip").delete()
        println("[deployMod] installed mindtheindustry.jar into $modsDir")
    }
}

tasks.jar{
    dependsOn(generateModHjson)
    archiveFileName.set("${projectName}Desktop.jar")

    from(configurations.runtimeClasspath.map{ files ->
        files.map{ if(it.isDirectory) it else zipTree(it) }
    })

    from(projectDir){
        include("icon.png")
        include("bundles/**")
    }

    //generated mod.hjson (build SHA in the version), not the static one
    from(layout.buildDirectory.file("modHjson/mod.hjson"))

    from("assets/"){
        include("**")
    }
}

val jarAndroid = tasks.register("jarAndroid"){
    dependsOn(tasks.jar)

    doLast{
        val sdk = sdkRoot?.let{ File(it) }
        if(sdk == null || !sdk.exists()){
            throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")
        }

        val platformRoot = File(sdk, "platforms").listFiles()
            ?.sortedDescending()
            ?.firstOrNull{ File(it, "android.jar").exists() }
            ?: throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")

        //collect dependencies needed for desugaring
        val classpath = (configurations.compileClasspath.get().files +
                configurations.runtimeClasspath.get().files +
                File(platformRoot, "android.jar"))

        val d8 = if(isWindows) "d8.bat" else "d8"

        val command = buildList{
            add(d8)
            classpath.forEach{ addAll(listOf("--classpath", it.path)) }
            addAll(listOf("--min-api", "21"))
            addAll(listOf("--output", "${projectName}Android.jar"))
            add("${projectName}Desktop.jar")
        }

        //dex and desugar files - this requires d8 in your PATH
        val exitCode = ProcessBuilder(command)
            .directory(layout.buildDirectory.dir("libs").get().asFile)
            .inheritIO()
            .start()
            .waitFor()

        if(exitCode != 0) throw GradleException("d8 failed with exit code $exitCode")
    }
}

tasks.register<Jar>("deploy"){
    dependsOn(jarAndroid, tasks.jar)
    archiveFileName.set("$projectName.jar")

    val libs = layout.buildDirectory.dir("libs")
    from(provider{
        listOf(
            zipTree(libs.get().file("${projectName}Desktop.jar")),
            zipTree(libs.get().file("${projectName}Android.jar"))
        )
    })

    doLast{
        libs.get().file("${projectName}Android.jar").asFile.delete()
    }
}