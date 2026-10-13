package init

import neton.core.annotations.Module

/**
 * organization 模块声明锚点：账号之上的**通用组织地基**。
 *
 * 提供"一个主账号创建组织、组织里挂成员（成员就是 account 的账号）、成员带组织内角色"
 * 的能力，以及对外只读鉴权端口 [port.OrganizationDirectory] 与生命周期扩展点
 * [port.OrganizationExtensions]。它像 module-account 一样是"很多业务的地基"：
 * 设备管理、privchat 工作组等都只**引用**组织 id 把自己的资源归属到某个组织，
 * 组织本身不认识任何具体业务，也不反向依赖它们。
 *
 * 只依赖 account（成员即账号）及其上游 system / infra。runtime 装配见 [OrganizationRuntimeBootstrap]。
 * 设计规范见本仓 docs/SPEC.md。
 */
@Module(dependsOn = ["system", "infra", "account"], migrations = true)
object OrganizationModule
