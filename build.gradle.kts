plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Kotlin 版本下限 2.4.20：低于它（包括 2.4.20 的 Beta / RC）直接拒绝编译。
// Neton 各仓走同一条 Kotlin 线 —— 混用时 K/N klib 与 KSP 产物会以很难看懂的方式失败。
run {
    val required = KotlinVersion(2, 4, 20)
    val actual = org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion(logger)
    val parts = actual.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
    val parsed = KotlinVersion(parts[0], parts[1], parts[2])
    require(parsed > required || (parsed == required && '-' !in actual)) {
        "Kotlin $actual is below the required minimum $required. Upgrade the Kotlin Gradle plugin."
    }
}

group = "com.netonstream.app"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    listOf(macosArm64(), linuxX64(), linuxArm64(), mingwX64()).forEach { target ->
        // 锚定到 Neton canonical 工作区：本模块位于 DeviceAdmin/，故 ../../Neton。复合构建里
        // rootProject 会变成宿主，相对路径会失配，链接期报 `library 'env' not found`。
        val coreInterop = project.file("../../Neton/neton/neton-core/build/nativeInterop/${target.name}").absolutePath
        target.binaries.forEach { binary ->
            binary.linkerOpts.add("-L$coreInterop")
            binary.linkerOpts.add("-lenv")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // 复用的脚手架核心（不重写）。
                implementation("com.netonstream.app:module-system")
                implementation("com.netonstream.app:module-infra")
                // 组织的成员就是账号，读取账号档案（昵称/头像）做成员投影。
                implementation("com.netonstream.app:module-account")

                implementation("com.netonstream:neton-core")
                implementation("com.netonstream:neton-routing")
                implementation("com.netonstream:neton-security")
                implementation("com.netonstream:neton-http")
                implementation("com.netonstream:neton-database")
                implementation("com.netonstream:neton-logging")
                implementation("com.netonstream:neton-validation")
                implementation("com.netonstream:neton-redis")
                implementation("com.netonstream:neton-cache")
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

dependencies {
    add("kspMacosArm64", "com.netonstream:neton-ksp")
    add("kspLinuxX64", "com.netonstream:neton-ksp")
    add("kspLinuxArm64", "com.netonstream:neton-ksp")
    add("kspMingwX64", "com.netonstream:neton-ksp")
}

ksp {
    arg("neton.moduleId", "organization")
}

// 把 sql/postgresql/V*.sql 内嵌进二进制（无运行期文件 IO），生成 init.generated.OrganizationMigrationResources。
// @Module(migrations = true) 的 KSP 校验依赖该类存在。路径相对本模块目录（复合构建里 rootProject 会变）。
extra["neton.migration.moduleId"] = "organization"
extra["neton.migration.dialects"] = listOf("postgresql")
extra["neton.migration.sqlSourceDir"] = file("sql")
apply(from = "../../Neton/neton/scripts/embed-migration-resources.gradle.kts")

// Ensure Kotlin compilation sees KSP-generated commonMain sources.
// Required because facade tables delegate to generated XxxTableImpl —
// K2 metadata compilation needs them at the commonMain level.
afterEvaluate {
    val kspOut = file("build/generated/ksp/macosArm64/macosArm64Main/kotlin")
    kotlin.sourceSets.named("commonMain") {
        kotlin.srcDir(kspOut)
    }
    listOf("macosArm64Main", "linuxX64Main", "linuxArm64Main", "mingwX64Main").forEach { name ->
        kotlin.sourceSets.findByName(name)?.let { ss ->
            val filtered = ss.kotlin.srcDirs.filter { !it.path.contains("generated/ksp") }
            if (filtered.size < ss.kotlin.srcDirs.size) ss.kotlin.setSrcDirs(filtered)
        }
    }
}

tasks.matching { it.name == "compileCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspKotlinMacosArm64")
}
tasks.matching { it.name.matches(Regex("compileKotlin(MacosArm64|LinuxX64|LinuxArm64|MingwX64)")) }.configureEach {
    dependsOn("kspKotlinMacosArm64")
}
tasks.matching { it.name.matches(Regex("kspKotlin(LinuxX64|LinuxArm64|MingwX64)")) }.configureEach {
    dependsOn("kspKotlinMacosArm64")
}
