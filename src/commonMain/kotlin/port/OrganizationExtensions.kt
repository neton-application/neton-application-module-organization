package port

import model.Organization

/**
 * 组织模块对外的扩展点（复刻 [port.AccountExtensions] 的地基模块范式）。
 *
 * 组织是"账号之上的通用分组"，它**不认识**任何具体业务（设备、privchat 工作组……）。
 * 业务模块要在组织生命周期里插一脚，只能通过这里登记的接口，并且只能单向依赖组织：
 * 组织永远不会反过来 import 业务。
 *
 * 由 [init.OrganizationRuntimeBootstrap] 创建并 bind 一个实例；依赖组织的模块在自己的
 * bootstrap 里（拓扑序在 organization 之后）取出来登记。读取发生在请求期，登记顺序晚于
 * 组织初始化没有关系。
 */
class OrganizationExtensions {
    private val creationHooks = mutableListOf<OrganizationCreationHook>()
    private val memberChangedHooks = mutableListOf<OrganizationMemberChangedHook>()

    fun addCreationHook(hook: OrganizationCreationHook) {
        creationHooks += hook
    }

    fun addMemberChangedHook(hook: OrganizationMemberChangedHook) {
        memberChangedHooks += hook
    }

    internal fun creationHooks(): List<OrganizationCreationHook> = creationHooks.toList()

    internal fun memberChangedHooks(): List<OrganizationMemberChangedHook> = memberChangedHooks.toList()
}

/**
 * 组织创建后回调。典型用法：
 * - 设备管理：为新组织建一个默认设备池 / 归属根节点。
 * - privchat：为创建人建一个公司工作组。
 *
 * [afterCreated] 在组织与其 OWNER 成员落库之后调用；实现应**自行 catch** 非致命副作用
 * （尽力而为），不要让某个业务的初始化失败拖垮整个建组织流程。
 */
interface OrganizationCreationHook {
    suspend fun afterCreated(organization: Organization, ownerAccountId: Long) {}
}

/**
 * 组织成员变动后回调（新增 / 移除 / 角色变更）。[changedAccountIds] 是本次变动的账号集合。
 * 业务可据此刷新自己的成员投影 / 缓存。同样要求实现自行 catch。
 */
interface OrganizationMemberChangedHook {
    suspend fun afterMembersChanged(orgId: Long, changedAccountIds: List<Long>) {}
}
