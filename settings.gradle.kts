pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = "module-organization"

// 独立构建时把框架与脚手架当 composite build 引入，替换 com.netonstream(:|.app) 坐标。
// 路径锚定到 Neton canonical 工作区：本模块位于 DeviceAdmin/，故 ../../Neton/。
// 主构建（deviceadmin-backend / privchat-application）走 include() + projectDir，不读本文件。
includeBuild("../../Neton/neton")
includeBuild("../../Neton/geolite4k")

// 复用的脚手架核心模块（不重写），坐标 com.netonstream.app:module-system/infra 由源码替换。
includeBuild("../../Neton/neton-application-module-system")
includeBuild("../../Neton/neton-application-module-infra")

// 组织是账号之上的通用分组地基：成员就是 account 模块的账号，故直接依赖 module-account
// （与 module-member 依赖 account 的口径一致，坐标 com.netonstream.app:module-account 源码替换）。
includeBuild("../../Neton/neton-application-module-account")
