package init

import logic.OrganizationDirectoryLogic
import logic.OrganizationMemberLogic
import neton.core.component.NetonContext
import port.OrganizationDirectory
import port.OrganizationExtensions

/**
 * organization 模块手写 runtime 装配（KSP manifest 在 logics 之后、routes 之前调用）。
 *
 * 绑定两个对外端口：
 * - [OrganizationExtensions]：生命周期扩展点登记簿。消费方（设备 / privchat）在自己的
 *   bootstrap 里（拓扑序在 organization 之后）取出来 `addCreationHook` / `addMemberChangedHook`
 *   登记，组织本体永不 import 业务。
 * - [OrganizationDirectory]：只读鉴权端口，委托到已自动注册的 [OrganizationMemberLogic]。
 *   任何消费方（含绕过 HTTP 的 msgtrans 设备腿）判归属都走这里。
 *
 * 用 `bindIfAbsent`：允许上层（如某发行版自定义目录实现）先行覆盖。
 */
object OrganizationRuntimeBootstrap {
    fun initialize(ctx: NetonContext) {
        ctx.bindIfAbsent(OrganizationExtensions::class, OrganizationExtensions())
        ctx.bindIfAbsent(
            OrganizationDirectory::class,
            OrganizationDirectoryLogic(ctx.get(OrganizationMemberLogic::class)),
        )
    }
}
